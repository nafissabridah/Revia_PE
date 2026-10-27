import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { messageErreur } from '../../core/http';
import {
  ActionPlan, Alertes, PlanAdmin, ProfilCompetences, PromptSysteme, PromptUtilisateur, RecommandationQuota, RecommandationsQuotas, ResultatAction,
  SystemeInfo, Utilisateur,
} from '../../core/models';
import { NotifService } from '../../core/notif.service';
import { IconeComponent } from '../../shared/icone.component';
import { ReponseIaComponent } from '../../shared/reponse-ia.component';

type Onglet = 'utilisateurs' | 'quotas' | 'prompt' | 'partages' | 'alertes';
type Filtre = 'tous' | 'DEVELOPPEUR' | 'ADMIN' | 'bloque';

/** Administration : comptes et rôles, quotas, versions du prompt système. */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, DatePipe, IconeComponent, ReponseIaComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="barre"><div class="fil">Administration / <b>{{ titreOnglet() }}</b></div></header>
    <div class="page">
      <div class="entete">
        <div>
          <h1 class="titre-page">Administration</h1>
          <p class="sous-titre">Gérez les comptes et leurs rôles, les quotas journaliers et le prompt système de la revue.</p>
        </div>
        @if (systeme(); as s) {
          <div class="etat-ia" [class.ko]="!s.disponible" [title]="'Température ' + s.temperature + ' · délai ' + s.timeoutSec + ' s'">
            <span class="point"></span>
            <div>
              <b>Module IA : {{ s.fournisseur }}</b>
              <span>{{ s.modele }} · {{ s.disponible ? 'opérationnel' : "clé d'API absente (REVIA_AI_API_KEY)" }}</span>
            </div>
          </div>
        }
      </div>

      <section class="kpis" aria-label="Indicateurs">
        <div class="carte kpi"><span class="kpi-n">{{ stats().total }}</span><span class="kpi-l">Comptes</span></div>
        <div class="carte kpi"><span class="kpi-n">{{ stats().developpeurs }}</span><span class="kpi-l">Développeurs</span></div>
        <div class="carte kpi"><span class="kpi-n">{{ stats().admins }}</span><span class="kpi-l">Administrateurs</span></div>
        <div class="carte kpi" [class.alerte]="stats().bloques > 0"><span class="kpi-n">{{ stats().bloques }}</span><span class="kpi-l">Comptes bloqués</span></div>
        <div class="carte kpi"><span class="kpi-n">{{ stats().revuesJour }}</span><span class="kpi-l">Revues aujourd'hui</span></div>
        <div class="carte kpi"><span class="kpi-n">{{ stats().assistantJour }}</span><span class="kpi-l">Requêtes assistant aujourd'hui</span></div>
        <div class="carte kpi" [class.alerte]="nbCritiques() > 0"><span class="kpi-n">{{ alertes()?.alertes?.length ?? 0 }}</span><span class="kpi-l">Alertes d'usage</span></div>
      </section>

      <nav class="onglets" role="tablist" aria-label="Sections d'administration">
        <button type="button" role="tab" [class.actif]="onglet() === 'utilisateurs'" [attr.aria-selected]="onglet() === 'utilisateurs'" (click)="onglet.set('utilisateurs')">
          <app-icone nom="utilisateurs" [taille]="15" />Utilisateurs <span class="compte">{{ stats().total }}</span>
        </button>
        <button type="button" role="tab" [class.actif]="onglet() === 'quotas'" [attr.aria-selected]="onglet() === 'quotas'" (click)="onglet.set('quotas')">
          <app-icone nom="tableau" [taille]="15" />Quotas
        </button>
        <button type="button" role="tab" [class.actif]="onglet() === 'prompt'" [attr.aria-selected]="onglet() === 'prompt'" (click)="onglet.set('prompt')">
          <app-icone nom="prompt" [taille]="15" />Prompt système <span class="compte">{{ prompts().length }}</span>
        </button>
        <button type="button" role="tab" [class.actif]="onglet() === 'partages'" [attr.aria-selected]="onglet() === 'partages'" (click)="onglet.set('partages')">
          <app-icone nom="chat" [taille]="15" />Prompts partagés <span class="compte">{{ partages().length }}</span>
        </button>
        <button type="button" role="tab" [class.actif]="onglet() === 'alertes'" [attr.aria-selected]="onglet() === 'alertes'" (click)="onglet.set('alertes'); chargerAlertes(true)">
          <app-icone nom="bouclier" [taille]="15" />Alertes <span class="compte" [class.rouge]="nbCritiques() > 0">{{ alertes()?.alertes?.length ?? 0 }}</span>
        </button>
      </nav>

      @switch (onglet()) {
        @case ('utilisateurs') {
          @if (motDePasseTemporaire(); as t) {
            <div class="carte temporaire" role="status">
              <div>
                <b>Mot de passe temporaire de {{ t.nom }}</b>
                <p class="aide">Affiché une seule fois : transmettez-le à l'utilisateur, qui pourra le changer dans Paramètres.</p>
              </div>
              <code>{{ t.valeur }}</code>
              <div class="ligne-actions">
                <button class="btn petit" type="button" (click)="copierTemporaire(t.valeur)">Copier</button>
                <button class="btn petit" type="button" (click)="motDePasseTemporaire.set(null)">Fermer</button>
              </div>
            </div>
          }

          <form class="carte assistant-admin" (ngSubmit)="interpreter()">
            <div class="aa-titre"><app-icone nom="etoile" [taille]="16" /><b>Assistant d'administration</b>
              <span class="aide">Décrivez ce que vous voulez faire : l'assistant propose des actions, vous confirmez avant toute exécution.</span></div>
            <div class="aa-saisie">
              <input class="input" name="commande" [ngModel]="commande()" (ngModelChange)="commande.set($event)" maxlength="500"
                     placeholder="Ex. : bloque les comptes inactifs depuis 30 jours · passe Sarra en administrateur · mets le quota de Sarra à 30"
                     aria-label="Commande d'administration" />
              <button class="btn primaire" type="submit" [disabled]="!commande().trim() || interpretation()">{{ interpretation() ? 'Analyse…' : 'Interpréter' }}</button>
            </div>
            @if (plan(); as p) {
              <div class="plan">
                <p class="aide"><span class="badge neutre">{{ p.source === 'ia' ? 'Interprété par l’IA' : 'Règles de secours' }}</span> {{ p.interpretation }}</p>
                @for (w of p.avertissements; track $index) { <p class="avert">⚠ {{ w }}</p> }
                @if (p.actions.length) {
                  <ul class="actions-plan">
                    @for (a of p.actions; track $index) {
                      <li><label><input type="checkbox" [checked]="selection().has($index)" (change)="basculer($index)" /> {{ a.libelle }}</label></li>
                    }
                  </ul>
                  <div class="ligne-actions fin">
                    <button class="btn" type="button" (click)="annulerPlan()">Annuler</button>
                    <button class="btn primaire" type="button" (click)="executerPlan()" [disabled]="!selection().size || execution()">
                      {{ execution() ? 'Exécution…' : 'Confirmer et exécuter (' + selection().size + ')' }}
                    </button>
                  </div>
                } @else {
                  <div class="ligne-actions fin"><button class="btn" type="button" (click)="annulerPlan()">Fermer</button></div>
                }
              </div>
            }
            @if (resultats(); as rs) {
              <div class="plan">
                @for (r of rs; track $index) { <p [class.ok]="r.ok" [class.ko]="!r.ok">{{ r.ok ? '✔' : '✘' }} {{ r.nom }} : {{ r.message }}</p> }
                <div class="ligne-actions fin"><button class="btn petit" type="button" (click)="resultats.set(null)">Fermer</button></div>
              </div>
            }
          </form>

          @if (profil(); as pr) {
            <div class="carte profil">
              <div class="profil-tete">
                <div><h2 class="titre-section">Profil de compétences · {{ pr.nom }}</h2>
                  <span class="badge neutre">{{ pr.source === 'ia' ? 'Synthèse rédigée par l’IA' : 'Synthèse par règles' }}</span></div>
                <button class="btn petit" type="button" (click)="profil.set(null)">Fermer</button>
              </div>
              <div class="profil-chiffres">
                <div><b>{{ pr.nbRevues }}</b><span>revue(s) · 180 j</span></div>
                <div><b>{{ pr.scoreMoyen }}/100</b><span>score moyen</span></div>
                <div><b>{{ pr.scoreInitial }} → {{ pr.scoreRecent }}</b><span>{{ libelleTendance(pr.tendance) }}</span></div>
                <div><b>{{ pr.nbMasquages }}</b><span>secret(s) masqué(s)</span></div>
              </div>
              @if (pr.recurrents.length) {
                <div class="puces">
                  @for (r of pr.recurrents; track r.titre) { <span class="puce">{{ r.titre }} <b>×{{ r.occurrences }}</b></span> }
                </div>
              }
              <app-reponse-ia [contenu]="pr.synthese" [actions]="false" />
            </div>
          } @else if (profilEnCours()) {
            <div class="carte profil"><p class="aide">Analyse du profil en cours…</p></div>
          }

          <div class="outils">
            <input class="input recherche" type="search" placeholder="Rechercher un nom ou un e-mail…" aria-label="Rechercher un utilisateur"
                   [ngModel]="recherche()" (ngModelChange)="recherche.set($event)" />
            <select class="input" aria-label="Filtrer" [ngModel]="filtre()" (ngModelChange)="filtre.set($event)">
              <option value="tous">Tous les comptes</option>
              <option value="DEVELOPPEUR">Développeurs</option>
              <option value="ADMIN">Administrateurs</option>
              <option value="bloque">Comptes bloqués</option>
            </select>
            <span class="aide resultat">{{ visibles().length }} / {{ utilisateurs().length }}</span>
            <button class="btn primaire" type="button" (click)="ouvrirCreation()"><app-icone nom="nouvelle" [taille]="15" />Nouvel utilisateur</button>
          </div>

          @if (formulaire(); as f) {
            <form class="carte formulaire" (ngSubmit)="enregistrer()" #form="ngForm">
              <h2 class="titre-section">{{ f.mode === 'creation' ? 'Nouvel utilisateur' : "Modifier l'utilisateur" }}</h2>
              <div class="grille-form">
                <div class="champ"><label for="f-nom">Nom</label>
                  <input id="f-nom" name="nom" class="input" [(ngModel)]="saisie.nom" required minlength="2" maxlength="80" /></div>
                <div class="champ"><label for="f-email">E-mail</label>
                  <input id="f-email" name="email" type="email" class="input" [(ngModel)]="saisie.email" required maxlength="160" /></div>
                <div class="champ"><label for="f-mdp">{{ f.mode === 'creation' ? 'Mot de passe' : 'Nouveau mot de passe (facultatif)' }}</label>
                  <input id="f-mdp" name="motDePasse" type="password" class="input" autocomplete="new-password" [(ngModel)]="saisie.motDePasse"
                    [required]="f.mode === 'creation'" minlength="8" maxlength="72" />
                  <span class="aide">8 caractères minimum, avec au moins une lettre et un chiffre.</span></div>
                @if (f.mode === 'creation') {
                  <div class="champ"><label for="f-role">Rôle</label>
                    <select id="f-role" name="role" class="input" [(ngModel)]="saisie.role">
                      <option value="DEVELOPPEUR">Développeur</option>
                      <option value="ADMIN">Administrateur</option>
                    </select></div>
                }
                @if (f.mode === 'creation' ? saisie.role === 'DEVELOPPEUR' : f.role === 'DEVELOPPEUR') {
                  <div class="champ"><label for="f-quota">Quota de revues / jour</label>
                    <input id="f-quota" name="quota" type="number" min="0" max="10000" class="input" [(ngModel)]="saisie.quota" /></div>
                  <div class="champ"><label for="f-quota-a">Quota assistant / jour</label>
                    <input id="f-quota-a" name="quotaAssistant" type="number" min="0" max="10000" class="input" [(ngModel)]="saisie.quotaAssistant" /></div>
                }
              </div>
              <div class="ligne-actions fin">
                <button class="btn" type="button" (click)="formulaire.set(null)">Annuler</button>
                <button class="btn primaire" type="submit" [disabled]="form.invalid || envoi()">{{ f.mode === 'creation' ? 'Créer le compte' : 'Enregistrer' }}</button>
              </div>
            </form>
          }

          <div class="carte">
            <div class="defile">
              <table>
                <thead>
                  <tr><th>Utilisateur</th><th>Rôle</th><th>Statut</th><th>Activité du jour</th><th>Revues</th><th>Dernière connexion</th><th class="droite">Actions</th></tr>
                </thead>
                <tbody>
                  @for (u of visibles(); track u.id) {
                    <tr [class.bloque]="u.bloque">
                      <td>
                        <div class="identite">
                          <span class="avatar" [class.admin]="u.role === 'ADMIN'">{{ initiales(u.nom) }}</span>
                          <div><b>{{ u.nom }}</b>@if (u.id === auth.utilisateur()?.id) { <span class="vous">vous</span> }<br /><span class="aide">{{ u.email }}</span></div>
                        </div>
                      </td>
                      <td>
                        @if (u.id !== auth.utilisateur()?.id) {
                          <select class="input role" (change)="changerRole(u, $any($event.target))" [attr.aria-label]="'Rôle de ' + u.nom">
                            <option value="DEVELOPPEUR" [selected]="u.role === 'DEVELOPPEUR'">Développeur</option>
                            <option value="ADMIN" [selected]="u.role === 'ADMIN'">Administrateur</option>
                          </select>
                        } @else { <span class="badge neutre">Administrateur</span> }
                      </td>
                      <td><span class="badge" [class.s-corrigee]="!u.bloque" [class.s-a_corriger]="u.bloque">{{ u.bloque ? 'Bloqué' : 'Actif' }}</span></td>
                      <td>
                        @if (u.role === 'DEVELOPPEUR') {
                          <div class="activite">
                            <div><span>Revues</span><b>{{ u.revuesDuJour ?? 0 }} / {{ u.quotaJournalier ?? 0 }}</b></div>
                            <div class="jauge" [class.plein]="pct(u.revuesDuJour, u.quotaJournalier) >= 100"><span [style.width.%]="pct(u.revuesDuJour, u.quotaJournalier)"></span></div>
                            <div><span>Assistant</span><b>{{ u.assistantDuJour ?? 0 }} / {{ u.quotaAssistant ?? 0 }}</b></div>
                            <div class="jauge" [class.plein]="pct(u.assistantDuJour, u.quotaAssistant) >= 100"><span [style.width.%]="pct(u.assistantDuJour, u.quotaAssistant)"></span></div>
                          </div>
                        } @else { <span class="aide">Non limité</span> }
                      </td>
                      <td>{{ u.nbRevues ?? 0 }}</td>
                      <td>@if (u.derniereConnexion) { {{ u.derniereConnexion | date: 'dd/MM/yyyy HH:mm' }} } @else { <span class="aide">Jamais</span> }<br /><span class="aide">inscrit le {{ u.creeLe | date: 'dd/MM/yyyy' }}</span></td>
                      <td class="droite">
                        <div class="ligne-actions fin">
                          <button class="btn petit" type="button" (click)="ouvrirEdition(u)" [attr.aria-label]="'Modifier ' + u.nom">Modifier</button>
                          @if (u.role === 'DEVELOPPEUR') {
                            <button class="btn petit" type="button" (click)="voirProfil(u)" [attr.aria-label]="'Profil de compétences de ' + u.nom">Profil</button>
                          }
                          <button class="btn petit" type="button" (click)="reinitialiser(u)" [attr.aria-label]="'Réinitialiser le mot de passe de ' + u.nom">Mot de passe</button>
                          @if (u.id !== auth.utilisateur()?.id) {
                            <details class="plus">
                              <summary class="btn petit" [attr.aria-label]="'Plus d\\'actions pour ' + u.nom">⋯</summary>
                              <div class="menu" role="menu">
                                <button type="button" role="menuitem" (click)="fermer($event); bloquer(u)">{{ u.bloque ? 'Débloquer le compte' : 'Bloquer le compte' }}</button>
                                <button type="button" role="menuitem" class="danger" (click)="fermer($event); supprimer(u)">Supprimer le compte</button>
                              </div>
                            </details>
                          }
                        </div>
                      </td>
                    </tr>
                  } @empty {
                    <tr><td colspan="7" class="vide">Aucun compte ne correspond à votre recherche.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        @case ('quotas') {
          <form class="carte defauts" (ngSubmit)="enregistrerDefauts()">
            <div class="defauts-texte">
              <h2 class="titre-section">Quotas par défaut</h2>
              <p class="aide">Appliqués aux nouveaux comptes (inscription et création par l'administrateur).</p>
            </div>
            <div class="champ"><label for="d-revues">Revues / jour</label>
              <input id="d-revues" name="defautRevues" type="number" min="0" max="10000" class="input" [(ngModel)]="defauts.quotaRevuesDefaut" /></div>
            <div class="champ"><label for="d-assist">Assistant / jour</label>
              <input id="d-assist" name="defautAssistant" type="number" min="0" max="10000" class="input" [(ngModel)]="defauts.quotaAssistantDefaut" /></div>
            <label class="case"><input type="checkbox" name="appliquerATous" [(ngModel)]="defauts.appliquerATous" /> Appliquer aussi à tous les développeurs existants</label>
            <button class="btn primaire" type="submit">Enregistrer</button>
          </form>

          @if (systeme(); as s) {
            <div class="carte debit">
              <div>
                <h2 class="titre-section">Limitation de débit (Bucket4j)</h2>
                <p class="aide">En plus des quotas journaliers : un seau de jetons par utilisateur ou par adresse IP, rechargé chaque minute. Au-delà : réponse 429 avec <code>Retry-After</code> ; chaque réponse porte <code>X-RateLimit-Limit</code> et <code>X-RateLimit-Remaining</code>.</p>
              </div>
              <div class="debit-valeurs">
                <div><b>{{ s.analysesParMinute }} / min</b><span>Revues, assistant et outils, par utilisateur</span></div>
                <div><b>{{ s.connexionsParMinute }} / min</b><span>Connexions et inscriptions, par adresse IP</span></div>
              </div>
            </div>
          }

          <div class="carte recos">
            <div class="recos-tete">
              <div><h2 class="titre-section">Quotas conseillés par l’IA</h2>
                <p class="aide">Analyse de la consommation des 30 derniers jours : quota conseillé = pic d'usage + 25 %, arrondi aux 5 supérieurs.</p></div>
              <button class="btn primaire" type="button" (click)="analyserQuotas()" [disabled]="recosEnCours()">{{ recosEnCours() ? 'Analyse…' : 'Analyser la consommation' }}</button>
            </div>
            @if (recos(); as rc) {
              <p class="commentaire"><span class="badge neutre">{{ rc.source === 'ia' ? 'Commentaire IA' : 'Analyse par règles' }}</span> {{ rc.commentaire }}</p>
              <div class="defile">
                <table class="petite">
                  <thead><tr><th>Développeur</th><th>Activité (30 j)</th><th>Quotas actuels</th><th>Quotas conseillés</th><th class="droite">Action</th></tr></thead>
                  <tbody>
                    @for (r of rc.recommandations; track r.utilisateurId) {
                      <tr>
                        <td><b>{{ r.nom }}</b></td>
                        <td class="aide">{{ r.joursActifs }} j actif(s) · pic {{ r.picRevues }} revues / {{ r.picAssistant }} assistant</td>
                        <td>{{ r.quotaRevues }} / {{ r.quotaAssistant }}</td>
                        <td [class.change]="r.modificationUtile"><b>{{ r.recommandeRevues }} / {{ r.recommandeAssistant }}</b></td>
                        <td class="droite">
                          @if (r.modificationUtile) { <button class="btn petit primaire" type="button" (click)="appliquerReco(r)">Appliquer</button> }
                          @else { <span class="aide">Déjà adapté</span> }
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
              @if (nbAChanger() > 0) {
                <div class="ligne-actions fin"><button class="btn primaire" type="button" (click)="toutAppliquer()">Tout appliquer ({{ nbAChanger() }})</button></div>
              }
            }
          </div>

          <div class="carte">
            <div class="defile">
              <table>
                <thead>
                  <tr><th>Développeur</th><th>Revues aujourd'hui</th><th>Quota de revues</th><th>Assistant aujourd'hui</th><th>Quota assistant</th><th class="droite">Enregistrer</th></tr>
                </thead>
                <tbody>
                  @for (u of developpeurs(); track u.id) {
                    <tr [class.bloque]="u.bloque">
                      <td>
                        <div class="identite">
                          <span class="avatar">{{ initiales(u.nom) }}</span>
                          <div><b>{{ u.nom }}</b><br /><span class="aide">{{ u.email }}</span></div>
                        </div>
                      </td>
                      <td>{{ u.revuesDuJour ?? 0 }}</td>
                      <td><input class="input nombre" type="number" min="0" max="10000" [(ngModel)]="quotas[u.id]" [attr.aria-label]="'Quota de revues de ' + u.nom" /></td>
                      <td>{{ u.assistantDuJour ?? 0 }}</td>
                      <td><input class="input nombre" type="number" min="0" max="10000" [(ngModel)]="quotasAssistant[u.id]" [attr.aria-label]="'Quota assistant de ' + u.nom" /></td>
                      <td class="droite">
                        <button class="btn petit primaire" type="button" (click)="definirQuota(u)"
                          [disabled]="quotas[u.id] === u.quotaJournalier && quotasAssistant[u.id] === u.quotaAssistant">Enregistrer</button>
                      </td>
                    </tr>
                  } @empty {
                    <tr><td colspan="6" class="vide">Aucun développeur pour le moment.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        @case ('alertes') {
          <div class="outils">
            <span class="aide resultat">Détection automatique : pics d'activité, secrets en dur répétés, connexions échouées, quotas presque atteints.</span>
            <button class="btn" type="button" (click)="chargerAlertes(true)"><app-icone nom="relancer" [taille]="14" />Actualiser</button>
          </div>
          @if (alertes(); as al) {
            <div class="carte resume-alertes"><span class="badge neutre">{{ al.source === 'ia' ? 'Résumé IA' : 'Résumé par règles' }}</span> {{ al.resume }}</div>
            @for (a of al.alertes; track $index) {
              <div class="carte alerte-carte" [class.critique]="a.niveau === 'critique'" [class.attention]="a.niveau === 'attention'">
                <div class="alerte-tete">
                  <span class="badge" [class.g-critique]="a.niveau === 'critique'" [class.g-majeure]="a.niveau === 'attention'" [class.g-info]="a.niveau === 'info'">{{ a.niveau }}</span>
                  <b>{{ a.message }}</b><span class="aide">· {{ a.nom }}</span>
                </div>
                <p class="aide">{{ a.detail }}</p>
              </div>
            } @empty {
              <div class="carte vide">✔ Aucune anomalie détectée : l'activité est dans la normale.</div>
            }
          }
        }

        @case ('partages') {
          <p class="aide intro">Prompts « officiels » : visibles de tous les développeurs dans l'Assistant IA et dans « Mes prompts » (lecture seule pour eux).</p>
          <div class="outils">
            <span class="aide resultat">{{ partages().length }} prompt(s) partagé(s)</span>
            <button class="btn primaire" type="button" (click)="nouveauPartage()"><app-icone nom="nouvelle" [taille]="15" />Nouveau prompt partagé</button>
          </div>
          @if (partageEdition(); as e) {
            <form class="carte formulaire" (ngSubmit)="enregistrerPartage()" #fp="ngForm">
              <h2 class="titre-section">{{ e.id ? 'Modifier le prompt partagé' : 'Nouveau prompt partagé' }}</h2>
              <div class="champ"><label for="ps-titre">Titre</label>
                <input id="ps-titre" name="titre" class="input" [(ngModel)]="partageTitre" required minlength="2" maxlength="80" /></div>
              <div class="champ"><label for="ps-contenu">Prompt</label>
                <textarea id="ps-contenu" name="contenu" class="input" rows="6" [(ngModel)]="partageContenu" required maxlength="4000"></textarea></div>
              <div class="ligne-actions fin">
                <button class="btn" type="button" (click)="partageEdition.set(null)">Annuler</button>
                <button class="btn primaire" type="submit" [disabled]="fp.invalid || envoi()">{{ e.id ? 'Enregistrer' : 'Publier' }}</button>
              </div>
            </form>
          }
          <div class="carte">
            <div class="defile">
              <table>
                <thead><tr><th>Titre</th><th>Prompt</th><th>Modifié le</th><th class="droite">Actions</th></tr></thead>
                <tbody>
                  @for (p of partages(); track p.id) {
                    <tr>
                      <td><b>{{ p.titre }}</b></td>
                      <td class="apercu">{{ p.contenu }}</td>
                      <td>{{ p.modifieLe | date: 'dd/MM/yyyy HH:mm' }}</td>
                      <td class="droite"><div class="ligne-actions fin">
                        <button class="btn petit" type="button" (click)="modifierPartage(p)">Modifier</button>
                        <button class="btn petit danger" type="button" (click)="supprimerPartage(p)">Supprimer</button>
                      </div></td>
                    </tr>
                  } @empty {
                    <tr><td colspan="4" class="vide">Aucun prompt partagé pour le moment.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }

        @case ('prompt') {
          <p class="aide intro">Le prompt système pilote la revue de code. Publier une version l'active immédiatement ; les anciennes versions restent consultables.</p>
          <div class="prompts">
            <div class="carte versions">
              @for (p of prompts(); track p.id) {
                <button type="button" class="version" [class.selection]="p.id === promptVu()?.id" (click)="promptVu.set(p); edition.set(false)">
                  <b>{{ p.version }}</b> @if (p.actif) { <span class="badge s-corrigee">actif</span> }
                  <span class="aide">{{ p.creeLe | date: 'dd/MM/yyyy HH:mm' }}</span>
                </button>
              }
              <button type="button" class="btn petit" (click)="nouveau()">+ Nouvelle version</button>
            </div>
            <div class="carte editeur-prompt">
              @if (edition()) {
                <div class="champ"><label for="v">Version</label><input id="v" class="input" [(ngModel)]="nouvelleVersion" placeholder="v2" maxlength="20" /></div>
                <textarea class="input" rows="18" [(ngModel)]="nouveauContenu" aria-label="Contenu du prompt"></textarea>
                <p class="aide">Variables disponibles : {{ '{' }}langage{{ '}' }} et {{ '{' }}mode{{ '}' }}. La nouvelle version devient active immédiatement.</p>
                <div class="ligne-actions fin">
                  <button class="btn" type="button" (click)="edition.set(false)">Annuler</button>
                  <button class="btn primaire" type="button" (click)="publier()">Publier et activer</button>
                </div>
              } @else {
                @if (promptVu(); as p) {
                  <pre>{{ p.contenu }}</pre>
                  @if (!p.actif) { <div class="ligne-actions fin"><button class="btn" type="button" (click)="activer(p)">Activer cette version</button></div> }
                }
              }
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .entete { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
    .etat-ia { display: flex; gap: 10px; align-items: center; padding: 8px 14px; border: 1px solid var(--bord); border-radius: 10px; background: var(--surface); font-size: 12.5px; }
    .etat-ia div { display: flex; flex-direction: column; }
    .etat-ia span:not(.point) { color: var(--texte-2); }
    .etat-ia .point { width: 9px; height: 9px; border-radius: 50%; background: var(--ok); box-shadow: 0 0 0 3px var(--ok-pale); }
    .etat-ia.ko .point { background: var(--critique); box-shadow: 0 0 0 3px var(--critique-pale); }

    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 18px; }
    .kpi { padding: 12px 14px; display: flex; flex-direction: column; gap: 2px; }
    .kpi-n { font-size: 24px; font-weight: 700; line-height: 1.1; }
    .kpi-l { font-size: 12px; color: var(--texte-2); }
    .kpi.alerte .kpi-n { color: var(--critique); }

    .onglets { display: flex; gap: 4px; border-bottom: 1px solid var(--bord); margin-bottom: 16px; overflow-x: auto; overflow-y: hidden; }
    .onglets button {
      display: inline-flex; align-items: center; gap: 7px; padding: 10px 14px; background: none; border: none; border-bottom: 2px solid transparent;
      color: var(--texte-2); font: 600 13px var(--police); cursor: pointer; white-space: nowrap; margin-bottom: -1px;
    }
    .onglets button:hover { color: var(--texte); }
    .onglets button.actif { color: var(--primaire); border-bottom-color: var(--primaire); }
    .onglets button:focus-visible { outline: 2px solid var(--primaire); outline-offset: -2px; border-radius: 6px; }
    .compte { background: var(--surface-2); border: 1px solid var(--bord); border-radius: 999px; padding: 0 7px; font-size: 11px; color: var(--texte-2); }

    .outils { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }
    .recherche { flex: 1; min-width: 220px; max-width: 360px; }
    .outils select.input { width: 190px; }
    .resultat { margin-right: auto; }

    .aide { color: var(--texte-3); font-size: 12px; margin: 0; }
    .intro { margin-bottom: 12px; }
    .titre-section { font-size: 15px; margin: 0; }

    .temporaire { padding: 14px 16px; margin-bottom: 12px; display: flex; flex-wrap: wrap; gap: 14px; align-items: center; justify-content: space-between; border-left: 4px solid var(--primaire); }
    .temporaire code { font-size: 18px; letter-spacing: .08em; padding: 6px 12px; background: var(--surface-2); border-radius: 8px; }
    .formulaire { padding: 16px; margin-bottom: 12px; display: flex; flex-direction: column; gap: 12px; }
    .grille-form { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
    .ligne-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
    .ligne-actions.fin { justify-content: flex-end; }

    .defile { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; min-width: 900px; }
    th { text-align: left; font-size: 11px; letter-spacing: .08em; text-transform: uppercase; color: var(--texte-3); padding: 11px 14px; border-bottom: 1px solid var(--bord); background: var(--surface-2); white-space: nowrap; }
    td { padding: 11px 14px; border-bottom: 1px solid var(--bord); font-size: 13px; vertical-align: middle; }
    tbody tr:last-child td { border-bottom: none; }
    tbody tr:hover td { background: var(--surface-2); }
    tr.bloque td { background: #fff7f7; }
    .droite { text-align: right; }
    .vide { text-align: center; color: var(--texte-2); padding: 28px; }

    .identite { display: flex; gap: 10px; align-items: center; }
    .avatar { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; font-weight: 700; color: var(--primaire-fonce); background: var(--primaire-pale); flex-shrink: 0; }
    .avatar.admin { color: #fff; background: var(--primaire); }
    .vous { margin-left: 6px; font-size: 10.5px; padding: 1px 6px; border-radius: 999px; background: var(--primaire-pale); color: var(--primaire-fonce); font-weight: 600; }
    select.role { height: 30px; min-width: 140px; font-size: 13px; }

    .activite { display: grid; grid-template-columns: auto 1fr; gap: 3px 10px; align-items: center; min-width: 210px; font-size: 12px; }
    .activite div:not(.jauge) { grid-column: 1 / -1; display: flex; justify-content: space-between; color: var(--texte-2); }
    .activite div:not(.jauge) b { color: var(--texte); }
    .jauge { grid-column: 1 / -1; height: 5px; border-radius: 999px; background: var(--bord); overflow: hidden; margin-bottom: 4px; }
    .jauge span { display: block; height: 100%; background: var(--primaire); border-radius: 999px; }
    .jauge.plein span { background: var(--critique); }

    details.plus { position: relative; }
    details.plus summary { list-style: none; cursor: pointer; }
    details.plus summary::-webkit-details-marker { display: none; }
    .menu { position: absolute; right: 0; top: calc(100% + 4px); z-index: 30; min-width: 190px; padding: 5px; display: flex; flex-direction: column; background: var(--surface); border: 1px solid var(--bord); border-radius: 10px; box-shadow: 0 8px 24px rgba(15, 18, 34, .14); }
    .menu button { all: unset; cursor: pointer; padding: 8px 10px; border-radius: 7px; font: 13px var(--police); text-align: left; }
    .menu button:hover, .menu button:focus-visible { background: var(--surface-2); }
    .menu button.danger { color: var(--critique); }

    .debit { padding: 14px 16px; margin-bottom: 14px; display: flex; flex-wrap: wrap; gap: 16px; align-items: center; justify-content: space-between; }
    .debit p { max-width: 560px; margin-top: 4px; }
    .debit code { background: var(--surface-2); padding: 1px 5px; border-radius: 5px; font-size: 11.5px; }
    .debit-valeurs { display: flex; gap: 12px; flex-wrap: wrap; }
    .debit-valeurs div { display: flex; flex-direction: column; gap: 2px; padding: 8px 14px; border: 1px solid var(--bord); border-radius: 10px; background: var(--surface-2); }
    .debit-valeurs b { font-size: 16px; }
    .debit-valeurs span { font-size: 11.5px; color: var(--texte-2); }
    .apercu { color: var(--texte-2); max-width: 520px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .defauts { padding: 16px; margin-bottom: 14px; display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-end; }
    .defauts-texte { flex: 1; min-width: 220px; display: flex; flex-direction: column; gap: 4px; }
    .defauts .input, .input.nombre { width: 110px; }
    .case { display: flex; gap: 6px; align-items: center; font-size: 13px; }

    .prompts { display: grid; grid-template-columns: 240px 1fr; gap: 14px; align-items: start; }
    .versions { padding: 10px; display: flex; flex-direction: column; gap: 6px; }
    .version { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; text-align: left; padding: 8px 10px; border-radius: 8px; border: 1px solid transparent; background: none; cursor: pointer; font: 13px var(--police); }
    .version.selection { border-color: var(--primaire); background: var(--primaire-pale); }
    .editeur-prompt { padding: 14px; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
    pre { white-space: pre-wrap; font-size: 12.5px; margin: 0; background: var(--surface-2); padding: 12px; border-radius: 8px; max-height: 520px; overflow: auto; }
    .assistant-admin { padding: 14px 16px; margin-bottom: 12px; display: flex; flex-direction: column; gap: 10px; border-left: 4px solid var(--primaire); }
    .aa-titre { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .aa-saisie { display: flex; gap: 8px; }
    .aa-saisie .input { flex: 1; }
    .plan { border-top: 1px dashed var(--bord); padding-top: 10px; display: flex; flex-direction: column; gap: 6px; }
    .plan p { margin: 0; font-size: 13px; }
    .avert { color: var(--majeure); }
    .actions-plan { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; font-size: 13px; }
    .actions-plan label { display: flex; gap: 8px; align-items: center; cursor: pointer; }
    .ok { color: var(--ok); } .ko { color: var(--critique); }
    .profil { padding: 14px 16px; margin-bottom: 12px; display: flex; flex-direction: column; gap: 10px; }
    .profil-tete { display: flex; justify-content: space-between; gap: 10px; align-items: flex-start; }
    .profil-chiffres { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 10px; }
    .profil-chiffres div { display: flex; flex-direction: column; padding: 8px 12px; border: 1px solid var(--bord); border-radius: 10px; background: var(--surface-2); }
    .profil-chiffres b { font-size: 17px; } .profil-chiffres span { font-size: 11.5px; color: var(--texte-2); }
    .puces { display: flex; gap: 6px; flex-wrap: wrap; }
    .puce { font-size: 12px; padding: 3px 10px; border-radius: 999px; background: var(--primaire-pale); color: var(--primaire-fonce); }
    .recos { padding: 14px 16px; margin-bottom: 14px; display: flex; flex-direction: column; gap: 10px; }
    .recos-tete { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .recos .aide { margin-top: 4px; }
    .commentaire { margin: 0; font-size: 13px; }
    table.petite { min-width: 640px; }
    td.change { background: var(--primaire-pale); }
    .compte.rouge { background: var(--critique-pale); color: var(--critique); border-color: var(--critique); }
    .resume-alertes { padding: 12px 16px; margin-bottom: 12px; font-size: 13px; }
    .alerte-carte { padding: 12px 16px; margin-bottom: 10px; border-left: 4px solid var(--info); display: flex; flex-direction: column; gap: 4px; }
    .alerte-carte.attention { border-left-color: var(--majeure); }
    .alerte-carte.critique { border-left-color: var(--critique); }
    .alerte-tete { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .alerte-carte p { margin: 0; }
    @media (max-width: 900px) { .prompts { grid-template-columns: 1fr; } .page { padding: 16px; } .aa-saisie { flex-direction: column; } }
  `],
})
export class AdminComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly notif = inject(NotifService);

  readonly onglet = signal<Onglet>('utilisateurs');
  readonly recherche = signal('');
  readonly filtre = signal<Filtre>('tous');
  readonly utilisateurs = signal<Utilisateur[]>([]);
  readonly prompts = signal<PromptSysteme[]>([]);
  readonly promptVu = signal<PromptSysteme | null>(null);
  readonly systeme = signal<SystemeInfo | null>(null);
  readonly edition = signal(false);
  readonly formulaire = signal<{ mode: 'creation' | 'edition'; id?: number; role?: 'DEVELOPPEUR' | 'ADMIN' } | null>(null);
  readonly envoi = signal(false);
  readonly partages = signal<PromptUtilisateur[]>([]);
  readonly partageEdition = signal<{ id?: number } | null>(null);
  partageTitre = '';
  partageContenu = '';
  readonly motDePasseTemporaire = signal<{ nom: string; valeur: string } | null>(null);

  readonly titreOnglet = computed(() => ({ utilisateurs: 'Utilisateurs', quotas: 'Quotas', prompt: 'Prompt système', partages: 'Prompts partagés', alertes: 'Alertes' })[this.onglet()]);
  readonly nbCritiques = computed(() => (this.alertes()?.alertes ?? []).filter((a) => a.niveau === 'critique').length);
  readonly developpeurs = computed(() => this.utilisateurs().filter((u) => u.role === 'DEVELOPPEUR'));
  readonly visibles = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const f = this.filtre();
    return this.utilisateurs().filter(
      (u) =>
        (f === 'tous' || (f === 'bloque' ? u.bloque : u.role === f)) &&
        (!q || u.nom.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)),
    );
  });
  readonly stats = computed(() => {
    const us = this.utilisateurs();
    const devs = us.filter((u) => u.role === 'DEVELOPPEUR');
    return {
      total: us.length,
      developpeurs: devs.length,
      admins: us.length - devs.length,
      bloques: us.filter((u) => u.bloque).length,
      revuesJour: devs.reduce((n, u) => n + (u.revuesDuJour ?? 0), 0),
      assistantJour: devs.reduce((n, u) => n + (u.assistantDuJour ?? 0), 0),
    };
  });

  defauts = { quotaRevuesDefaut: 50, quotaAssistantDefaut: 100, appliquerATous: false };
  saisie = { nom: '', email: '', motDePasse: '', role: 'DEVELOPPEUR' as 'DEVELOPPEUR' | 'ADMIN', quota: null as number | null, quotaAssistant: null as number | null };
  quotas: Record<number, number | null> = {};
  quotasAssistant: Record<number, number | null> = {};
  nouvelleVersion = '';
  nouveauContenu = '';

  // ---- IA d'administration
  readonly commande = signal('');
  readonly plan = signal<PlanAdmin | null>(null);
  readonly selection = signal<Set<number>>(new Set());
  readonly resultats = signal<ResultatAction[] | null>(null);
  readonly interpretation = signal(false);
  readonly execution = signal(false);
  readonly profil = signal<ProfilCompetences | null>(null);
  readonly profilEnCours = signal(false);
  readonly recos = signal<RecommandationsQuotas | null>(null);
  readonly recosEnCours = signal(false);
  readonly alertes = signal<Alertes | null>(null);
  readonly nbAChanger = computed(() => (this.recos()?.recommandations ?? []).filter((r) => r.modificationUtile).length);

  ngOnInit(): void {
    this.chargerUtilisateurs();
    this.chargerPrompts();
    this.chargerPartages();
    this.chargerAlertes();
    this.api.parametresQuotas().subscribe((p) => (this.defauts = { ...this.defauts, ...p }));
    this.api.systeme().subscribe((s) => this.systeme.set(s));
  }

  initiales(nom: string): string {
    return (
      nom
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((m) => m[0])
        .join('')
        .toUpperCase() || '?'
    );
  }

  pct(utilise: number | null, quota: number | null): number {
    if (!quota) {
      return utilise ? 100 : 0;
    }
    return Math.min(100, Math.round(((utilise ?? 0) / quota) * 100));
  }

  /** Referme le menu « ⋯ » après le choix d'une action. */
  fermer(e: Event): void {
    (e.target as HTMLElement).closest('details')?.removeAttribute('open');
  }

  private chargerUtilisateurs(): void {
    this.api.utilisateurs().subscribe({
      next: (us) => {
        this.quotas = Object.fromEntries(us.map((u) => [u.id, u.quotaJournalier]));
        this.quotasAssistant = Object.fromEntries(us.map((u) => [u.id, u.quotaAssistant]));
        this.utilisateurs.set(us);
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  private chargerPrompts(): void {
    this.api.prompts().subscribe((ps) => {
      this.prompts.set(ps);
      this.promptVu.set(ps.find((p) => p.actif) ?? ps[0] ?? null);
    });
  }

  private chargerPartages(): void {
    this.api.promptsPartages().subscribe({
      next: (ps) => this.partages.set(ps),
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  nouveauPartage(): void {
    this.partageTitre = '';
    this.partageContenu = '';
    this.partageEdition.set({});
  }

  modifierPartage(p: PromptUtilisateur): void {
    this.partageTitre = p.titre;
    this.partageContenu = p.contenu;
    this.partageEdition.set({ id: p.id });
  }

  enregistrerPartage(): void {
    const e = this.partageEdition();
    if (!e) {
      return;
    }
    const corps = { titre: this.partageTitre.trim(), contenu: this.partageContenu.trim() };
    this.envoi.set(true);
    const appel = e.id ? this.api.modifierPromptPartage(e.id, corps) : this.api.creerPromptPartage(corps);
    appel.subscribe({
      next: (p) => {
        this.envoi.set(false);
        this.partageEdition.set(null);
        this.notif.succes(e.id ? `Prompt partagé « ${p.titre} » modifié.` : `Prompt « ${p.titre} » partagé avec tous les développeurs.`);
        this.chargerPartages();
      },
      error: (err) => {
        this.envoi.set(false);
        this.notif.erreur(messageErreur(err));
      },
    });
  }

  supprimerPartage(p: PromptUtilisateur): void {
    if (!confirm(`Retirer le prompt partagé « ${p.titre} » pour tous les développeurs ?`)) {
      return;
    }
    this.api.supprimerPromptPartage(p.id).subscribe({
      next: () => {
        this.notif.succes('Prompt partagé supprimé.');
        this.chargerPartages();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  libelleTendance(t: string): string {
    return ({ progression: 'en progression', baisse: 'en baisse', stable: 'stable', insuffisant: 'trop peu de revues' } as Record<string, string>)[t] ?? t;
  }

  chargerAlertes(avecResume = false): void {
    this.api.alertes(avecResume).subscribe({
      next: (a) => this.alertes.set(a),
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  voirProfil(u: Utilisateur): void {
    this.profil.set(null);
    this.profilEnCours.set(true);
    this.api.profilCompetences(u.id).subscribe({
      next: (p) => {
        this.profilEnCours.set(false);
        this.profil.set(p);
      },
      error: (e) => {
        this.profilEnCours.set(false);
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  analyserQuotas(): void {
    this.recosEnCours.set(true);
    this.api.recommandationsQuotas().subscribe({
      next: (r) => {
        this.recosEnCours.set(false);
        this.recos.set(r);
      },
      error: (e) => {
        this.recosEnCours.set(false);
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  appliquerReco(r: RecommandationQuota): void {
    this.api.definirQuota(r.utilisateurId, r.recommandeRevues, r.recommandeAssistant).subscribe({
      next: (n) => {
        this.remplacer(n);
        this.notif.succes(`Quotas de ${n.nom} mis à jour : ${r.recommandeRevues} revues, ${r.recommandeAssistant} requêtes assistant.`);
        this.analyserQuotas();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  toutAppliquer(): void {
    const a = (this.recos()?.recommandations ?? []).filter((r) => r.modificationUtile);
    if (!a.length || !confirm(`Appliquer les quotas conseillés à ${a.length} compte(s) ?`)) {
      return;
    }
    forkJoin(a.map((r) => this.api.definirQuota(r.utilisateurId, r.recommandeRevues, r.recommandeAssistant))).subscribe({
      next: () => {
        this.notif.succes(`${a.length} compte(s) mis à jour.`);
        this.chargerUtilisateurs();
        this.analyserQuotas();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  interpreter(): void {
    const c = this.commande().trim();
    if (!c) {
      return;
    }
    this.interpretation.set(true);
    this.resultats.set(null);
    this.api.interpreterCommande(c).subscribe({
      next: (p) => {
        this.interpretation.set(false);
        this.plan.set(p);
        this.selection.set(new Set(p.actions.map((_, i) => i)));
      },
      error: (e) => {
        this.interpretation.set(false);
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  basculer(i: number): void {
    this.selection.update((s) => {
      const n = new Set(s);
      n.has(i) ? n.delete(i) : n.add(i);
      return n;
    });
  }

  annulerPlan(): void {
    this.plan.set(null);
    this.selection.set(new Set());
  }

  executerPlan(): void {
    const p = this.plan();
    if (!p) {
      return;
    }
    const choisies: ActionPlan[] = p.actions.filter((_, i) => this.selection().has(i));
    if (!choisies.length) {
      return;
    }
    this.execution.set(true);
    this.api
      .executerActions(choisies.map((a) => ({ type: a.type, utilisateurId: a.utilisateurId, role: a.role, quota: a.quota, quotaAssistant: a.quotaAssistant })))
      .subscribe({
        next: (r) => {
          this.execution.set(false);
          this.resultats.set(r.resultats);
          this.annulerPlan();
          this.commande.set('');
          this.chargerUtilisateurs();
          this.chargerAlertes();
        },
        error: (e) => {
          this.execution.set(false);
          this.notif.erreur(messageErreur(e));
        },
      });
  }

  ouvrirCreation(): void {
    this.saisie = { nom: '', email: '', motDePasse: '', role: 'DEVELOPPEUR', quota: this.defauts.quotaRevuesDefaut, quotaAssistant: this.defauts.quotaAssistantDefaut };
    this.formulaire.set({ mode: 'creation' });
  }

  ouvrirEdition(u: Utilisateur): void {
    this.saisie = { nom: u.nom, email: u.email, motDePasse: '', role: u.role, quota: u.quotaJournalier, quotaAssistant: u.quotaAssistant };
    this.formulaire.set({ mode: 'edition', id: u.id, role: u.role });
  }

  enregistrer(): void {
    const f = this.formulaire();
    if (!f) {
      return;
    }
    const s = this.saisie;
    const nombre = (v: number | null) => (v === null || (v as unknown) === '' ? null : Number(v));
    const quota = nombre(s.quota);
    const quotaAssistant = nombre(s.quotaAssistant);
    this.envoi.set(true);
    const appel =
      f.mode === 'creation'
        ? this.api.creerUtilisateur({ nom: s.nom.trim(), email: s.email.trim(), motDePasse: s.motDePasse, role: s.role, quota: s.role === 'DEVELOPPEUR' ? quota : null, quotaAssistant: s.role === 'DEVELOPPEUR' ? quotaAssistant : null })
        : this.api.modifierUtilisateur(f.id!, { nom: s.nom.trim(), email: s.email.trim(), motDePasse: s.motDePasse || null, quota: f.role === 'DEVELOPPEUR' ? quota : null, quotaAssistant: f.role === 'DEVELOPPEUR' ? quotaAssistant : null });
    appel.subscribe({
      next: (n) => {
        this.envoi.set(false);
        this.formulaire.set(null);
        this.notif.succes(f.mode === 'creation' ? `Compte de ${n.nom} créé.` : `Compte de ${n.nom} modifié.`);
        this.chargerUtilisateurs();
      },
      error: (e) => {
        this.envoi.set(false);
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  supprimer(u: Utilisateur): void {
    if (!confirm(`Supprimer définitivement le compte de ${u.nom} et ses ${u.nbRevues ?? 0} revue(s) ? Cette action est irréversible.`)) {
      return;
    }
    this.api.supprimerUtilisateur(u.id).subscribe({
      next: () => {
        this.notif.succes(`Compte de ${u.nom} supprimé.`);
        this.chargerUtilisateurs();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  definirQuota(u: Utilisateur): void {
    const q = Number(this.quotas[u.id]);
    const a = this.quotasAssistant[u.id];
    this.api.definirQuota(u.id, q, a === null || (a as unknown) === '' ? null : Number(a)).subscribe({
      next: (n) => {
        this.remplacer(n);
        this.notif.succes(`Quotas de ${n.nom} : ${n.quotaJournalier} revues et ${n.quotaAssistant} requêtes assistant / jour.`);
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  bloquer(u: Utilisateur): void {
    if (!u.bloque && !confirm(`Bloquer le compte de ${u.nom} ? Il sera déconnecté immédiatement.`)) {
      return;
    }
    this.api.bloquer(u.id, !u.bloque).subscribe({
      next: (n) => {
        this.remplacer(n);
        this.notif.succes(n.bloque ? 'Compte bloqué.' : 'Compte débloqué.');
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  nouveau(): void {
    const actif = this.prompts().find((p) => p.actif);
    this.nouvelleVersion = `v${this.prompts().length + 1}`;
    this.nouveauContenu = actif?.contenu ?? '';
    this.edition.set(true);
  }

  publier(): void {
    this.api.publierPrompt(this.nouvelleVersion.trim(), this.nouveauContenu).subscribe({
      next: () => {
        this.edition.set(false);
        this.notif.succes('Nouvelle version du prompt publiée et activée.');
        this.chargerPrompts();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  activer(p: PromptSysteme): void {
    this.api.activerPrompt(p.id).subscribe({
      next: () => {
        this.notif.succes(`Prompt ${p.version} activé.`);
        this.chargerPrompts();
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  changerRole(u: Utilisateur, liste: HTMLSelectElement): void {
    const role = liste.value as 'DEVELOPPEUR' | 'ADMIN';
    const libelle = role === 'ADMIN' ? 'Administrateur' : 'Développeur';
    if (!confirm(`Passer le compte de ${u.nom} au rôle « ${libelle} » ?`)) {
      liste.value = u.role;
      return;
    }
    this.api.changerRole(u.id, role).subscribe({
      next: (n) => {
        this.remplacer(n);
        this.notif.succes(`${n.nom} est maintenant ${libelle.toLowerCase()}.`);
      },
      error: (e) => {
        liste.value = u.role;
        this.notif.erreur(messageErreur(e));
      },
    });
  }

  reinitialiser(u: Utilisateur): void {
    if (!confirm(`Réinitialiser le mot de passe de ${u.nom} ? L'ancien mot de passe ne fonctionnera plus.`)) {
      return;
    }
    this.api.reinitialiserMotDePasse(u.id).subscribe({
      next: (r) => {
        this.onglet.set('utilisateurs');
        this.motDePasseTemporaire.set({ nom: u.nom, valeur: r.motDePasse });
      },
      error: (e) => this.notif.erreur(messageErreur(e)),
    });
  }

  copierTemporaire(valeur: string): void {
    navigator.clipboard?.writeText(valeur).then(
      () => this.notif.succes('Mot de passe copié.'),
      () => this.notif.erreur('Copie impossible : sélectionnez le mot de passe à la main.'),
    );
  }

  enregistrerDefauts(): void {
    const d = this.defauts;
    this.api
      .enregistrerParametresQuotas({
        quotaRevuesDefaut: Number(d.quotaRevuesDefaut),
        quotaAssistantDefaut: Number(d.quotaAssistantDefaut),
        appliquerATous: d.appliquerATous,
      })
      .subscribe({
        next: () => {
          this.notif.succes(d.appliquerATous ? 'Quotas par défaut enregistrés et appliqués à tous les développeurs.' : 'Quotas par défaut enregistrés.');
          this.defauts = { ...this.defauts, appliquerATous: false };
          this.chargerUtilisateurs();
        },
        error: (e) => this.notif.erreur(messageErreur(e)),
      });
  }

  private remplacer(n: Utilisateur): void {
    this.utilisateurs.update((us) => us.map((u) => (u.id === n.id ? n : u)));
    this.quotas[n.id] = n.quotaJournalier;
    this.quotasAssistant[n.id] = n.quotaAssistant;
  }
}
