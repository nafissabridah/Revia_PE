/** Types échangés avec l'API Spring Boot (miroir des DTO Java). */

export type Gravite = 'critique' | 'majeure' | 'mineure' | 'info';
export type Categorie = 'securite' | 'bug' | 'bonne_pratique' | 'performance';
export type Statut = 'a_corriger' | 'a_revoir' | 'corrigee';
export type Mode = 'complet' | 'securite' | 'qualite';

export interface Utilisateur {
  id: number;
  nom: string;
  email: string;
  role: 'DEVELOPPEUR' | 'ADMIN';
  quotaJournalier: number | null;
  revuesDuJour: number | null;
  quotaAssistant: number | null;
  assistantDuJour: number | null;
  bloque: boolean;
  creeLe: string;
  nbRevues: number | null;
  derniereConnexion: string | null;
}

export interface AuthResponse {
  token: string;
  expireDansSecondes: number;
  utilisateur: Utilisateur;
}

export interface Correction {
  codeAvant: string;
  codeApres: string;
  justification: string;
  copiee: boolean;
}

export interface Probleme {
  id: number;
  ligne: number;
  categorie: Categorie;
  gravite: Gravite;
  titre: string;
  explication: string;
  owasp: string | null;
  fauxPositif: boolean;
  corrige: boolean;
  utile: boolean | null;
  correction: Correction | null;
  occurrencesPrecedentes: number;
}

export interface RevueDetail {
  id: number;
  titre: string;
  langage: string;
  code: string;
  score: number;
  resume: string;
  mode: Mode;
  creeLe: string;
  dureeMs: number;
  fournisseur: string;
  modele: string;
  promptVersion: string;
  nbLignes: number;
  nbMasquages: number;
  problemesRejetes: number;
  statut: Statut;
  parGravite: Record<Gravite, number>;
  parCategorie: Record<Categorie, number>;
  problemes: Probleme[];
}

export interface RevueResume {
  id: number;
  titre: string;
  langage: string;
  creeLe: string;
  score: number;
  nbProblemes: number;
  nbCritiques: number;
  statut: Statut;
}

export interface Page<T> {
  contenu: T[];
  page: number;
  taille: number;
  total: number;
  pages: number;
}

export interface FiltresHistorique {
  q?: string;
  langage?: string;
  scoreMin?: number | null;
  scoreMax?: number | null;
  statut?: string;
  jours?: number | null;
  tri?: string;
}

export interface TableauDeBord {
  debut: string;
  fin: string;
  scoreMoyen: number;
  nbRevues: number;
  nbRevuesSemaine: number;
  nbProblemes: number;
  nbCritiques: number;
  nbCorriges: number;
  progression: number;
  erreursFrequentes: string[];
  erreursDetaillees: { titre: string; categorie: Categorie; occurrences: number; corrigees: number }[];
  parCategorie: Record<Categorie, number>;
  evolution: { jour: string; scoreMoyenJour: number; moyenneGlissante: number; nbRevues: number }[];
}

export interface SystemeInfo {
  application: string;
  version: string;
  fournisseur: string;
  modele: string;
  disponible: boolean;
  temperature: number;
  timeoutSec: number;
  quotaParDefaut: number;
  quotaAssistantParDefaut: number;
  analysesParMinute: number;
  connexionsParMinute: number;
  contexteMaxTokens: number;
  maxLignes: number;
}

export interface PromptSysteme {
  id: number;
  version: string;
  contenu: string;
  actif: boolean;
  creeLe: string;
}

export type OutilIA = 'tests' | 'documentation' | 'explication' | 'commit' | 'refactoring' | 'conversion';

export interface MessageChat {
  role: 'utilisateur' | 'assistant';
  contenu: string;
}

export interface AssistantReponse {
  contenu: string;
  outil: OutilIA | 'chat';
  langage: string | null;
  nbMasquages: number;
  dureeMs: number;
  fournisseur: string;
  modele: string;
}

export interface ErreurApi {
  code: string;
  message: string;
  status: number;
  champs?: Record<string, string>;
}

export interface ParametresQuotas {
  quotaRevuesDefaut: number;
  quotaAssistantDefaut: number;
}

export interface PromptUtilisateur {
  id: number;
  titre: string;
  contenu: string;
  creeLe: string;
  modifieLe: string;
  partage: boolean;
  modifiable: boolean;
}

export interface Recurrent {
  titre: string;
  occurrences: number;
}

export interface ProfilCompetences {
  utilisateurId: number;
  nom: string;
  nbRevues: number;
  scoreMoyen: number;
  scoreInitial: number;
  scoreRecent: number;
  tendance: 'progression' | 'baisse' | 'stable' | 'insuffisant';
  recurrents: Recurrent[];
  parCategorie: Record<string, number>;
  parGravite: Record<string, number>;
  nbMasquages: number;
  synthese: string;
  source: 'ia' | 'regles';
}

export interface RecommandationQuota {
  utilisateurId: number;
  nom: string;
  quotaRevues: number;
  quotaAssistant: number;
  recommandeRevues: number;
  recommandeAssistant: number;
  moyenneRevuesJour: number;
  picRevues: number;
  moyenneAssistantJour: number;
  picAssistant: number;
  joursActifs: number;
  explication: string;
  modificationUtile: boolean;
}

export interface RecommandationsQuotas {
  recommandations: RecommandationQuota[];
  commentaire: string;
  source: 'ia' | 'regles';
}

export interface Alerte {
  niveau: 'critique' | 'attention' | 'info';
  type: string;
  utilisateurId: number | null;
  nom: string;
  message: string;
  detail: string;
  date: string;
}

export interface Alertes {
  alertes: Alerte[];
  resume: string;
  source: 'ia' | 'regles';
}

export interface ActionPlan {
  type: 'BLOQUER' | 'DEBLOQUER' | 'CHANGER_ROLE' | 'DEFINIR_QUOTA';
  utilisateurId: number;
  nom: string;
  libelle: string;
  role: 'DEVELOPPEUR' | 'ADMIN' | null;
  quota: number | null;
  quotaAssistant: number | null;
}

export interface PlanAdmin {
  interpretation: string;
  source: 'ia' | 'regles';
  actions: ActionPlan[];
  avertissements: string[];
}

export interface ResultatAction {
  type: string;
  utilisateurId: number;
  nom: string;
  ok: boolean;
  message: string;
}
