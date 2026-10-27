import { DecimalPipe } from '@angular/common';
import { AfterViewChecked, ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { FluxIaService } from '../../core/flux-ia.service';
import { messageErreur } from '../../core/http';
import { NotifService } from '../../core/notif.service';
import { LANGAGES } from '../../core/libelles';
import { MessageChat, PromptUtilisateur } from '../../core/models';
import { TransfertCodeService } from '../../core/transfert-code.service';
import { IconeComponent } from '../../shared/icone.component';
import { ReponseIaComponent } from '../../shared/reponse-ia.component';

const CLE = 'revia.chat';
const SUGGESTIONS = [
  "Explique-moi l'erreur NullPointerException et comment l'éviter",
  'Comment se protéger des injections SQL en Java ?',
  "Quelle est la différence entre == et equals() ?",
  'Écris une expression régulière qui valide une adresse e-mail',
];

interface Bulle extends MessageChat {
  meta?: string;
  enCours?: boolean;
  compacte?: boolean;
}

/** Estimation grossière : environ 4 caractères par token. */
const estimerTokens = (texte: string): number => Math.ceil(texte.length / 4);

/** Assistant IA (chatbot) : questions libres sur le code, avec un extrait joint en option. */
@Component({
  selector: 'app-assistant',
  standalone: true,
  imports: [FormsModule, RouterLink, DecimalPipe, IconeComponent, ReponseIaComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="barre">
      <div class="fil"><a routerLink="/revues/nouvelle">RevIA</a> / <b>Assistant IA</b></div>
      <div class="menu-prompts">
        <button class="btn" type="button" (click)="menuPrompts.set(!menuPrompts())" [attr.aria-expanded]="menuPrompts()" aria-haspopup="true">
          <app-icone nom="prompt" />Mes prompts
        </button>
        @if (menuPrompts()) {
          <div class="carte liste-prompts" role="menu">
            @for (p of prompts(); track p.id) {
              <button type="button" role="menuitem" class="entree" (click)="inserer(p)">
                <b>{{ p.titre }} @if (p.partage) { <i class="officiel">officiel</i> }</b><span>{{ p.contenu }}</span>
              </button>
            } @empty {
              <p class="vide">Aucun prompt enregistré.</p>
            }
            <a routerLink="/prompts" class="gerer">Gérer mes prompts</a>
          </div>
        }
      </div>
      <button class="btn" type="button" (click)="codeOuvert.set(!codeOuvert())">
        <app-icone nom="import" />{{ codeOuvert() ? 'Masquer le code joint' : 'Joindre du code' }}
      </button>
      <button class="btn" type="button" (click)="nouvelle()" [disabled]="!messages().length || attente()">
        <app-icone nom="nouvelle" />Nouvelle conversation
      </button>
    </header>

    <div class="page grille" [class.avec-code]="codeOuvert()">
      <section class="carte chat" aria-label="Conversation">
        <div class="fil-messages" #fil>
          @if (!messages().length) {
            <div class="accueil">
              <div class="logo-ia">✦</div>
              <h2>Bonjour, je suis l'assistant RevIA</h2>
              <p>Posez une question sur votre code, une erreur ou une bonne pratique. Je réponds en français, avec des exemples.</p>
              <div class="suggestions">
                @for (s of suggestions; track s) {
                  <button type="button" class="suggestion" (click)="envoyer(s)">{{ s }}</button>
                }
              </div>
              @if (prompts().length) {
                <p class="titre-prompts">Mes prompts</p>
                <div class="suggestions">
                  @for (p of prompts(); track p.id) {
                    <button type="button" class="suggestion perso" (click)="inserer(p)" [title]="p.contenu">{{ p.titre }}</button>
                  }
                </div>
              }
            </div>
          }
          @for (m of messages(); track $index) {
            <div class="message" [class.moi]="m.role === 'utilisateur'">
              @if (m.role === 'assistant') { <span class="avatar-ia">✦</span> }
              <div class="bulle" [class.resume]="m.compacte">
                @if (m.compacte) { <div class="etiquette-resume">📌 Résumé de la conversation précédente</div> }
                @if (m.role === 'assistant' && m.enCours && !m.contenu) {
                  <div class="ecrit"><i></i><i></i><i></i></div>
                } @else if (m.role === 'assistant') {
                  <app-reponse-ia [contenu]="m.contenu" [actions]="!m.enCours" />
                  @if (m.meta) { <div class="meta">{{ m.meta }}</div> }
                } @else {
                  <div class="texte">{{ m.contenu }}</div>
                }
              </div>
            </div>
          }
          @if (erreur()) {
            <div class="erreur" role="alert">{{ erreur() }} <button class="btn petit" type="button" (click)="reessayer()">Réessayer</button></div>
          }
        </div>

        <div class="contexte" [class.alerte]="pourcentageContexte() >= 70" [class.critique]="pourcentageContexte() >= 90"
             role="group" aria-label="Contexte de la conversation">
          <div class="ctx-ligne">
            <span>Contexte utilisé · ≈ {{ tokens() | number }} / {{ contexteMax() | number }} tokens</span>
            <span class="ctx-droite">
              <b>{{ pourcentageContexte() }} %</b>
              <button class="btn petit" type="button" (click)="compacter()" [disabled]="messages().length < 2 || attente() || compactage()"
                      [class.primaire]="pourcentageContexte() >= 70 && messages().length >= 2"
                      title="Résume la conversation pour libérer du contexte">
                <app-icone nom="relancer" [taille]="13" />{{ compactage() ? 'Compactage…' : 'Compacter' }}
              </button>
            </span>
          </div>
          <div class="ctx-barre" role="progressbar" [attr.aria-valuenow]="pourcentageContexte()" aria-valuemin="0" aria-valuemax="100">
            <span [style.width.%]="pourcentageContexte() > 100 ? 100 : pourcentageContexte()"></span>
          </div>
          @if (pourcentageContexte() >= 70) {
            <div class="ctx-conseil">{{ pourcentageContexte() >= 100 ? 'Contexte plein : les plus anciens messages ne sont plus envoyés.' : 'La conversation devient longue.' }} Compactez-la pour garder le fil.</div>
          }
        </div>

        <form class="saisie" (ngSubmit)="envoyer(question())">
          <textarea class="input" rows="2" [ngModel]="question()" (ngModelChange)="question.set($event)" name="question"
                    placeholder="Écrivez votre question… (Entrée pour envoyer, Maj+Entrée pour aller à la ligne)" maxlength="8000"
                    (keydown.enter)="toucheEntree($event)" aria-label="Votre question"></textarea>
          <button class="btn primaire envoyer" type="submit" [disabled]="!question().trim() || attente()" aria-label="Envoyer">
            <app-icone nom="envoyer" />
          </button>
        </form>
        <p class="confiance"><app-icone nom="bouclier" [taille]="13" /> Mots de passe, clés et e-mails sont masqués avant l'envoi à l'IA.</p>
      </section>

      @if (codeOuvert()) {
        <aside class="carte code" aria-label="Code joint">
          <div class="tete">
            <b>Code joint</b>
            <select class="input petit-select" [ngModel]="langage()" (ngModelChange)="langage.set($event)" aria-label="Langage">
              @for (l of langages; track l.valeur) { <option [value]="l.valeur">{{ l.libelle }}</option> }
            </select>
          </div>
          <textarea class="input zone-code" [ngModel]="code()" (ngModelChange)="code.set($event)" spellcheck="false"
                    placeholder="Collez ici le code dont vous voulez parler (optionnel)" aria-label="Code joint"></textarea>
          <span class="aide">{{ code() ? (code().split('\\n').length | number) + ' ligne(s) jointe(s) à chaque question' : "L'assistant verra ce code avec vos questions." }}</span>
        </aside>
      }
    </div>
  `,
  styles: [`
    .grille { display: grid; grid-template-columns: minmax(0, 1fr); gap: 18px; max-width: 1100px; }
    .grille.avec-code { grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); max-width: 1440px; }
    .chat { display: flex; flex-direction: column; height: calc(100vh - 110px); min-height: 480px; }
    .fil-messages { flex: 1; overflow-y: auto; padding: 22px; display: flex; flex-direction: column; gap: 16px; }
    .accueil { margin: auto; text-align: center; max-width: 560px; }
    .logo-ia {
      width: 52px; height: 52px; margin: 0 auto 12px; border-radius: 14px; display: grid; place-items: center; font-size: 24px; color: #fff;
      background: linear-gradient(135deg, #6d6af8, #4338ca);
    }
    .accueil h2 { font-size: 19px; margin-bottom: 6px; }
    .accueil p { color: var(--texte-2); margin: 0 0 18px; }
    .suggestions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .suggestion {
      text-align: left; border: 1px solid var(--bord); background: var(--surface-2); border-radius: 10px; padding: 10px 12px;
      font: 13px var(--police); color: var(--texte); cursor: pointer;
    }
    .suggestion:hover { border-color: var(--primaire); background: var(--primaire-pale); }
    .contexte { padding: 8px 16px 0; display: flex; flex-direction: column; gap: 5px; border-top: 1px solid var(--bord); }
    .ctx-ligne { display: flex; justify-content: space-between; align-items: center; gap: 10px; font-size: 11.5px; color: var(--texte-2); }
    .ctx-droite { display: inline-flex; align-items: center; gap: 10px; }
    .ctx-ligne b { color: var(--texte); }
    .ctx-barre { height: 6px; border-radius: 999px; background: var(--bord); overflow: hidden; }
    .ctx-barre span { display: block; height: 100%; border-radius: 999px; background: var(--ok); transition: width .3s, background .3s; }
    .contexte.alerte .ctx-barre span { background: var(--majeure); }
    .contexte.critique .ctx-barre span { background: var(--critique); }
    .contexte.critique .ctx-ligne b { color: var(--critique); }
    .ctx-conseil { font-size: 11.5px; color: var(--majeure); }
    .contexte.critique .ctx-conseil { color: var(--critique); }
    .bulle.resume { border-style: dashed; border-color: var(--primaire); background: var(--primaire-pale); }
    .etiquette-resume { font-size: 11px; font-weight: 700; color: var(--primaire-fonce); margin-bottom: 6px; }
    .titre-prompts { margin: 18px 0 8px; font-size: 12px; letter-spacing: .08em; text-transform: uppercase; color: var(--texte-3); }
    .suggestion.perso { border-style: dashed; }
    .menu-prompts { position: relative; }
    .liste-prompts { position: absolute; right: 0; top: calc(100% + 6px); z-index: 20; width: 340px; max-height: 360px; overflow-y: auto; padding: 6px; display: flex; flex-direction: column; gap: 2px; }
    .entree { all: unset; cursor: pointer; display: flex; flex-direction: column; gap: 2px; padding: 8px 10px; border-radius: 8px; font: 13px var(--police); }
    .entree:hover, .entree:focus-visible { background: var(--primaire-pale); }
    .entree span { color: var(--texte-2); font-size: 12px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
    .officiel { font-style: normal; font-size: 10.5px; padding: 1px 6px; border-radius: 999px; background: var(--primaire-pale); color: var(--primaire-fonce); margin-left: 4px; }
    .vide { margin: 0; padding: 8px 10px; color: var(--texte-2); font-size: 13px; }
    .gerer { padding: 8px 10px; font-size: 12.5px; border-top: 1px solid var(--bord); margin-top: 4px; }
    .message { display: flex; gap: 10px; align-items: flex-start; }
    .message.moi { justify-content: flex-end; }
    .avatar-ia {
      width: 28px; height: 28px; border-radius: 8px; flex-shrink: 0; display: grid; place-items: center; color: #fff; font-size: 13px;
      background: linear-gradient(135deg, #6d6af8, #4338ca);
    }
    .bulle { max-width: 82%; min-width: 0; background: var(--surface-2); border: 1px solid var(--bord); border-radius: 4px 14px 14px 14px; padding: 12px 14px; }
    .moi .bulle { background: var(--primaire); color: #fff; border-color: var(--primaire); border-radius: 14px 4px 14px 14px; }
    .texte { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 13.5px; line-height: 1.55; }
    .meta { margin-top: 8px; font-size: 11.5px; color: var(--texte-3); }
    .ecrit { display: flex; gap: 5px; padding: 4px 2px; }
    .ecrit i { width: 7px; height: 7px; border-radius: 50%; background: var(--texte-3); animation: rebond 1s infinite ease-in-out; }
    .ecrit i:nth-child(2) { animation-delay: .15s; } .ecrit i:nth-child(3) { animation-delay: .3s; }
    @keyframes rebond { 0%, 80%, 100% { opacity: .3; transform: translateY(0); } 40% { opacity: 1; transform: translateY(-4px); } }
    .erreur { background: var(--critique-pale); color: var(--critique); border-radius: 10px; padding: 10px 12px; font-size: 13px; display: flex; gap: 10px; align-items: center; justify-content: space-between; }
    .saisie { display: flex; gap: 10px; padding: 10px 16px 6px; }
    .saisie textarea { flex: 1; resize: none; font-family: var(--police); font-size: 14px; }
    .envoyer { height: auto; width: 46px; padding: 0; }
    .confiance { margin: 0; padding: 0 16px 12px; font-size: 11.5px; color: var(--texte-3); display: flex; gap: 6px; align-items: center; }
    .code { display: flex; flex-direction: column; gap: 10px; padding: 14px; height: calc(100vh - 110px); min-height: 480px; }
    .tete { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
    .petit-select { height: 32px; font-size: 13px; }
    .zone-code { flex: 1; resize: none; background: #111427; color: #e5e7ff; border-color: #1f2444; }
    .aide { font-size: 12px; color: var(--texte-3); }
    @media (max-width: 900px) {
      .grille.avec-code { grid-template-columns: 1fr; }
      .suggestions { grid-template-columns: 1fr; }
      .code { height: 360px; min-height: 0; }
    }
  `],
})
export class AssistantComponent implements OnInit, AfterViewChecked {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly notif = inject(NotifService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly flux = inject(FluxIaService);
  private readonly transfert = inject(TransfertCodeService);
  private readonly fil = viewChild<ElementRef<HTMLElement>>('fil');

  readonly suggestions = SUGGESTIONS;
  readonly langages = LANGAGES;
  readonly messages = signal<Bulle[]>([]);
  readonly question = signal('');
  readonly attente = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly codeOuvert = signal(false);
  readonly code = signal('');
  readonly langage = signal('auto');
  readonly prompts = signal<PromptUtilisateur[]>([]);
  readonly menuPrompts = signal(false);
  readonly compactage = signal(false);
  readonly contexteMax = signal(12000);
  readonly tokens = computed(() => {
    const historique = this.messages().reduce((n, m) => n + estimerTokens(m.contenu), 0);
    const joint = this.codeOuvert() ? estimerTokens(this.code()) : 0;
    return historique + joint + estimerTokens(this.question());
  });
  readonly pourcentageContexte = computed(() => Math.round((this.tokens() / this.contexteMax()) * 100));
  private defiler = false;

  /** Place un prompt enregistré dans la zone de saisie (à la suite du texte déjà écrit) ; l'utilisateur peut le compléter avant d'envoyer. */
  inserer(p: PromptUtilisateur): void {
    const actuel = this.question().trim();
    this.question.set(actuel ? `${actuel}\n${p.contenu}` : p.contenu);
    this.menuPrompts.set(false);
  }

  ngOnInit(): void {
    this.api.systeme().subscribe({ next: (s) => this.contexteMax.set(s.contexteMaxTokens || 12000), error: () => undefined });
    this.api.mesPrompts().subscribe({
      next: (ps) => {
        this.prompts.set(ps);
        const voulu = Number(this.route.snapshot.queryParamMap.get('prompt'));
        const trouve = voulu ? ps.find((p) => p.id === voulu) : undefined;
        if (trouve) {
          this.inserer(trouve);
        }
        if (voulu) {
          this.router.navigate([], { queryParams: {}, replaceUrl: true });
        }
      },
      error: () => this.prompts.set([]),
    });
    try {
      const sauve = sessionStorage.getItem(CLE);
      if (sauve) {
        this.messages.set(JSON.parse(sauve) as Bulle[]);
      }
    } catch {
      /* stockage indisponible : conversation vide */
    }
    const recu = this.transfert.prendre();
    if (recu) {
      this.code.set(recu.code);
      this.langage.set(recu.langage);
      this.codeOuvert.set(true);
    }
    this.defiler = true;
  }

  ngAfterViewChecked(): void {
    const el = this.fil()?.nativeElement;
    if (this.defiler && el) {
      el.scrollTop = el.scrollHeight;
      this.defiler = false;
    }
  }

  toucheEntree(e: Event): void {
    const k = e as KeyboardEvent;
    if (!k.shiftKey) {
      k.preventDefault();
      this.envoyer(this.question());
    }
  }

  envoyer(texte: string): void {
    const q = texte.trim();
    if (!q || this.attente()) {
      return;
    }
    this.messages.update((m) => [...m, { role: 'utilisateur', contenu: q }]);
    this.question.set('');
    this.demander();
  }

  /** Remplace toute la conversation par un résumé rédigé par l'IA (libère du contexte, garde le fil). */
  compacter(): void {
    const historique = this.messages();
    if (historique.length < 2 || this.attente() || this.compactage()) {
      return;
    }
    const avant = this.tokens();
    this.compactage.set(true);
    this.api.compacter(historique.map(({ role, contenu }) => ({ role, contenu }))).subscribe({
      next: (r) => {
        const resume = r.contenu.trim();
        this.messages.set([{ role: 'assistant', contenu: resume, compacte: true, meta: `Compactée · ${historique.length} messages → 1 résumé` }]);
        this.sauver();
        this.compactage.set(false);
        this.defiler = true;
        this.auth.rafraichir().subscribe({ error: () => undefined });
        const gagne = Math.max(0, avant - this.tokens());
        this.notif.succes(`Conversation compactée : environ ${gagne} tokens libérés.`);
      },
      error: (e) => {
        this.compactage.set(false);
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  reessayer(): void {
    this.demander();
  }

  nouvelle(): void {
    this.messages.set([]);
    this.erreur.set(null);
    this.sauver();
  }

  private demander(): void {
    this.attente.set(true);
    this.erreur.set(null);
    this.defiler = true;
    const historique = this.messages().map(({ role, contenu }) => ({ role, contenu }));
    const code = this.codeOuvert() ? this.code() : '';
    this.messages.update((m) => [...m, { role: 'assistant', contenu: '', enCours: true }]);
    const majDernier = (f: (b: Bulle) => Bulle) => this.messages.update((m) => [...m.slice(0, -1), f(m[m.length - 1])]);
    this.flux.discuter(historique, code, this.langage()).subscribe((ev) => {
      if (ev.type === 'morceau') {
        majDernier((b) => ({ ...b, contenu: b.contenu + ev.texte }));
      } else if (ev.type === 'fin') {
        const secrets = ev.info.nbMasquages ? ` · ${ev.info.nbMasquages} secret(s) masqué(s)` : '';
        majDernier((b) => ({
          role: 'assistant', contenu: b.contenu.trim(),
          meta: `${ev.info.modele} · ${(ev.info.dureeMs / 1000).toFixed(1).replace('.', ',')} s${secrets}`,
        }));
        this.attente.set(false);
        this.sauver();
        this.auth.rafraichir().subscribe({ error: () => undefined });
      } else {
        // Réponse incomplète : on la retire pour pouvoir réessayer proprement
        this.messages.update((m) => m.slice(0, -1));
        this.erreur.set(ev.message);
        this.attente.set(false);
      }
      this.defiler = true;
    });
  }

  private sauver(): void {
    try {
      sessionStorage.setItem(CLE, JSON.stringify(this.messages()));
    } catch {
      /* ignoré */
    }
  }
}
