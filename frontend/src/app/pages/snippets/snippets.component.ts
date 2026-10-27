import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';
import { messageErreur } from '../../core/http';
import { LANGAGES, libelleLangage } from '../../core/libelles';
import { EtatSnippet, RevueDetail, RevueResume, Snippet } from '../../core/models';
import { NotifService } from '../../core/notif.service';
import { TransfertCodeService } from '../../core/transfert-code.service';
import { IconeComponent } from '../../shared/icone.component';

interface FormSnippet { titre: string; langage: string; tags: string; code: string }

/** Une ligne du rapport d'analyse groupée. */
interface LigneLot { snippet: Snippet; etat: 'attente' | 'encours' | 'ok' | 'erreur' | 'deja'; revue?: RevueDetail; erreur?: string }

const ETATS: Record<EtatSnippet, { libelle: string; classe: string }> = {
  non_checke: { libelle: 'Jamais checké', classe: 'neutre' },
  valide: { libelle: '✓ Checké · Validé', classe: 's-corrigee' },
  a_corriger: { libelle: 'Checké · À corriger', classe: 's-a_corriger' },
  modifie: { libelle: 'Modifié depuis le check', classe: 's-a_revoir' },
};

const VIDE: FormSnippet = { titre: '', langage: 'java', tags: '', code: '' };

/** Bibliothèque de snippets : CRUD complet + envoi d'un snippet vers la revue IA. */
@Component({
  selector: 'app-snippets',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, IconeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="barre">
      <div class="fil">Espace de travail / <b>Snippets</b></div>
      <button class="btn primaire" type="button" (click)="nouveau()"><app-icone nom="nouvelle" [taille]="16" />Nouveau snippet</button>
    </div>

    <div class="page">
      <h1 class="titre-page">Bibliothèque de snippets</h1>
      <p class="sous-titre">Sauvegardez vos extraits de code réutilisables et envoyez-les en revue IA en un clic.</p>

      <div class="outils">
        <input class="input recherche" type="search" placeholder="Rechercher par titre ou tag…"
               [ngModel]="recherche()" (ngModelChange)="recherche.set($event)" />
        <select class="input" [ngModel]="filtreLangage()" (ngModelChange)="filtreLangage.set($event)">
          <option value="">Tous les langages</option>
          @for (l of langages; track l.valeur) { <option [value]="l.valeur">{{ l.libelle }}</option> }
        </select>
        <span class="total">{{ filtres().length }} snippet(s)</span>
        <label class="tout"><input type="checkbox" [checked]="toutSelectionne()" (change)="toutSelectionner()" /> Tout sélectionner</label>
        <label class="tout sans-marge" title="Les snippets validés et non modifiés depuis ne consomment pas de quota">
          <input type="checkbox" [checked]="ignorerValides()" (change)="ignorerValides.set(!ignorerValides())" /> Ignorer les déjà validés
        </label>
        <button class="btn primaire" type="button" [disabled]="selection().length === 0 || lotEnCours()" (click)="analyserSelection()">
          @if (lotEnCours()) { <span class="spinner"></span> Analyse {{ lotFaits() }} / {{ lot().length }} } @else { ⚡ Analyser la sélection ({{ selection().length }}) }
        </button>
      </div>

      <!-- Rapport de l'analyse groupée -->
      @if (lot().length) {
        <section class="carte rapport">
          <div class="rapport-tete">
            <h2>Rapport d'analyse groupée</h2>
            <span class="sous">{{ lotFaits() }} / {{ lot().length }} snippet(s) analysé(s)</span>
            @if (!lotEnCours()) { <button class="btn petit" type="button" (click)="lot.set([])">Fermer</button> }
          </div>
          <div class="progression"><span [style.width.%]="lot().length ? (lotFaits() / lot().length) * 100 : 0"></span></div>
          <div class="stats">
            <div class="stat"><div [class]="'v ' + couleur(stats().moyenne)">{{ stats().moyenne }}</div><div class="l">Score moyen /100</div></div>
            <div class="stat"><div class="v">{{ stats().problemes }}</div><div class="l">Problèmes détectés</div></div>
            <div class="stat"><div class="v crit">{{ stats().critiques }}</div><div class="l">Critiques</div></div>
            <div class="stat"><div class="v petit" [title]="stats().pire">{{ stats().pire }}</div><div class="l">Snippet le plus risqué</div></div>
          </div>
          <div class="tableau">
            <table>
              <thead><tr><th>Snippet</th><th>Score</th><th>Critique</th><th>Majeure</th><th>Mineure</th><th>Info</th><th></th></tr></thead>
              <tbody>
                @for (l of lot(); track l.snippet.id) {
                  <tr>
                    <td><b>{{ l.snippet.titre }}</b> <span class="badge langage">{{ libelle(l.snippet.langage) }}</span></td>
                    @switch (l.etat) {
                      @case ('attente') { <td colspan="5" class="gris">En attente…</td> }
                      @case ('encours') { <td colspan="5" class="gris"><span class="spinner sombre"></span> Analyse en cours…</td> }
                      @case ('erreur') { <td colspan="5" class="erreur-champ">{{ l.erreur }}</td> }
                      @case ('deja') {
                        <td><span [class]="'score ' + couleur(l.snippet.dernierScore ?? 0)">{{ l.snippet.dernierScore }}</span></td>
                        <td colspan="4"><span class="badge s-corrigee">✓ Déjà validé</span> <span class="gris">· non ré-analysé (aucun changement depuis le {{ l.snippet.dernierCheck | date: 'dd/MM HH:mm' }})</span></td>
                      }
                      @case ('ok') {
                        <td><span [class]="'score ' + couleur(l.revue!.score)">{{ l.revue!.score }}</span></td>
                        <td>{{ l.revue!.parGravite.critique || 0 }}</td>
                        <td>{{ l.revue!.parGravite.majeure || 0 }}</td>
                        <td>{{ l.revue!.parGravite.mineure || 0 }}</td>
                        <td>{{ l.revue!.parGravite.info || 0 }}</td>
                      }
                    }
                    <td class="droite">
                      @if (l.revue) { <a [routerLink]="['/revues', l.revue.id]">Voir le rapport →</a> }
                      @else if (l.etat === 'deja' && l.snippet.derniereRevueId) { <a [routerLink]="['/revues', l.snippet.derniereRevueId]">Voir le rapport →</a> }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }

      @if (formOuvert()) {
        <form class="carte formulaire" (ngSubmit)="enregistrer()">
          <h2>{{ edition() ? 'Modifier le snippet' : 'Nouveau snippet' }}</h2>
          <div class="grille">
            <div class="champ">
              <label for="s-titre">Titre *</label>
              <input id="s-titre" class="input" name="titre" [(ngModel)]="form.titre" maxlength="120" required />
            </div>
            <div class="champ">
              <label for="s-lang">Langage *</label>
              <select id="s-lang" class="input" name="langage" [(ngModel)]="form.langage">
                @for (l of langages; track l.valeur) { <option [value]="l.valeur">{{ l.libelle }}</option> }
              </select>
            </div>
            <div class="champ">
              <label for="s-tags">Tags (séparés par des virgules)</label>
              <input id="s-tags" class="input" name="tags" [(ngModel)]="form.tags" maxlength="200" placeholder="sql, sécurité, api" />
            </div>
          </div>
          <div class="champ">
            <label for="s-code">Code *</label>
            <textarea id="s-code" class="input" name="code" rows="10" [(ngModel)]="form.code" required></textarea>
          </div>
          @if (erreur()) { <div class="erreur-champ">{{ erreur() }}</div> }
          <div class="actions">
            <button class="btn" type="button" (click)="fermer()">Annuler</button>
            <button class="btn primaire" type="submit" [disabled]="enCours()">{{ edition() ? 'Enregistrer' : 'Créer' }}</button>
          </div>
        </form>
      }

      @if (chargement()) {
        <div class="vide">Chargement…</div>
      } @else if (filtres().length === 0) {
        <div class="carte vide">Aucun snippet pour l'instant. Cliquez sur « Nouveau snippet » pour commencer.</div>
      } @else {
        <div class="liste">
          @for (s of filtres(); track s.id) {
            <article class="carte item">
              <header>
                <input type="checkbox" class="choix" [checked]="selection().includes(s.id)" (change)="basculer(s.id)" aria-label="Sélectionner" />
                <div>
                  <h3>{{ s.titre }}</h3>
                  <div class="meta">
                    <span class="badge langage">{{ libelle(s.langage) }}</span>
                    @for (t of tagsDe(s); track t) { <span class="badge neutre">#{{ t }}</span> }
                    <span class="date">modifié le {{ s.modifieLe | date: 'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                </div>
                <span class="badge etat" [class]="'badge etat ' + etats[s.etat].classe">{{ etats[s.etat].libelle }}</span>
              </header>
              @if (s.nbChecks) {
                <div class="dernier-check">
                  Dernier check le {{ s.dernierCheck | date: 'dd/MM/yyyy HH:mm' }} ·
                  score <b [class]="couleur(s.dernierScore ?? 0)">{{ s.dernierScore }}/100</b>
                  @if (s.dernierCritiques) { · <span class="rouge">{{ s.dernierCritiques }} critique(s)</span> }
                  · <button type="button" class="btn lien" (click)="basculerHistorique(s)">
                    {{ historiqueOuvert() === s.id ? 'Masquer' : 'Historique' }} ({{ s.nbChecks }})
                  </button>
                </div>
                @if (historiqueOuvert() === s.id) {
                  <ul class="historique">
                    @for (c of historique(); track c.id) {
                      <li>
                        <span class="date">{{ c.creeLe | date: 'dd/MM/yyyy HH:mm' }}</span>
                        <b [class]="couleur(c.score)">{{ c.score }}/100</b>
                        <span class="gris">{{ c.nbProblemes }} problème(s) · {{ c.nbCritiques }} critique(s)</span>
                        <a [routerLink]="['/revues', c.id]">Rapport →</a>
                      </li>
                    } @empty { <li class="gris">Chargement…</li> }
                  </ul>
                }
              }
              <pre class="code">{{ apercu(s.code) }}</pre>
              <footer>
                <button class="btn petit primaire" type="button" (click)="envoyerEnRevue(s)"><app-icone nom="envoyer" [taille]="14" />Analyser avec l'IA</button>
                <button class="btn petit" type="button" (click)="copier(s)"><app-icone nom="copier" [taille]="14" />Copier</button>
                <button class="btn petit" type="button" (click)="modifier(s)">Modifier</button>
                <button class="btn petit danger" type="button" (click)="supprimer(s)"><app-icone nom="poubelle" [taille]="14" />Supprimer</button>
              </footer>
            </article>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .etat { margin-left: auto; flex-shrink: 0; }
    .sans-marge { margin-left: 0 !important; }
    .dernier-check { font-size: 12.5px; color: var(--texte-2); }
    .historique { list-style: none; margin: 0; padding: 8px 10px; background: var(--surface-2); border-radius: 8px; display: flex; flex-direction: column; gap: 6px; font-size: 12.5px; }
    .historique li { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .historique a { margin-left: auto; }
    .tout { display: flex; align-items: center; gap: 6px; font-size: 13px; margin-left: auto; }
    .item header { display: flex; gap: 10px; align-items: flex-start; }
    .choix { margin-top: 3px; width: 16px; height: 16px; }
    .rapport { padding: 18px; margin-bottom: 18px; }
    .rapport-tete { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
    .rapport-tete h2 { font-size: 16px; }
    .rapport-tete .sous { color: var(--texte-2); font-size: 13px; flex: 1; }
    .progression { height: 6px; background: var(--surface-2); border-radius: 4px; overflow: hidden; margin-bottom: 14px; }
    .progression span { display: block; height: 100%; background: var(--primaire); transition: width .3s; }
    .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
    .stat { border: 1px solid var(--bord); border-radius: 10px; padding: 12px; min-width: 0; }
    .stat .v { font-size: 22px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .stat .v.petit { font-size: 14px; padding-top: 6px; }
    .stat .l { color: var(--texte-2); font-size: 12px; }
    .crit, .rouge { color: var(--critique); }
    .orange { color: var(--majeure); }
    .vert { color: var(--ok); }
    .tableau { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th { text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .06em; color: var(--texte-3); padding: 8px 10px; border-bottom: 1px solid var(--bord); }
    td { padding: 9px 10px; border-bottom: 1px solid var(--bord); }
    .gris { color: var(--texte-2); }
    .score { font-weight: 800; font-size: 15px; }
    .droite { text-align: right; white-space: nowrap; }
    @media (max-width: 900px) { .stats { grid-template-columns: repeat(2, 1fr); } }
    .outils { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
    .recherche { flex: 1; min-width: 220px; }
    .total { color: var(--texte-2); font-size: 13px; }
    .formulaire { padding: 18px; margin-bottom: 18px; display: flex; flex-direction: column; gap: 12px; }
    .formulaire h2 { font-size: 16px; }
    .grille { display: grid; grid-template-columns: 2fr 1fr 2fr; gap: 12px; }
    .actions { display: flex; justify-content: flex-end; gap: 8px; }
    .liste { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 14px; }
    .item { padding: 14px; display: flex; flex-direction: column; gap: 10px; }
    .item h3 { font-size: 15px; margin-bottom: 6px; }
    .meta { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
    .date { color: var(--texte-3); font-size: 11.5px; }
    .code {
      margin: 0; background: #0f1330; color: #d6dbff; border-radius: 8px; padding: 10px 12px; font-size: 12px;
      max-height: 160px; overflow: auto; white-space: pre;
    }
    footer { display: flex; gap: 6px; flex-wrap: wrap; }
    @media (max-width: 900px) { .grille { grid-template-columns: 1fr; } .liste { grid-template-columns: 1fr; } }
  `],
})
export class SnippetsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notif = inject(NotifService);
  private readonly router = inject(Router);
  private readonly transfert = inject(TransfertCodeService);
  private readonly auth = inject(AuthService);

  // ---------- Analyse groupée ----------
  readonly etats = ETATS;
  readonly ignorerValides = signal(true);
  readonly historiqueOuvert = signal<number | null>(null);
  readonly historique = signal<RevueResume[]>([]);
  readonly selection = signal<number[]>([]);
  readonly lot = signal<LigneLot[]>([]);
  readonly lotEnCours = signal(false);
  readonly lotFaits = computed(() => this.lot().filter((l) => l.etat === 'ok' || l.etat === 'erreur' || l.etat === 'deja').length);
  readonly toutSelectionne = computed(() => this.filtres().length > 0 && this.filtres().every((s) => this.selection().includes(s.id)));
  readonly stats = computed(() => {
    const ok = this.lot().filter((l) => l.revue).map((l) => l.revue!);
    const moyenne = ok.length ? Math.round(ok.reduce((t, r) => t + r.score, 0) / ok.length) : 0;
    const problemes = ok.reduce((t, r) => t + Object.values(r.parGravite).reduce((a, b) => a + (b || 0), 0), 0);
    const critiques = ok.reduce((t, r) => t + (r.parGravite.critique || 0), 0);
    const pire = ok.length ? ok.reduce((a, b) => (b.score < a.score ? b : a)).titre : '—';
    return { moyenne, problemes, critiques, pire };
  });

  readonly langages = LANGAGES.filter((l) => l.valeur !== 'auto');
  readonly snippets = signal<Snippet[]>([]);
  readonly chargement = signal(true);
  readonly recherche = signal('');
  readonly filtreLangage = signal('');
  readonly formOuvert = signal(false);
  readonly edition = signal<Snippet | null>(null);
  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  form: FormSnippet = { ...VIDE };

  /** Filtrage côté client par texte (titre / tags) et par langage. */
  readonly filtres = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const lang = this.filtreLangage();
    return this.snippets().filter((s) =>
      (!lang || s.langage === lang) &&
      (!q || s.titre.toLowerCase().includes(q) || (s.tags ?? '').toLowerCase().includes(q)));
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.api.snippets().subscribe({
      next: (l) => { this.snippets.set(l); this.chargement.set(false); },
      error: (e) => { this.chargement.set(false); this.notif.erreur(messageErreur(e)); },
    });
  }

  nouveau(): void {
    this.form = { ...VIDE };
    this.edition.set(null);
    this.erreur.set(null);
    this.formOuvert.set(true);
  }

  modifier(s: Snippet): void {
    this.form = { titre: s.titre, langage: s.langage, tags: s.tags ?? '', code: s.code };
    this.edition.set(s);
    this.erreur.set(null);
    this.formOuvert.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  fermer(): void {
    this.formOuvert.set(false);
    this.edition.set(null);
  }

  enregistrer(): void {
    if (!this.form.titre.trim() || !this.form.code.trim()) {
      this.erreur.set('Le titre et le code sont obligatoires.');
      return;
    }
    this.enCours.set(true);
    const e = this.edition();
    const req = e ? this.api.modifierSnippet(e.id, this.form) : this.api.creerSnippet(this.form);
    req.subscribe({
      next: () => {
        this.enCours.set(false);
        this.notif.succes(e ? 'Snippet modifié.' : 'Snippet créé.');
        this.fermer();
        this.charger();
      },
      error: (err) => { this.enCours.set(false); this.erreur.set(messageErreur(err)); },
    });
  }

  supprimer(s: Snippet): void {
    if (!confirm(`Supprimer le snippet « ${s.titre} » ?`)) return;
    this.api.supprimerSnippet(s.id).subscribe({
      next: () => {
        this.snippets.update((l) => l.filter((x) => x.id !== s.id));
        this.notif.succes('Snippet supprimé.');
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  /** Dépose le code dans la page « Nouvelle revue », qui le charge automatiquement. */
  envoyerEnRevue(s: Snippet): void {
    this.transfert.deposer(s.code, s.langage, s.id);
    this.router.navigate(['/revues/nouvelle']);
  }

  copier(s: Snippet): void {
    navigator.clipboard?.writeText(s.code).then(() => this.notif.succes('Code copié.'));
  }

  /** Affiche / masque l'historique des checks d'un snippet. */
  basculerHistorique(s: Snippet): void {
    if (this.historiqueOuvert() === s.id) {
      this.historiqueOuvert.set(null);
      return;
    }
    this.historique.set([]);
    this.historiqueOuvert.set(s.id);
    this.api.checksSnippet(s.id).subscribe({
      next: (l) => this.historique.set(l),
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  /** Met à jour les états des snippets sans afficher « Chargement… ». */
  private rechargerSilencieux(): void {
    this.api.snippets().subscribe({ next: (l) => this.snippets.set(l) });
  }

  basculer(id: number): void {
    this.selection.update((l) => (l.includes(id) ? l.filter((x) => x !== id) : [...l, id]));
  }

  toutSelectionner(): void {
    this.selection.set(this.toutSelectionne() ? [] : this.filtres().map((s) => s.id));
  }

  /** Lance une revue IA par snippet sélectionné, l'une après l'autre, et remplit le rapport au fil de l'eau. */
  analyserSelection(): void {
    const choisis = this.snippets().filter((s) => this.selection().includes(s.id));
    if (!choisis.length) return;
    // Un snippet déjà validé et non modifié depuis n'est pas ré-analysé (économie de quota)
    this.lot.set(choisis.map((snippet) => ({
      snippet, etat: this.ignorerValides() && snippet.etat === 'valide' ? 'deja' as const : 'attente' as const,
    })));
    this.lotEnCours.set(true);
    this.analyserSuivant(0);
  }

  private analyserSuivant(i: number): void {
    const lignes = this.lot();
    if (i >= lignes.length) {
      this.lotEnCours.set(false);
      this.auth.rafraichir().subscribe();
      this.rechargerSilencieux();
      const ok = lignes.filter((l) => l.etat === 'ok').length;
      this.notif.succes('Analyse groupée terminée : ' + ok + ' / ' + lignes.length + ' snippet(s).');
      return;
    }
    if (lignes[i].etat === 'deja') {
      this.analyserSuivant(i + 1);
      return;
    }
    const s = lignes[i].snippet;
    this.majLigne(i, { etat: 'encours' });
    this.api.soumettre({ titre: s.titre, langage: s.langage, code: s.code, mode: 'complet', snippetId: s.id }).subscribe({
      next: (revue) => { this.majLigne(i, { etat: 'ok', revue }); this.analyserSuivant(i + 1); },
      error: (e) => { this.majLigne(i, { etat: 'erreur', erreur: messageErreur(e) }); this.analyserSuivant(i + 1); },
    });
  }

  private majLigne(i: number, maj: Partial<LigneLot>): void {
    this.lot.update((l) => l.map((x, j) => (j === i ? { ...x, ...maj } : x)));
  }

  couleur(score: number): string {
    return score >= 80 ? 'vert' : score >= 50 ? 'orange' : 'rouge';
  }

  tagsDe(s: Snippet): string[] {
    return (s.tags ?? '').split(',').map((t) => t.trim()).filter(Boolean);
  }

  apercu(code: string): string {
    const lignes = code.split('\n');
    return lignes.length > 12 ? lignes.slice(0, 12).join('\n') + '\n…' : code;
  }

  libelle(v: string): string {
    return libelleLangage(v);
  }
}
