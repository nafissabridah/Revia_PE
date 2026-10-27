import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssistantReponse, FiltresHistorique, MessageChat, OutilIA, Page, Probleme, PromptSysteme, RevueDetail, RevueResume, SystemeInfo,
  ActionPlan, Alertes, ParametresQuotas, PlanAdmin, ProfilCompetences, PromptUtilisateur, RecommandationsQuotas, ResultatAction, TableauDeBord, Utilisateur,
} from './models';

/** Accès à l'API REST. Angular ne parle qu'à notre back-end Spring : la clé d'API ne quitte jamais le serveur. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  soumettre(req: { titre?: string; langage: string; code: string; mode: string }): Observable<RevueDetail> {
    return this.http.post<RevueDetail>('/api/revues', req);
  }

  revue(id: number): Observable<RevueDetail> {
    return this.http.get<RevueDetail>(`/api/revues/${id}`);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`/api/revues/${id}`);
  }

  historique(f: FiltresHistorique, page: number, taille: number): Observable<Page<RevueResume>> {
    return this.http.get<Page<RevueResume>>('/api/revues', { params: this.params(f).set('page', page).set('taille', taille) });
  }

  exporterCsv(f: FiltresHistorique): Observable<Blob> {
    return this.http.get('/api/revues/export.csv', { params: this.params(f), responseType: 'blob' });
  }

  fauxPositif(id: number, valeur: boolean): Observable<Probleme> {
    return this.http.post<Probleme>(`/api/problemes/${id}/faux-positif`, { valeur });
  }

  corrige(id: number, valeur: boolean): Observable<Probleme> {
    return this.http.post<Probleme>(`/api/problemes/${id}/corrige`, { valeur });
  }

  utile(id: number, valeur: boolean): Observable<Probleme> {
    return this.http.post<Probleme>(`/api/problemes/${id}/utile`, { valeur });
  }

  copier(id: number): Observable<{ code: string }> {
    return this.http.post<{ code: string }>(`/api/problemes/${id}/correction/copie`, {});
  }

  tableauDeBord(jours: number): Observable<TableauDeBord> {
    return this.http.get<TableauDeBord>('/api/tableau-de-bord', { params: { jours } });
  }

  modifierProfil(nom: string): Observable<Utilisateur> {
    return this.http.put<Utilisateur>('/api/me', { nom });
  }

  changerMotDePasse(ancien: string, nouveau: string): Observable<void> {
    return this.http.put<void>('/api/me/mot-de-passe', { ancien, nouveau });
  }

  // ---------- assistant IA et outils IA
  discuter(messages: MessageChat[], code?: string, langage?: string): Observable<AssistantReponse> {
    return this.http.post<AssistantReponse>('/api/assistant/chat', { messages, code: code || null, langage: langage || null });
  }

  outil(outil: OutilIA, req: { code: string; langage: string; cible?: string }): Observable<AssistantReponse> {
    return this.http.post<AssistantReponse>(`/api/assistant/outils/${outil}`, req);
  }

  systeme(): Observable<SystemeInfo> {
    return this.http.get<SystemeInfo>('/api/systeme/info');
  }

  // ---------- administration
  utilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>('/api/admin/utilisateurs');
  }

  /** Résume la conversation pour libérer du contexte (« compactage »). */
  compacter(messages: MessageChat[]): Observable<AssistantReponse> {
    return this.http.post<AssistantReponse>('/api/assistant/compacter', { messages });
  }

  // ---------- administration assistée par IA
  profilCompetences(id: number): Observable<ProfilCompetences> {
    return this.http.get<ProfilCompetences>(`/api/admin/utilisateurs/${id}/profil`);
  }

  recommandationsQuotas(): Observable<RecommandationsQuotas> {
    return this.http.get<RecommandationsQuotas>('/api/admin/recommandations-quotas');
  }

  alertes(avecResume = true): Observable<Alertes> {
    return this.http.get<Alertes>('/api/admin/alertes', { params: { resume: avecResume } });
  }

  interpreterCommande(commande: string): Observable<PlanAdmin> {
    return this.http.post<PlanAdmin>('/api/admin/assistant', { commande });
  }

  executerActions(actions: Pick<ActionPlan, 'type' | 'utilisateurId' | 'role' | 'quota' | 'quotaAssistant'>[]): Observable<{ resultats: ResultatAction[] }> {
    return this.http.post<{ resultats: ResultatAction[] }>('/api/admin/assistant/executer', { actions });
  }

  // ---------- prompts partagés (administrateur)
  promptsPartages(): Observable<PromptUtilisateur[]> {
    return this.http.get<PromptUtilisateur[]>('/api/admin/prompts-partages');
  }

  creerPromptPartage(p: { titre: string; contenu: string }): Observable<PromptUtilisateur> {
    return this.http.post<PromptUtilisateur>('/api/admin/prompts-partages', p);
  }

  modifierPromptPartage(id: number, p: { titre: string; contenu: string }): Observable<PromptUtilisateur> {
    return this.http.put<PromptUtilisateur>(`/api/admin/prompts-partages/${id}`, p);
  }

  supprimerPromptPartage(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/prompts-partages/${id}`);
  }

  // ---------- mes prompts (bibliothèque personnelle)
  mesPrompts(): Observable<PromptUtilisateur[]> {
    return this.http.get<PromptUtilisateur[]>('/api/mes-prompts');
  }

  creerMonPrompt(p: { titre: string; contenu: string }): Observable<PromptUtilisateur> {
    return this.http.post<PromptUtilisateur>('/api/mes-prompts', p);
  }

  modifierMonPrompt(id: number, p: { titre: string; contenu: string }): Observable<PromptUtilisateur> {
    return this.http.put<PromptUtilisateur>(`/api/mes-prompts/${id}`, p);
  }

  supprimerMonPrompt(id: number): Observable<void> {
    return this.http.delete<void>(`/api/mes-prompts/${id}`);
  }

  creerUtilisateur(u: { nom: string; email: string; motDePasse: string; role: 'DEVELOPPEUR' | 'ADMIN'; quota: number | null; quotaAssistant: number | null }): Observable<Utilisateur> {
    return this.http.post<Utilisateur>('/api/admin/utilisateurs', u);
  }

  modifierUtilisateur(id: number, u: { nom: string; email: string; motDePasse: string | null; quota: number | null; quotaAssistant: number | null }): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`/api/admin/utilisateurs/${id}`, u);
  }

  supprimerUtilisateur(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/utilisateurs/${id}`);
  }

  definirQuota(id: number, quota: number, quotaAssistant: number | null): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`/api/admin/utilisateurs/${id}/quota`, { quota, quotaAssistant });
  }

  changerRole(id: number, role: 'DEVELOPPEUR' | 'ADMIN'): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`/api/admin/utilisateurs/${id}/role`, { role });
  }

  reinitialiserMotDePasse(id: number): Observable<{ motDePasse: string }> {
    return this.http.post<{ motDePasse: string }>(`/api/admin/utilisateurs/${id}/reinitialisation-mot-de-passe`, {});
  }

  parametresQuotas(): Observable<ParametresQuotas> {
    return this.http.get<ParametresQuotas>('/api/admin/parametres');
  }

  enregistrerParametresQuotas(p: ParametresQuotas & { appliquerATous: boolean }): Observable<ParametresQuotas> {
    return this.http.put<ParametresQuotas>('/api/admin/parametres/quotas', p);
  }

  bloquer(id: number, valeur: boolean): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`/api/admin/utilisateurs/${id}/blocage`, { valeur });
  }

  prompts(): Observable<PromptSysteme[]> {
    return this.http.get<PromptSysteme[]>('/api/admin/prompts');
  }

  publierPrompt(version: string, contenu: string): Observable<PromptSysteme> {
    return this.http.post<PromptSysteme>('/api/admin/prompts', { version, contenu });
  }

  activerPrompt(id: number): Observable<PromptSysteme> {
    return this.http.put<PromptSysteme>(`/api/admin/prompts/${id}/activer`, {});
  }

  private params(f: FiltresHistorique): HttpParams {
    let p = new HttpParams();
    for (const [k, v] of Object.entries(f)) {
      if (v !== undefined && v !== null && v !== '') {
        p = p.set(k, String(v));
      }
    }
    return p;
  }
}
