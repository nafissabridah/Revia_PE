import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { messageErreur } from '../../core/http';
import { GraviteRegle, RegleRevue } from '../../core/models';
import { NotifService } from '../../core/notif.service';
import { IconeComponent } from '../../shared/icone.component';

interface FormRegle { nom: string; description: string; gravite: GraviteRegle; active: boolean }

const VIDE: FormRegle = { nom: '', description: '', gravite: 'MAJEURE', active: true };

/** Règles de revue personnalisées : CRUD + activation / désactivation en un clic. */
@Component({
  selector: 'app-regles',
  standalone: true,
  imports: [FormsModule, IconeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="barre">
      <div class="fil">Espace de travail / <b>Règles de revue</b></div>
      <button class="btn primaire" type="button" (click)="nouveau()"><app-icone nom="nouvelle" [taille]="16" />Nouvelle règle</button>
    </div>

    <div class="page">
      <h1 class="titre-page">Règles de revue personnalisées</h1>
      <p class="sous-titre">Définissez votre propre checklist qualité : conventions d'équipe, bonnes pratiques, interdits.</p>

      <div class="stats">
        <div class="carte stat"><div class="v">{{ regles().length }}</div><div class="l">Règles au total</div></div>
        <div class="carte stat"><div class="v ok">{{ nbActives() }}</div><div class="l">Actives</div></div>
        <div class="carte stat"><div class="v">{{ regles().length - nbActives() }}</div><div class="l">Désactivées</div></div>
        <div class="carte stat"><div class="v crit">{{ nbCritiques() }}</div><div class="l">Critiques</div></div>
      </div>

      @if (formOuvert()) {
        <form class="carte formulaire" (ngSubmit)="enregistrer()">
          <h2>{{ edition() ? 'Modifier la règle' : 'Nouvelle règle' }}</h2>
          <div class="grille">
            <div class="champ">
              <label for="r-nom">Nom de la règle *</label>
              <input id="r-nom" class="input" name="nom" [(ngModel)]="form.nom" maxlength="120" required
                     placeholder="Ex. : Pas de System.out.println en production" />
            </div>
            <div class="champ">
              <label for="r-grav">Gravité *</label>
              <select id="r-grav" class="input" name="gravite" [(ngModel)]="form.gravite">
                @for (g of gravites; track g.valeur) { <option [value]="g.valeur">{{ g.libelle }}</option> }
              </select>
            </div>
          </div>
          <div class="champ">
            <label for="r-desc">Description</label>
            <textarea id="r-desc" class="input texte" name="description" rows="3" [(ngModel)]="form.description" maxlength="500"
                      placeholder="Pourquoi cette règle ? Comment la respecter ?"></textarea>
          </div>
          <label class="case"><input type="checkbox" name="active" [(ngModel)]="form.active" /> Règle active</label>
          @if (erreur()) { <div class="erreur-champ">{{ erreur() }}</div> }
          <div class="actions">
            <button class="btn" type="button" (click)="fermer()">Annuler</button>
            <button class="btn primaire" type="submit" [disabled]="enCours()">{{ edition() ? 'Enregistrer' : 'Créer' }}</button>
          </div>
        </form>
      }

      @if (chargement()) {
        <div class="vide">Chargement…</div>
      } @else if (regles().length === 0) {
        <div class="carte vide">Aucune règle définie. Cliquez sur « Nouvelle règle » pour créer votre checklist.</div>
      } @else {
        <div class="carte tableau">
          <table>
            <thead>
              <tr><th>Active</th><th>Règle</th><th>Gravité</th><th class="droite">Actions</th></tr>
            </thead>
            <tbody>
              @for (r of regles(); track r.id) {
                <tr [class.inactive]="!r.active">
                  <td>
                    <button type="button" class="interrupteur" [class.on]="r.active" (click)="basculer(r)"
                            [attr.aria-pressed]="r.active" [title]="r.active ? 'Désactiver' : 'Activer'"><span></span></button>
                  </td>
                  <td>
                    <div class="nom">{{ r.nom }}</div>
                    @if (r.description) { <div class="desc">{{ r.description }}</div> }
                  </td>
                  <td><span class="badge" [class]="'badge g-' + r.gravite.toLowerCase()"><span class="point"></span>{{ libelleGravite(r.gravite) }}</span></td>
                  <td class="droite">
                    <button class="btn petit" type="button" (click)="modifier(r)">Modifier</button>
                    <button class="btn petit danger" type="button" (click)="supprimer(r)"><app-icone nom="poubelle" [taille]="14" /></button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [`
    .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 18px; }
    .stat { padding: 14px; }
    .stat .v { font-size: 24px; font-weight: 800; }
    .stat .v.ok { color: var(--ok); }
    .stat .v.crit { color: var(--critique); }
    .stat .l { color: var(--texte-2); font-size: 12.5px; }
    .formulaire { padding: 18px; margin-bottom: 18px; display: flex; flex-direction: column; gap: 12px; }
    .formulaire h2 { font-size: 16px; }
    .grille { display: grid; grid-template-columns: 3fr 1fr; gap: 12px; }
    textarea.texte { font-family: var(--police); font-size: 13.5px; }
    .case { display: flex; align-items: center; gap: 8px; font-size: 13.5px; }
    .actions { display: flex; justify-content: flex-end; gap: 8px; }
    .tableau { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; font-size: 11.5px; text-transform: uppercase; letter-spacing: .06em; color: var(--texte-3); padding: 12px 14px; border-bottom: 1px solid var(--bord); }
    td { padding: 12px 14px; border-bottom: 1px solid var(--bord); vertical-align: middle; }
    tr:last-child td { border-bottom: none; }
    tr.inactive .nom, tr.inactive .desc { opacity: .5; }
    .nom { font-weight: 600; font-size: 14px; }
    .desc { color: var(--texte-2); font-size: 12.5px; margin-top: 2px; }
    .droite { text-align: right; white-space: nowrap; }
    .droite .btn { margin-left: 6px; }
    .interrupteur { width: 38px; height: 22px; border-radius: 11px; border: none; background: #cfd3e6; position: relative; cursor: pointer; transition: background .2s; }
    .interrupteur span { position: absolute; top: 3px; left: 3px; width: 16px; height: 16px; border-radius: 50%; background: #fff; transition: transform .2s; }
    .interrupteur.on { background: var(--ok); }
    .interrupteur.on span { transform: translateX(16px); }
    @media (max-width: 900px) { .stats { grid-template-columns: repeat(2, 1fr); } .grille { grid-template-columns: 1fr; } }
  `],
})
export class ReglesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notif = inject(NotifService);

  readonly gravites: { valeur: GraviteRegle; libelle: string }[] = [
    { valeur: 'CRITIQUE', libelle: 'Critique' },
    { valeur: 'MAJEURE', libelle: 'Majeure' },
    { valeur: 'MINEURE', libelle: 'Mineure' },
    { valeur: 'INFO', libelle: 'Info' },
  ];
  readonly regles = signal<RegleRevue[]>([]);
  readonly chargement = signal(true);
  readonly formOuvert = signal(false);
  readonly edition = signal<RegleRevue | null>(null);
  readonly enCours = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly nbActives = computed(() => this.regles().filter((r) => r.active).length);
  readonly nbCritiques = computed(() => this.regles().filter((r) => r.gravite === 'CRITIQUE').length);
  form: FormRegle = { ...VIDE };

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.chargement.set(true);
    this.api.regles().subscribe({
      next: (l) => { this.regles.set(l); this.chargement.set(false); },
      error: (e) => { this.chargement.set(false); this.notif.erreur(messageErreur(e)); },
    });
  }

  nouveau(): void {
    this.form = { ...VIDE };
    this.edition.set(null);
    this.erreur.set(null);
    this.formOuvert.set(true);
  }

  modifier(r: RegleRevue): void {
    this.form = { nom: r.nom, description: r.description ?? '', gravite: r.gravite, active: r.active };
    this.edition.set(r);
    this.erreur.set(null);
    this.formOuvert.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  fermer(): void {
    this.formOuvert.set(false);
    this.edition.set(null);
  }

  enregistrer(): void {
    if (!this.form.nom.trim()) {
      this.erreur.set('Le nom de la règle est obligatoire.');
      return;
    }
    this.enCours.set(true);
    const e = this.edition();
    const req = e ? this.api.modifierRegle(e.id, this.form) : this.api.creerRegle(this.form);
    req.subscribe({
      next: () => {
        this.enCours.set(false);
        this.notif.succes(e ? 'Règle modifiée.' : 'Règle créée.');
        this.fermer();
        this.charger();
      },
      error: (err) => { this.enCours.set(false); this.erreur.set(messageErreur(err)); },
    });
  }

  basculer(r: RegleRevue): void {
    this.api.basculerRegle(r.id).subscribe({
      next: (maj) => this.regles.update((l) => l.map((x) => (x.id === maj.id ? maj : x))),
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  supprimer(r: RegleRevue): void {
    if (!confirm(`Supprimer la règle « ${r.nom} » ?`)) return;
    this.api.supprimerRegle(r.id).subscribe({
      next: () => {
        this.regles.update((l) => l.filter((x) => x.id !== r.id));
        this.notif.succes('Règle supprimée.');
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  libelleGravite(g: GraviteRegle): string {
    return this.gravites.find((x) => x.valeur === g)?.libelle ?? g;
  }
}
