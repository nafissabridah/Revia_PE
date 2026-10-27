import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { messageErreur } from '../../core/http';
import { PromptUtilisateur } from '../../core/models';
import { NotifService } from '../../core/notif.service';
import { IconeComponent } from '../../shared/icone.component';

const MAX_CONTENU = 4000;

/** Bibliothèque de prompts personnels, réutilisables dans l'Assistant IA. */
@Component({
  selector: 'app-prompts',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, IconeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="barre">
      <div class="fil"><a routerLink="/revues/nouvelle">RevIA</a> / <b>Mes prompts</b></div>
      <button class="btn primaire" type="button" (click)="nouveau()"><app-icone nom="nouvelle" />Nouveau prompt</button>
    </header>

    <div class="page">
      <h1 class="titre-page">Mes prompts</h1>
      <p class="sous-titre">Enregistrez les consignes que vous utilisez souvent, puis insérez-les en un clic depuis l'écran Assistant IA.</p>

      <div class="grille">
        <section class="carte liste" aria-label="Mes prompts">
          @if (!prompts().length) {
            <p class="vide">Aucun prompt pour l'instant. Cliquez sur « Nouveau prompt » pour créer le premier.</p>
          }
          @for (p of prompts(); track p.id) {
            <article class="prompt" [class.selection]="p.id === courant()?.id">
              <button type="button" class="corps" (click)="p.modifiable && modifier(p)" [disabled]="!p.modifiable"
                      [attr.aria-label]="(p.modifiable ? 'Modifier ' : 'Prompt officiel : ') + p.titre">
                <b>{{ p.titre }} @if (p.partage) { <span class="badge neutre">Officiel</span> }</b>
                <span class="apercu">{{ p.contenu }}</span>
                <span class="date">Modifié le {{ p.modifieLe | date: 'dd/MM/yyyy HH:mm' }}</span>
              </button>
              <div class="actions-ligne">
                <button class="btn petit primaire" type="button" (click)="utiliser(p)">Utiliser dans l'assistant</button>
                @if (p.modifiable) {
                  <button class="btn petit danger" type="button" (click)="supprimer(p)" [attr.aria-label]="'Supprimer ' + p.titre">
                    <app-icone nom="poubelle" [taille]="14" />
                  </button>
                }
              </div>
            </article>
          }
        </section>

        @if (edition()) {
          <form class="carte formulaire" (ngSubmit)="enregistrer()" #form="ngForm">
            <h2 class="sous-section">{{ courant() ? 'Modifier le prompt' : 'Nouveau prompt' }}</h2>
            <div class="champ">
              <label for="p-titre">Titre</label>
              <input id="p-titre" name="titre" class="input" [(ngModel)]="titre" required minlength="2" maxlength="80"
                     placeholder="Ex. : Explique en simple" />
            </div>
            <div class="champ">
              <label for="p-contenu">Prompt</label>
              <textarea id="p-contenu" name="contenu" class="input" rows="10" [(ngModel)]="contenu" required [maxlength]="max"
                        placeholder="Ex. : Explique ce code à un débutant, ligne par ligne, avec un exemple."></textarea>
              <span class="aide">{{ contenu.length }} / {{ max }} caractères. Le code joint dans l'assistant est envoyé avec votre prompt.</span>
            </div>
            <div class="actions">
              <button class="btn" type="button" (click)="annuler()">Annuler</button>
              <button class="btn primaire" type="submit" [disabled]="form.invalid || envoi()">{{ courant() ? 'Enregistrer' : 'Créer le prompt' }}</button>
            </div>
          </form>
        }
      </div>
    </div>
  `,
  styles: [`
    .grille { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; align-items: start; max-width: 1200px; }
    .liste { padding: 10px; display: flex; flex-direction: column; gap: 8px; }
    .vide { color: var(--texte-2); padding: 12px; margin: 0; }
    .prompt { border: 1px solid var(--bord); border-radius: 10px; padding: 10px 12px; display: flex; flex-direction: column; gap: 8px; }
    .prompt.selection { border-color: var(--primaire); background: var(--primaire-pale); }
    .corps:disabled { cursor: default; }
    .corps { all: unset; cursor: pointer; display: flex; flex-direction: column; gap: 4px; font: 13px var(--police); min-width: 0; }
    .apercu { color: var(--texte-2); overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow-wrap: anywhere; }
    .date { color: var(--texte-3); font-size: 11.5px; }
    .actions-ligne { display: flex; gap: 8px; justify-content: flex-end; }
    .formulaire { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
    .formulaire .sous-section { margin: 0; font-size: 16px; }
    .formulaire textarea { resize: vertical; font-family: var(--police); }
    .aide { font-size: 12px; color: var(--texte-3); }
    .actions { display: flex; gap: 8px; justify-content: flex-end; }
    @media (max-width: 900px) { .grille { grid-template-columns: 1fr; } }
  `],
})
export class PromptsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notif = inject(NotifService);
  private readonly router = inject(Router);

  readonly max = MAX_CONTENU;
  readonly prompts = signal<PromptUtilisateur[]>([]);
  readonly courant = signal<PromptUtilisateur | null>(null);
  readonly edition = signal(false);
  readonly envoi = signal(false);
  titre = '';
  contenu = '';

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.api.mesPrompts().subscribe({
      next: (ps) => this.prompts.set(ps),
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  nouveau(): void {
    this.courant.set(null);
    this.titre = '';
    this.contenu = '';
    this.edition.set(true);
  }

  modifier(p: PromptUtilisateur): void {
    this.courant.set(p);
    this.titre = p.titre;
    this.contenu = p.contenu;
    this.edition.set(true);
  }

  annuler(): void {
    this.edition.set(false);
    this.courant.set(null);
  }

  enregistrer(): void {
    const existant = this.courant();
    const corps = { titre: this.titre.trim(), contenu: this.contenu.trim() };
    this.envoi.set(true);
    const appel = existant ? this.api.modifierMonPrompt(existant.id, corps) : this.api.creerMonPrompt(corps);
    appel.subscribe({
      next: (p) => {
        this.envoi.set(false);
        this.notif.succes(existant ? `Prompt « ${p.titre} » modifié.` : `Prompt « ${p.titre} » créé.`);
        this.annuler();
        this.charger();
      },
      error: (e) => {
        this.envoi.set(false);
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  supprimer(p: PromptUtilisateur): void {
    if (!confirm(`Supprimer le prompt « ${p.titre} » ?`)) {
      return;
    }
    this.api.supprimerMonPrompt(p.id).subscribe({
      next: () => {
        this.notif.succes('Prompt supprimé.');
        if (this.courant()?.id === p.id) {
          this.annuler();
        }
        this.charger();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  utiliser(p: PromptUtilisateur): void {
    this.router.navigate(['/assistant'], { queryParams: { prompt: p.id } });
  }
}
