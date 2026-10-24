import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FluxIaService } from '../../core/flux-ia.service';
import { LANGAGES } from '../../core/libelles';
import { AssistantReponse, OutilIA } from '../../core/models';
import { TransfertCodeService } from '../../core/transfert-code.service';
import { CodeEditorComponent } from '../../shared/code-editor.component';
import { IconeComponent } from '../../shared/icone.component';
import { ReponseIaComponent } from '../../shared/reponse-ia.component';

interface Outil {
  id: OutilIA;
  icone: string;
  titre: string;
  description: string;
  bouton: string;
  placeholder: string;
}

export const OUTILS: Outil[] = [
  { id: 'tests', icone: '🧪', titre: 'Génération de tests', description: 'Tests unitaires JUnit, pytest ou Jest prêts à copier.',
    bouton: 'Générer les tests', placeholder: 'Collez la fonction ou la classe à tester' },
  { id: 'documentation', icone: '📝', titre: 'Documentation', description: 'Javadoc, docstrings, commentaires et README générés.',
    bouton: 'Documenter le code', placeholder: 'Collez le code à documenter' },
  { id: 'explication', icone: '💡', titre: 'Explication de code', description: 'Le code expliqué pas à pas, ligne par ligne.',
    bouton: 'Expliquer le code', placeholder: 'Collez le code à comprendre' },
  { id: 'commit', icone: '✍️', titre: 'Commit & pull request', description: 'Message de commit et description de PR à partir du diff.',
    bouton: 'Rédiger le message', placeholder: 'Collez le résultat de « git diff »' },
  { id: 'refactoring', icone: '🔁', titre: 'Refactoring', description: 'Un code plus lisible et plus simple, même comportement.',
    bouton: 'Refactorer', placeholder: 'Collez le code à améliorer' },
  { id: 'conversion', icone: '🔀', titre: 'Conversion de langage', description: "Traduire le code dans un autre langage, de façon idiomatique.",
    bouton: 'Convertir', placeholder: 'Collez le code à convertir' },
];

const EXEMPLES: Record<OutilIA, { code: string; langage: string }> = {
  tests: { langage: 'java', code: 'public class Panier {\n    public double total(double prix, int quantite) {\n        if (quantite < 0) {\n            throw new IllegalArgumentException("Quantité négative");\n        }\n        return prix * quantite;\n    }\n}' },
  documentation: { langage: 'python', code: 'def moyenne(notes):\n    if not notes:\n        return 0\n    return sum(notes) / len(notes)\n' },
  explication: { langage: 'javascript', code: 'const actifs = users\n  .filter(u => u.actif)\n  .map(u => u.email.toLowerCase())\n  .sort();\n' },
  commit: { langage: 'auto', code: 'diff --git a/src/Panier.java b/src/Panier.java\n@@ -2,5 +2,8 @@ public class Panier {\n     public double total(double prix, int quantite) {\n+        if (quantite < 0) {\n+            throw new IllegalArgumentException("Quantité négative");\n+        }\n         return prix * quantite;\n     }\n' },
  refactoring: { langage: 'java', code: 'public String statut(int score) {\n    String s = "";\n    if (score >= 80) { s = "bon"; } else { if (score >= 50) { s = "moyen"; } else { s = "faible"; } }\n    return s;\n}' },
  conversion: { langage: 'python', code: 'def compter_mots(texte):\n    compte = {}\n    for mot in texte.lower().split():\n        compte[mot] = compte.get(mot, 0) + 1\n    return compte\n' },
};

/** Boîte à outils IA : tout ce qui accélère le travail du développeur, au-delà de la revue. */
@Component({
  selector: 'app-outils',
  standalone: true,
  imports: [FormsModule, RouterLink, CodeEditorComponent, IconeComponent, ReponseIaComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="barre">
      <div class="fil"><a routerLink="/revues/nouvelle">RevIA</a> / <b>Outils IA</b></div>
      <select class="input petit-select" [ngModel]="langage()" (ngModelChange)="langage.set($event)" aria-label="Langage du code">
        @for (l of langages; track l.valeur) { <option [value]="l.valeur">{{ l.icone }}  {{ l.libelle }}</option> }
      </select>
      @if (outil().id === 'conversion') {
        <select class="input petit-select" [ngModel]="cible()" (ngModelChange)="cible.set($event)" aria-label="Langage cible">
          @for (l of cibles; track l.valeur) { <option [value]="l.valeur">Vers {{ l.libelle }}</option> }
        </select>
      }
      <button class="btn" type="button" (click)="exemple()"><app-icone nom="ampoule" />Exemple</button>
      <button class="btn primaire" type="button" (click)="generer()" [disabled]="attente() || !code().trim()">
        @if (attente()) { <span class="spinner"></span> Génération… } @else { <app-icone nom="etoile" />{{ outil().bouton }} }
      </button>
    </header>

    <div class="page">
      <div class="outils" role="tablist" aria-label="Outils IA">
        @for (o of outils; track o.id) {
          <button type="button" role="tab" class="outil" [class.actif]="o.id === outil().id" [attr.aria-selected]="o.id === outil().id"
                  (click)="choisir(o)">
            <span class="ico">{{ o.icone }}</span>
            <span><b>{{ o.titre }}</b><small>{{ o.description }}</small></span>
          </button>
        }
      </div>

      <div class="grille">
        <section class="editeur-carte" aria-label="Code source">
          <div class="tete"><span>{{ outil().placeholder }}</span><span class="meta">{{ nbLignes() }} / 500 lignes</span></div>
          <div class="zone">
            <app-code-editor [code]="code()" (codeChange)="code.set($event)" [langage]="outil().id === 'commit' ? 'plaintext' : langage()" />
          </div>
        </section>

        <section class="carte resultat" aria-label="Résultat" aria-live="polite">
          @if (attente() && !texte()) {
            <div class="vide"><span class="spinner sombre grand"></span><b>Gemini travaille…</b><span>Masquage des secrets, puis génération.</span></div>
          } @else if (attente()) {
            <div class="entete"><span class="badge langage">{{ outil().icone }} {{ outil().titre }}</span><span class="infos">Gemini écrit…</span></div>
            <app-reponse-ia [contenu]="texte()" [actions]="false" />
          } @else if (erreur()) {
            <div class="erreur" role="alert"><b>{{ erreur() }}</b>
              <button class="btn petit" type="button" (click)="generer()">Réessayer</button></div>
          } @else if (reponse()) { @if (reponse(); as r) {
            <div class="entete">
              <span class="badge langage">{{ outil().icone }} {{ outil().titre }}</span>
              <span class="infos">{{ r.modele }} · {{ duree(r.dureeMs) }} s
                @if (r.nbMasquages) { · 🔒 {{ r.nbMasquages }} secret(s) masqué(s) }</span>
            </div>
            <app-reponse-ia [contenu]="r.contenu" />
          } } @else {
            <div class="vide">
              <span class="grand">{{ outil().icone }}</span>
              <b>{{ outil().titre }}</b>
              <span>{{ outil().description }}</span>
              <span>Collez votre code à gauche (ou cliquez sur « Exemple »), puis « {{ outil().bouton }} ».</span>
            </div>
          }
        </section>
      </div>
    </div>
  `,
  styles: [`
    .petit-select { height: 34px; font-size: 13px; padding: 0 8px; max-width: 200px; }
    .page { max-width: none; }
    .outils { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; margin-bottom: 16px; }
    .outil {
      display: flex; gap: 10px; align-items: flex-start; text-align: left; padding: 12px; border-radius: var(--rayon);
      border: 1px solid var(--bord); background: var(--surface); cursor: pointer; font: 13px var(--police); color: var(--texte);
      box-shadow: var(--ombre); transition: border-color .15s, background .15s;
    }
    .outil:hover { border-color: #c7cbf0; }
    .outil.actif { border-color: var(--primaire); background: var(--primaire-pale); box-shadow: 0 0 0 1px var(--primaire) inset; }
    .ico { font-size: 20px; line-height: 1; }
    .outil b { display: block; font-size: 13px; margin-bottom: 3px; }
    .outil small { display: block; color: var(--texte-2); font-size: 11.5px; line-height: 1.35; }
    .grille { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; align-items: start; }
    .editeur-carte {
      background: #111427; border-radius: var(--rayon); overflow: hidden; border: 1px solid #1f2444;
      display: flex; flex-direction: column; height: calc(100vh - 250px); min-height: 420px;
    }
    .tete { display: flex; justify-content: space-between; padding: 9px 12px; border-bottom: 1px solid #1f2444; color: #a7addb; font-size: 12px; gap: 10px; }
    .meta { color: #7d84ad; white-space: nowrap; }
    .zone { flex: 1; min-height: 0; }
    .resultat { padding: 18px; min-height: 420px; max-height: calc(100vh - 250px); overflow-y: auto; }
    .entete { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
    .infos { font-size: 12px; color: var(--texte-3); }
    .vide { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; text-align: center; color: var(--texte-2); min-height: 380px; }
    .vide b { color: var(--texte); font-size: 15px; }
    .grand { font-size: 34px; }
    .erreur { background: var(--critique-pale); color: var(--critique); border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 10px; align-items: flex-start; }
    @media (max-width: 1200px) { .outils { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
    @media (max-width: 900px) {
      .outils { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .grille { grid-template-columns: 1fr; }
      .editeur-carte { height: 360px; min-height: 0; }
    }
  `],
})
export class OutilsComponent implements OnInit {
  private readonly flux = inject(FluxIaService);
  private readonly transfert = inject(TransfertCodeService);

  readonly outils = OUTILS;
  readonly langages = LANGAGES;
  readonly cibles = LANGAGES.filter((l) => l.valeur !== 'auto');
  readonly outil = signal<Outil>(OUTILS[0]);
  readonly code = signal('');
  readonly langage = signal('auto');
  readonly cible = signal('python');
  readonly attente = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly reponse = signal<AssistantReponse | null>(null);
  readonly texte = signal('');
  readonly nbLignes = computed(() => (this.code() ? this.code().split('\n').length : 0));

  ngOnInit(): void {
    const recu = this.transfert.prendre();
    if (recu) {
      this.code.set(recu.code);
      this.langage.set(recu.langage);
    }
  }

  choisir(o: Outil): void {
    if (o.id !== this.outil().id) {
      this.outil.set(o);
      this.reponse.set(null);
      this.erreur.set(null);
      if (o.id === 'conversion' && this.cible() === this.langage()) {
        this.cible.set(this.langage() === 'python' ? 'java' : 'python');
      }
    }
  }

  exemple(): void {
    const e = EXEMPLES[this.outil().id];
    this.code.set(e.code);
    this.langage.set(e.langage);
    this.reponse.set(null);
    this.erreur.set(null);
  }

  generer(): void {
    if (!this.code().trim() || this.attente()) {
      return;
    }
    this.attente.set(true);
    this.erreur.set(null);
    const id = this.outil().id;
    const cible = id === 'conversion' ? this.cibles.find((l) => l.valeur === this.cible())?.libelle : undefined;
    this.reponse.set(null);
    this.texte.set('');
    this.flux.outil(id, { code: this.code(), langage: this.langage(), cible }).subscribe((ev) => {
      if (ev.type === 'morceau') {
        this.texte.update((t) => t + ev.texte);
      } else if (ev.type === 'fin') {
        this.reponse.set({ ...ev.info, contenu: this.texte().trim() });
        this.attente.set(false);
      } else {
        this.erreur.set(ev.message);
        this.attente.set(false);
      }
    });
  }

  duree(ms: number): string {
    return (ms / 1000).toFixed(1).replace('.', ',');
  }
}
