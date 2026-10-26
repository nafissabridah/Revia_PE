# PROMPT AGENT IA — RevIA (Revia_PE) — Document dédié ultra-détaillé

> **Fichier autonome** destiné à être donné en entier (ou par sections) à un agent IA (Cursor, Claude, Copilot, etc.) pour **construire, réparer, tester, documenter et déployer** le projet RevIA jusqu’à l’état « prêt » (démo, soutenance, production).
>
> **Ne pas modifier** ce fichier pour du code applicatif : il décrit *comment* travailler sur le repo.
>
> **Sources officielles du projet** (à croiser en cas de doute) :
> - `C:\Users\Mustapha.Sifaoui\Downloads\Revia_PE\RESUME.md` — synthèse équipe, stack, tests, conformité, lancement
> - `C:\Users\Mustapha.Sifaoui\Downloads\Revia_PE\README.md` — manuel technique complet (16 sections)
> - `C:\Users\Mustapha.Sifaoui\Downloads\Revia_PE\docs\rapport-jeu-essai-gemini.md` — benchmark LLM réel
> - `C:\Users\Mustapha.Sifaoui\Downloads\Revia_PE\docs\captures\` — captures UI de référence
> - `C:\Users\Mustapha.Sifaoui\Downloads\Revia_PE\docs\historique-prompts.md` — historique réel des 44 prompts de construction (intégré et annoté en **§29**, le fichier source reste la référence verbatim)

**Règle de priorité en cas de conflit :** tests automatisés (`backend/src/test`, `frontend/**/*.spec.ts`) > `README.md` > `RESUME.md` > ce prompt.

**Dépôt :** `Revia_PE` — remote typique `github-education:nafissabridah/Revia_PE.git`, branche `main`.

---

## Table des matières

1. [Bloc prompt système (copier-coller)](#1-bloc-prompt-système-copier-coller)
2. [Mission, périmètre, interdictions](#2-mission-périmètre-interdictions)
3. [Contexte produit (RESUME.md intégré)](#3-contexte-produit-resumemd-intégré)
4. [Stack et versions exactes](#4-stack-et-versions-exactes)
5. [Conformité présentation et fiche AI App Studio](#5-conformité-présentation-et-fiche-ai-app-studio)
6. [Architecture technique détaillée](#6-architecture-technique-détaillée)
7. [Diagramme de classes → fichiers Java](#7-diagramme-de-classes--fichiers-java)
8. [Module IA — revue de code (5 étapes)](#8-module-ia--revue-de-code-5-étapes)
9. [Prompt système LLM (`revue-system.st`) — texte complet](#9-prompt-système-llm-revue-systemst--texte-complet)
10. [Module IA — Assistant chat et outils (extension)](#10-module-ia--assistant-chat-et-outils-extension)
11. [Sécurité, quotas, messages utilisateur](#11-sécurité-quotas-messages-utilisateur)
12. [API REST complète](#12-api-rest-complète)
13. [Front-end Angular — routes, gardes, intégration API](#13-front-end-angular--routes-gardes-intégration-api)
14. [Configuration — variables, profils, application.yml](#14-configuration--variables-profils-applicationyml)
15. [Scripts PowerShell — comportement ligne par ligne](#15-scripts-powershell--comportement-ligne-par-ligne)
16. [Build, exécution, modes dev / JAR / Docker](#16-build-exécution-modes-dev--jar--docker)
17. [Déploiement Render, Vercel, CI GitHub Actions](#17-déploiement-render-vercel-ci-github-actions)
18. [Jeu d'essai IA — manifeste et objectifs](#18-jeu-dessai-ia--manifeste-et-objectifs)
19. [Rapport Gemini — synthèse et leçons](#19-rapport-gemini--synthèse-et-leçons)
20. [Tests automatisés — inventaire détaillé](#20-tests-automatisés--inventaire-détaillé)
21. [Parcours de test manuel (15 étapes)](#21-parcours-de-test-manuel-15-étapes)
22. [UI / UX — maquettes, couleurs, captures](#22-ui--ux--maquettes-couleurs-captures)
23. [Arborescence complète du dépôt](#23-arborescence-complète-du-dépôt)
24. [Dépannage (README §16)](#24-dépannage-readme-16)
25. [Checklist « projet prêt » — 120 points](#25-checklist-projet-prêt--120-points)
26. [Plan de travail agent — 12 phases](#26-plan-de-travail-agent--12-phases)
27. [Conventions de code pour l’agent](#27-conventions-de-code-pour-lagent)
28. [Écarts assumés et limites connues](#28-écarts-assumés-et-limites-connues)
29. [Historique réel des prompts de construction (44 prompts, 10 phases)](#29-historique-réel-des-prompts-de-construction-44-prompts-10-phases)
30. [Matrice prompt → code → test](#30-matrice-prompt--code--test)
31. [Leçons de prompting et écarts historique / code](#31-leçons-de-prompting-et-écarts-historique--code)

---

## 1. Bloc prompt système (copier-coller)

```text
Tu es un ingénieur full-stack senior et responsable qualité sur RevIA (Revia_PE).

CONTEXTE
RevIA est une application web de revue de code assistée par IA : Angular 17 + Spring Boot 3 + PostgreSQL (prod) / H2 (dev). L'utilisateur colle du code ; le serveur analyse via Spring AI (Google Gemini gratuit par défaut) ou un moteur « demo » à règles. Rapport : score /100, problèmes par gravité, corrections avant/après, OWASP.

DOCUMENTATION OBLIGATOIRE AVANT ACTION
1) Lire RESUME.md à la racine du repo.
2) Pour install, API, config, déploiement, dépannage : README.md (sections 1 à 16).
3) Pour benchmarks LLM : docs/rapport-jeu-essai-gemini.md.
4) Pour l'UI attendue : docs/captures/01_connexion.png … 07_admin.png.
5) Pour le contrat technique : exécuter et respecter les tests backend (mvn test) et frontend (ng test).

MISSION
Rendre le projet « prêt » : build reproductible, tests verts, démo http://localhost:8085 fonctionnelle, secrets hors git, conformité sujet (présentation RevIA + fiche AI App Studio). Si demandé : Docker Compose :8080, Render API + Vercel front, CI verte.

INTERDICTIONS ABSOLUES
- Commiter .env, clés API, mots de passe réels.
- Appeler un LLM depuis le navigateur ou exposer REVIA_AI_API_KEY au client.
- Supprimer masquage (MasqueurDonnees), validation JSON, quotas (QuotaService), JWT, isolation des revues par auteur.
- Remplacer Angular/Spring par une autre stack.
- Casser le format JSON de réponse IA (ReponseIA / DTOs) sans mettre à jour front + tests.
- Afficher le score brut du LLM : le score affiché vient de Revue.calculerScore() côté serveur.
- Élargir le scope sans demande (refactoring massif, dépendances inutiles).

RÈGLES DE TRAVAIL
- Diff minimal ; nommage français domaine (Revue, Probleme, Developpeur) ; package com.revia.
- Après toute modification backend : mvn -B test (ou verify).
- Après toute modification frontend : ng test --watch=false --browsers=ChromeHeadless && ng build --configuration production.
- Textes UI et messages d'erreur en français (voir README §8).
- Si tu modifies revue-system.st : publier/activer une version prompt en admin OU réinitialiser H2 dev ; relancer JeuEssaiIATest ; documenter impact sur Gemini.

LANCEMENT RAPIDE (Windows, racine repo)
powershell -ExecutionPolicy Bypass -File scripts\start.ps1
→ http://localhost:8085
Comptes : sarra@revia.local / Sarra2026! (dev) ; admin@revia.local / Admin2026! (admin)
Gemini : fichier .env avec REVIA_AI_PROVIDER=gemini et REVIA_AI_API_KEY=… (jamais versionné)

DÉFINITION DE PRÊT
Suivre docs/PROMPT_AGENT_IA_DETAILLE.md section 25 (checklist 120 points) et section 26 (12 phases). Minimum : R1–R40 de la checklist + parcours manuel §21.

En cas d'incertitude : lire le test correspondant dans backend/src/test/java/com/revia/ avant de changer le comportement.

HISTORIQUE (reconstruction ou compréhension du « pourquoi »)
docs/PROMPT_AGENT_IA_DETAILLE.md §29 contient les 44 prompts réels qui ont construit RevIA, dans l'ordre, avec résultat observé, fichiers produits et tests associés. §30 = matrice prompt → code → test. §31 = leçons et écarts historique/code. Chaque règle du prompt système (revue-system.st, AssistantIA) est née d'un échec réel : ne pas la supprimer sans relancer JeuEssaiIATest et AssistantIATest.
```

---

## 2. Mission, périmètre, interdictions

### 2.1 Mission principale

Mettre RevIA dans un état où :

1. Un correcteur / un jury peut **cloner le repo**, lancer `scripts\start.ps1`, se connecter, lancer une **revue de code** (demo ou Gemini), voir masquage + validation + corrections.
2. **`mvn test`** et **`ng test`** passent sans échec.
3. La **CI GitHub Actions** (push sur `main`) reste verte : backend verify, frontend test + build, build Docker backend + frontend.
4. Optionnel mais souvent demandé : **Docker Compose** sur port **8080** ; **Render** (API) + **Vercel** (SPA) avec CORS aligné.

### 2.2 Périmètre fonctionnel (ce que l’app doit faire)

Reprise intégrale de `RESUME.md` :

| Capacité utilisateur | Détail |
|---------------------|--------|
| Soumission | Coller ou **importer un fichier** (.java, .py, .js, .ts, .php, .c, .c#…) |
| Rapport | Score **/100**, résumé, problèmes par **gravité** (critique, majeure, mineure, info) |
| Correction | Vue **avant/après** (diff), explication, référence **OWASP** si sécurité |
| Faux positif | Signalement → **recalcul du score** (problème exclu) |
| Historique | Recherche, filtres (langage, score, statut, période), pagination, suppression, **export CSV** |
| Tableau de bord | Indicateurs, courbe 30 jours, barres par catégorie, top 3 problèmes récurrents |
| Paramètres | Profil, mot de passe, infos module IA (fournisseur, modèle, température 0,2, timeout 30 s) |
| Admin | Utilisateurs, **quotas journaliers**, blocage compte (effet immédiat), **versions du prompt système** (créer / activer) |
| Modes d’analyse | Sécurité + qualité / Sécurité seule / Qualité seule (`mode` sur POST revue) |
| Extensions UI | Routes **Assistant IA** (`/assistant`) et **Outils IA** (`/outils`) — chat + outils serveur (voir §10) |

### 2.3 Interdictions (rappel explicite)

- Aucun secret dans Git (`.env` est dans `.gitignore`).
- Pas de `include-stacktrace: always` côté API prod.
- Pas de contournement admin : `/api/admin/**` réservé rôle ADMIN.
- Un développeur **ne doit jamais** lire la revue d’un autre (404, pas 403).
- Ne pas désactiver Flyway en prod ni passer `ddl-auto` à `create-drop` en prod.

---

## 3. Contexte produit (RESUME.md intégré)

### 3.1 Phrase produit

**RevIA relit ton code comme un collègue.** Tu colles un code, l’IA trouve failles et bugs, explique le problème et propose une correction.

### 3.2 Équipe et lien code

- **Équipe :** Nafissa Bridah · Rahma Kehrji · Housine Saihi · Aziz Eifa · Mustapha Sifaoui  
- **Sujet :** Mini-projet · Sujet 4  
- **Code public (référence README) :** https://github.com/nafissabridah/RevIA  
- **Clone local courant :** `Revia_PE` (GitHub Education)

### 3.3 LLM retenu — Google Gemini

| Attribut | Valeur |
|----------|--------|
| Modèle par défaut | `gemini-flash-lite-latest` |
| Gratuit | Oui (exigence fiche atelier AI App Studio) |
| Clé | [Google AI Studio](https://aistudio.google.com) → variable `REVIA_AI_API_KEY` ou `.env` |
| Emplacement clé | **Serveur uniquement** — lue par `start.ps1`, Spring Boot, jamais Angular |
| Alternative test sans réseau | `REVIA_AI_PROVIDER=demo` (analyseur à règles, JSON identique) |
| Modèle à éviter en gratuit | `gemini-flash-latest` — **20 requêtes/jour** (quota) |
| Performance observée | ~1,5 s/analyse (rapport Gemini) |

**Autres fournisseurs** (sans changer le code métier) : `groq`, `ollama`, `openai`, `anthropic` — voir README §3.4.

### 3.4 Pipeline LLM (5 étapes) — RESUME.md

| # | Étape | Implémentation |
|---|--------|----------------|
| 1 | Découpage | Morceaux de **300 lignes**, numérotation globale conservée |
| 2 | Masquage | Mots de passe, clés, e-mails → `***MASQUÉ***` |
| 3 | Appel Gemini | Température **0,2**, délai **30 s**, prompt versionné, JSON forcé |
| 4 | Validation | Lignes existantes, gravités/catégories, Bean Validation, **1 retry** si JSON invalide |
| 5 | Restitution | Enregistrement BDD + rapport UI |

### 3.5 Résultats de tests (référence RESUME — vérifier à l’exécution)

| Campagne | Résultat documenté | Commande agent |
|----------|-------------------|----------------|
| Back-end | 42/42 dans README ; **54/54** possible après ajout Assistant (voir §20) | `cd backend && mvn test` |
| Front-end | 14/14 | `cd frontend && npx ng test --watch=false --browsers=ChromeHeadless` |
| Jeu d’essai demo | 20/20 défauts, 0/3 faux positifs sains | `JeuEssaiIATest` |
| Jeu d’essai Gemini | 19/20 (95 %), 0/3 faux positifs | `docs/rapport-jeu-essai-gemini.md` |
| CI | Verte | push + GitHub Actions |
| Parcours navigateur | 0 erreur console | README §4.1 |
| Prod PostgreSQL | Flyway + revue OK | README §11 |

**L’agent doit toujours relever le nombre exact de tests** dans la sortie Maven / Karma, pas se fier uniquement au chiffre 42 du README.

---

## 4. Stack et versions exactes

Reprise de `README.md` §2 (ne pas upgrader majeur sans accord).

### 4.1 Front-end

| Composant | Version |
|-----------|---------|
| Angular core | 17.3.12 |
| Angular CLI | 17.3.17 |
| TypeScript | 5.4.5 |
| Angular Material / CDK | 17.3.10 |
| ngx-monaco-editor-v2 | 17.0.1 (monaco 0.44.0) |
| ng2-charts / Chart.js | 6.0.1 / 4.5.1 |
| RxJS / zone.js | 7.8.2 / 0.14.10 |
| Karma / Jasmine | 6.4.4 / 5.1.2 |
| Node (CI/Docker) | 20 LTS |

### 4.2 Back-end

| Composant | Version |
|-----------|---------|
| Java (compilation) | 17 |
| Spring Boot | 3.5.16 |
| Spring Security | 6.5.11 |
| Spring AI | 1.1.8 |
| Hibernate | 6.6.53.Final |
| Jackson | 2.21.4 |
| jjwt | 0.12.6 |
| Bucket4j | 8.14.0 |
| PostgreSQL driver | 42.7.11 |
| Flyway | 11.7.2 |
| H2 (dev) | 2.3.232 |
| Maven | 3.9.9 |

**Version applicative :** RevIA **1.0.0** — `GET /api/systeme/info`.

---

## 5. Conformité présentation et fiche AI App Studio

### 5.1 Présentation RevIA (diapos) — RESUME.md

| Exigence | État attendu |
|----------|--------------|
| Angular 17, Spring Boot 3, Java 17, Spring AI, PostgreSQL | ✅ |
| Diagramme de classes complet | ✅ (voir §7) |
| 11 cas d’utilisation | ✅ (liste README §13) |
| Module IA 5 étapes | ✅ |
| Sécurité BCrypt, JWT, 50 revues/j, Bucket4j | ✅ |
| Maquettes diapos 12–14 | ✅ (couleurs §22) |
| Messages d’erreur annexe | ✅ (§11) |
| Tests ≥85 % détection, ≤1/3 faux positif avec vrai LLM | ✅ (Gemini 95 %, 0 FP) |

**Écarts assumés :** Gemini au lieu d’OpenAI/Claude payants ; score exemple 36 vs 38 maquette ; correction JSON en objet ; admin = diagramme de **classes** (ne soumet pas de code).

### 5.2 Fiche AI App Studio (ESPRIT)

| Exigence | RevIA |
|----------|-------|
| Web + API IA **gratuite** | Gemini |
| Clé serveur, `.env` non versionné | ✅ |
| Tests mockés sans vraie API en CI | ✅ |
| Secours si API down | Messages + mode demo |
| GitHub + démo | ✅ |

---

## 6. Architecture technique détaillée

### 6.1 Principes

- **Angular → Spring REST uniquement** (`/api/*`, JWT Bearer).
- **Front servi** par le JAR (`backend/src/main/resources/static/` après build) **ou** nginx (Docker) **ou** Vercel (API proxyée vers Render).
- **Stateless** : JWT 2 h ; compte bloqué rechargé à chaque requête.
- **Schéma BDD** : Flyway migrations ; Hibernate `ddl-auto: validate`.

### 6.2 Flux HTTP d’une revue

1. `POST /api/revues` + JWT  
2. `RateLimitFilter` (Bucket4j) — 10 analyses/min/utilisateur  
3. Bean Validation (code 1–2000 lignes, langage, mode)  
4. `QuotaService` — revues du jour < quota (défaut 50) **avant** LLM  
5. `AnalyseurIA.analyser` — préparation, masquage, appel LLM/demo, validation  
6. `RevueService` — transaction : Revue + Problemes + Corrections  
7. Réponse JSON rapport → Angular (Monaco, score-ring, filtres)

### 6.3 Composants back-end (couches)

| Couche | Packages / rôles |
|--------|------------------|
| `web` | Contrôleurs REST, DTOs, `SpaController` (fallback SPA) |
| `service` | Revue, Auth, Quota, Admin, TableauDeBord, Mapper |
| `ai` | AnalyseurIA, AssistantIA, MasqueurDonnees, PreparateurCode, LlmClient, AnalyseurStatique, RedacteurRevue, LlmConfig, PromptService |
| `domain` | Entités JPA |
| `repository` | Spring Data |
| `security` | SecurityConfig, JwtService, JwtAuthFilter, RateLimitFilter |
| `error` | GlobalExceptionHandler, ErrorCode, ApiException |
| `config` | ReviaProperties, DataInitializer, ExecutorConfig |

---

## 7. Diagramme de classes → fichiers Java

| Classe | Fichier | Notes agent |
|--------|---------|-------------|
| Utilisateur | `domain/Utilisateur.java` | SINGLE_TABLE, abstract |
| Developpeur | `domain/Developpeur.java` | quotaJournalier, soumet revues |
| Administrateur | `domain/Administrateur.java` | quotas, blocage, prompts — **ne soumet pas** de revues |
| AnalyseurIA | `ai/AnalyseurIA.java` | cœur revue code |
| PromptSysteme | `domain/PromptSysteme.java` | versions actives en BDD |
| Revue | `domain/Revue.java` | `calculerScore()`, export CSV |
| Probleme | `domain/Probleme.java` | fauxPositif, ligne, gravité |
| Correction | `domain/Correction.java` | codeAvant, codeApres |
| Gravite | `domain/Gravite.java` | CRITIQUE, MAJEURE, MINEURE, INFO |
| Categorie | `domain/Categorie.java` | securite, bug, bonne_pratique, performance |
| TableauDeBord | `domain/TableauDeBord.java` | agrégats stats |

**Formule score (serveur) :**  
`100 - 20×(critiques) - 6×(majeures) - 2×(mineures)`, borné [0,100], **faux positifs exclus**.  
Exemple : 2C + 3M + 3m = **36/100**.

**Contrainte données :** toutes les requêtes revues filtrent par auteur (`RevueRepository`).

---

## 8. Module IA — revue de code (5 étapes)

| Étape | Classe | Détail technique |
|-------|--------|------------------|
| 01 Préparation | `PreparateurCode` | Langage explicite ou détecté ; format `NNN \| ligne` ; chunks **300** lignes |
| 02 Masquage | `MasqueurDonnees` | Regex/heuristiques : secrets OpenAI/AWS/GitHub/Stripe/Google/Slack/JWT, JDBC URLs, emails, etc. ; **conserve le nombre de lignes** |
| 03 Appel | `SpringAiLlmClient` | temp 0.2, timeout configurable, retry 2 s sur 5xx |
| 04 Validation | `ReponseIA` + BV | Ligne must exist ; catégorie/gravité normalisées ; **json-retries: 1** |
| 05 Persistance | `RevueService` | Cascade problèmes/corrections |

### 8.1 Fournisseurs (`REVIA_AI_PROVIDER`)

| Valeur | Modèle défaut | Clé |
|--------|---------------|-----|
| gemini | gemini-flash-lite-latest | Oui |
| groq | llama-3.3-70b-versatile | Oui |
| ollama | qwen2.5-coder:1.5b | Non (local :11434) |
| openai | gpt-4o-mini | Oui |
| anthropic | claude-sonnet-5-5 | Oui |
| demo | revia-regles-statiques-1.0 | Non (~25 règles + RedacteurRevue) |

### 8.2 Prompt en base vs fichier

- Fichier seed : `backend/src/main/resources/prompts/revue-system.st`
- Admin peut créer v2, v3… et **activer** une version sans redéployer.
- Dev H2 : si prompt obsolète en BDD, publier le contenu du fichier via admin ou reset `%LOCALAPPDATA%\RevIA\data`.

---

## 9. Prompt système LLM (`revue-system.st`) — texte complet

Placeholders runtime : `{langage}`, `{mode}`.

```
Tu es un développeur senior spécialisé en revue
de code et en sécurité applicative (OWASP Top 10).
Analyse l'extrait de code {langage} ci-dessous.
Les lignes sont numérotées.
Repère les bugs, les mauvaises pratiques et
les failles de sécurité. Pour chaque problème, indique :
ligne, catégorie, gravité, explication courte, correction.
N'invente aucun problème : si le code est correct,
renvoie une liste vide.
Réponds uniquement en JSON, sans texte autour.
Rédige tous les textes en français correct, sans aucun mot d'une autre langue ni d'un autre alphabet
(pour un secret, écris « le déplacer dans une variable d'environnement » plutôt que « l'externaliser »).

Périmètre demandé : {mode}
Les valeurs ***MASQUÉ*** ont été masquées avant l'envoi : ne les commente pas comme des erreurs de syntaxe.
Mais si ***MASQUÉ*** est la valeur littérale d'un mot de passe, d'une clé, d'un jeton ou d'un secret écrit dans le code
(ex. PASSWORD = "***MASQUÉ***"), c'est un secret en dur : signale-le (catégorie securite, gravité critique,
titre « Mot de passe en dur » ou « Secret en dur dans le code », OWASP A07:2021) et propose une variable d'environnement.
Le numéro de "ligne" doit être exactement l'un des numéros affichés devant le code.

Format exact de la réponse (JSON valide, en français) :
{
  "score": <entier 0 à 100, 100 = code irréprochable>,
  "resume": "<2 ou 3 phrases : verdict et priorités>",
  "problemes": [
    {
      "ligne": <entier>,
      "categorie": "securite" | "bug" | "bonne_pratique" | "performance",
      "gravite": "critique" | "majeure" | "mineure" | "info",
      "titre": "<titre court, ex. Injection SQL>",
      "explication": "<pourquoi c'est un problème, 1 à 3 phrases>",
      "owasp": "<référence OWASP Top 10 2021 si sécurité, ex. A03:2021 – Injection, sinon null>",
      "correction": {
        "codeAvant": "<la ou les lignes fautives, recopiées>",
        "codeApres": "<le code corrigé>",
        "justification": "<pourquoi la correction règle le problème>"
      }
    }
  ]
}
```

---

## 10. Module IA — Assistant chat et outils (extension)

Fonctionnalité **au-delà** du README original — présente dans le code actuel.

### 10.1 Contrôleur

- Classe : `backend/src/main/java/com/revia/web/AssistantController.java`
- Base : `/api/assistant`
- Rate limit : mêmes règles que `POST /api/revues` pour `/api/assistant/*` (`RateLimitFilter`)

### 10.2 Endpoints

| Méthode | Route | Réponse |
|---------|-------|---------|
| POST | `/api/assistant/chat` | JSON `AssistantReponse` |
| POST | `/api/assistant/chat/flux` | SSE (`text/event-stream`) — événements `morceau`, `fin`, `erreur` |
| POST | `/api/assistant/outils/{outil}` | JSON |
| POST | `/api/assistant/outils/{outil}/flux` | SSE |

**Auth :** JWT obligatoire (401 sans token).

### 10.3 Outils (`AssistantIA.Outil`)

| ID route `{outil}` | Libellé |
|--------------------|---------|
| `tests` | Génération de tests |
| `documentation` | Documentation automatique |
| `explication` | Explication de code |
| `commit` | Message de commit + PR |
| `refactoring` | Refactoring |
| `conversion` | Conversion de langage (paramètre `cible`) |

Outil inconnu → erreur métier (ex. `magie` → 404/introuvable en test).

### 10.4 Front-end

- Routes : `/assistant`, `/outils` (`app.routes.ts`, `developpeurGuard`)
- Services : `api.service.ts`, `flux-ia.service.ts` (SSE)

**Agent :** toute modification Assistant doit mettre à jour `AssistantIATest`, `AssistantApiTest`, et les composants pages associés.

### 10.5 Fonctionnement détaillé (relevé dans `AssistantIA.java`, prompts 37 à 44 de l'historique)

| Élément | Valeur / comportement |
|---------|----------------------|
| Réutilisation | Même `LlmClient`, même `MasqueurDonnees`, même `PreparateurCode`, même traduction d'erreurs (`AnalyseurIA.executer` / `AnalyseurIA.traduire`) que la revue |
| Format de réponse | Markdown libre (pas de JSON) ; record `Reponse(contenu, langage, nbMasquages, dureeMs, fournisseur, modele)` |
| Limite code | `MAX_LIGNES = 500` (erreur `VALIDATION` : « Les outils IA acceptent 500 lignes au maximum… ») |
| Limite conversation | `MAX_MESSAGES = 20` derniers messages seulement ; le dernier message doit avoir le rôle `utilisateur` (sinon « Écrivez votre question. ») |
| Masquage | Appliqué au code joint **et** à chaque message de la conversation ; `nbMasquages` cumulé |
| Conversion | Le paramètre `cible` est obligatoire (« Choisissez le langage cible de la conversion. ») |
| Outil `explication` | Le code est envoyé **numéroté** (`PreparateurCode.numeroter`) pour citer les lignes |
| Outil `commit` | Le texte est étiqueté « Diff : » et non « Code : » |
| Streaming | `diffuser(Demande)` → `Flux<String>` ; `timeout` appliqué **entre deux morceaux** (`config.timeoutSec()`), `TimeoutException` → `LLM_TIMEOUT` |
| Mode demo | `AssistantDemo.java` fournit des réponses fixes sans LLM (provider `demo`) |
| Clé absente | `LLM_CLE_ABSENTE` (503) via `verifierDisponible()` |

**Consignes communes (`CONSIGNES_COMMUNES`)** — à ne pas affaiblir :

```text
Tu es RevIA, un assistant de développement logiciel pour des étudiants et des développeurs.
Rédige en français correct, en Markdown (le code et ses identifiants restent dans leur langue).
Mets tout code dans un bloc ``` suivi du nom du langage.
Les valeurs ***MASQUÉ*** sont des secrets masqués volontairement avant l'envoi : ne les devine jamais,
garde ***MASQUÉ*** ou remplace-les par une variable d'environnement.
Si la demande n'a aucun rapport avec la programmation, réponds poliment que tu ne traites que le développement logiciel.
```

**Consigne du chat (`CONSIGNE_CHAT`)** : « Réponds à la dernière question de l'utilisateur en t'appuyant sur la conversation et, s'il est fourni, sur le code joint. Sois concis et concret, avec un court exemple de code quand c'est utile. » — le message envoyé se termine par « Réponds au dernier message de l'utilisateur. » (correctif du prompt 39 : le modèle répondait au 1er message).

**Consignes par outil** (corrigées au prompt 43) :

| Outil | Règles clés |
|-------|-------------|
| `tests` | Framework standard du langage : **JUnit 5 + AssertJ** (Java), pytest, Jest, PHPUnit, xUnit, paquet `testing` (Go) ; couvrir nominal + limites + **erreurs** ; un seul bloc complet + liste « Ce qui est testé » |
| `documentation` | Code **complet** avec commentaires au format standard (Javadoc, docstrings, JSDoc/TSDoc, PHPDoc, XML C#) **sans modifier la logique** ; section « README » courte |
| `explication` | Pour un débutant, phrases courtes : « En bref », « Pas à pas » (numéros de ligne), « Points d'attention » |
| `commit` | Conventional Commits `type(portée): résumé` ≤ 72 car. + corps en puces dans un bloc ```text ; PR avec `## Résumé`, `## Changements`, `## Comment tester`, `## Risques` |
| `refactoring` | Même comportement ; **code complet** dans un seul bloc (jamais « … reste inchangé … ») + « Ce qui a changé et pourquoi » |
| `conversion` | Idiomatique (conventions et bibliothèque standard du langage cible), pas mot à mot ; « Points d'attention » (comportement, dépendances) |

### 10.6 Front-end Assistant / Outils (relevé dans le code)

| Fichier | Rôle |
|---------|------|
| `core/flux-ia.service.ts` | Lit le SSE avec `fetch` + `ReadableStream` (HttpClient ne sait pas lire un flux morceau par morceau) ; événements `morceau` (`{t}`), `fin` (infos), `erreur` (`{message}`) ; fonction `lireEvenement` testée |
| `shared/markdown.ts` | Rendu Markdown **sans librairie** : tout le texte est **échappé avant** mise en forme (anti-XSS) ; titres, gras, italique, listes, citations, blocs de code avec étiquette de langage |
| `shared/reponse-ia.component.ts` | Affichage de la réponse (bouton copier sur les blocs) |
| `core/transfert-code.service.ts` | Signal `{code, langage}` : `deposer()` puis `prendre()` (lecture unique) ; **le code transféré ne repasse pas par le serveur** |
| `pages/assistant/assistant.component.ts`, `pages/outils/outils.component.ts` | Consomment `TransfertCodeService.prendre()` à l'ouverture |
| `pages/revue/revue.component.ts` | Boutons « 🧪 Outils IA » et « 💬 Demander à l'assistant » → `ouvrir('outils' \| 'assistant')` → `transfert.deposer(code, langage)` |
| Tests | `markdown.spec.ts`, `flux-ia.service.spec.ts` |

---

## 11. Sécurité, quotas, messages utilisateur

### 11.1 Mesures (README §8)

| Mesure | Détail |
|--------|--------|
| Masquage LLM | MasqueurDonnees |
| Validation sortie | Bean Validation + lignes |
| Clé API | Env only ; `/api/systeme/info` sans clé |
| Quota jour | 50 revues/dev (configurable) **before LLM** |
| Bucket4j | 10 analyses/min/user ; 10 connexions/min/IP |
| Auth | BCrypt cost 12 ; JWT HS256 120 min |
| Isolation | Revues par auteur ; admin 403 si non admin |
| CORS | `REVIA_CORS_ORIGINS` |
| CSV | Anti-injection formule Excel |
| Headers | X-Frame-Options, nosniff, Referrer-Policy |

### 11.2 Messages HTTP (texte exact UI)

| Situation | HTTP | Message |
|-----------|------|---------|
| Timeout analyse | 504 | L'analyse prend plus de temps que prévu. Réessayez dans un instant. |
| Quota fournisseur | 503 | Le service d'analyse est très sollicité. Réessayez dans une minute. |
| Clé absente/invalide | 503 | Service d'analyse indisponible. L'administrateur a été prévenu. |
| JSON invalide | 502 | Analyse incomplète : la réponse de l'IA n'a pas pu être validée. Réessayez. |
| Quota utilisateur | 429 | Vous avez atteint vos N revues du jour. |
| Code invalide | 400 | Collez entre 1 et 2 000 lignes de code. |
| Rate limit | 429 | Trop de demandes… + `Retry-After` |

---

## 12. API REST complète

**Auth par défaut :** `Authorization: Bearer <jwt>` sur `/api/**`.

| Méthode | Route | Public ? | Rôle |
|---------|-------|----------|------|
| POST | `/api/auth/register` | Oui | Inscription |
| POST | `/api/auth/login` | Oui | `{token, expireDansSecondes, utilisateur}` |
| GET | `/api/me` | Non | Profil + quota du jour |
| PUT | `/api/me` | Non | Modifier nom |
| PUT | `/api/me/mot-de-passe` | Non | Changer MDP |
| POST | `/api/revues` | Non | `{titre?, langage, code, mode}` → rapport |
| GET | `/api/revues?...` | Non | Historique paginé |
| GET | `/api/revues/export.csv?...` | Non | CSV UTF-8 `;` |
| GET | `/api/revues/{id}` | Non | Détail rapport |
| DELETE | `/api/revues/{id}` | Non | Suppression |
| POST | `/api/problemes/{id}/faux-positif` | Non | `{valeur: bool}` |
| POST | `/api/problemes/{id}/corrige` | Non | Marquer corrigé |
| POST | `/api/problemes/{id}/utile` | Non | Feedback utile |
| POST | `/api/problemes/{id}/correction/copie` | Non | Compteur copie |
| GET | `/api/tableau-de-bord?jours=30` | Non | Stats |
| GET | `/api/admin/utilisateurs` | ADMIN | Liste |
| PUT | `/api/admin/utilisateurs/{id}/quota` | ADMIN | Quota |
| PUT | `/api/admin/utilisateurs/{id}/blocage` | ADMIN | Bloquer |
| GET/POST/PUT | `/api/admin/prompts` … `/activer` | ADMIN | Prompts |
| POST | `/api/assistant/chat` (+ `/flux`) | Non | Chat IA |
| POST | `/api/assistant/outils/{outil}` (+ `/flux`) | Non | Outils IA |
| GET | `/api/systeme/info` | Oui | Version, provider, model |
| GET | `/actuator/health` | Oui | Sonde |

**Contrat front :** `frontend/src/app/core/api.service.ts` — toute rupture de contrat exige mise à jour + tests.

---

## 13. Front-end Angular — routes, gardes, intégration API

### 13.1 Routes (`frontend/src/app/app.routes.ts`)

| Path | Garde | Page |
|------|-------|------|
| `/connexion` | inviteGuard | Auth |
| `/inscription` | inviteGuard | Auth |
| `/revues/nouvelle` | connecte + developpeur | Nouvelle revue |
| `/revues/:id` | connecte + developpeur | Rapport |
| `/revues/:id/problemes/:index` | connecte + developpeur | Correction |
| `/historique` | connecte + developpeur | Historique |
| `/tableau-de-bord` | connecte + developpeur | Dashboard |
| `/assistant` | connecte + developpeur | Assistant chat |
| `/outils` | connecte + developpeur | Boîte à outils |
| `/parametres` | connecte | Paramètres |
| `/admin` | connecte + admin | Admin |

### 13.2 Dev proxy

`frontend/proxy.conf.json` : `/api` et `/actuator` → `http://localhost:8085`.

### 13.3 Prod nginx

`frontend/nginx.conf` : proxy `/api/` → `http://backend:8085`, SPA fallback `index.html`, timeout lecture **120 s** (analyses longues / SSE).

### 13.4 Vercel

`frontend/vercel.json` : rewrite `/api/:path*` → `https://revia-api.onrender.com/api/:path*` ( **à mettre à jour** si URL Render change).

---

## 14. Configuration — variables, profils, application.yml

### 14.1 Profils Spring

| Profil | BDD | IA défaut |
|--------|-----|-----------|
| dev | H2 (`REVIA_DATA_DIR`) | demo (sans .env) |
| prod | PostgreSQL | gemini |

### 14.2 Variables d'environnement (README §10)

| Variable | Défaut / notes |
|----------|----------------|
| SPRING_PROFILES_ACTIVE | dev |
| PORT | 8085 (JAR) ; nginx 80→8080 host |
| REVIA_AI_PROVIDER | demo / gemini prod |
| REVIA_AI_MODEL | vide = défaut fournisseur |
| REVIA_AI_API_KEY | jamais commit |
| REVIA_AI_TIMEOUT | 30 |
| REVIA_AI_BASE_URL | Ollama distant, proxy OpenAI |
| DATABASE_* | prod / Render |
| REVIA_JWT_SECRET | ≥32 car prod |
| REVIA_JWT_MINUTES | 120 |
| REVIA_ADMIN_EMAIL / PASSWORD | bootstrap admin |
| REVIA_QUOTA_JOUR | 50 |
| REVIA_ANALYSES_MINUTE | 10 |
| REVIA_CONNEXIONS_MINUTE | 10 |
| REVIA_CORS_ORIGINS | http://localhost:4200 ou URL Vercel |
| REVIA_DATA_DIR | H2 files |

### 14.3 Bloc `revia:` dans `application.yml`

```yaml
revia:
  version: 1.0.0
  ai:
    provider: ${REVIA_AI_PROVIDER:demo}
    model: ${REVIA_AI_MODEL:}
    api-key: ${REVIA_AI_API_KEY:${OPENAI_API_KEY:${ANTHROPIC_API_KEY:}}}
    temperature: 0.2
    timeout-sec: ${REVIA_AI_TIMEOUT:30}
    max-lines: 2000
    chunk-lines: 300
    json-retries: 1
    prompt-resource: classpath:prompts/revue-system.st
  quotas:
    revues-par-jour: ${REVIA_QUOTA_JOUR:50}
    analyses-par-minute: ${REVIA_ANALYSES_MINUTE:10}
    connexions-par-minute: ${REVIA_CONNEXIONS_MINUTE:10}
```

### 14.4 Fichier `.env.example` (racine)

Copier vers `.env` : `DATABASE_PASSWORD`, `REVIA_JWT_SECRET`, admin password, `REVIA_AI_*`, `REVIA_CORS_ORIGINS`.

---

## 15. Scripts PowerShell — comportement ligne par ligne

### 15.1 `scripts/start.ps1`

1. `$racine` = parent de `scripts`
2. JAR = `backend\target\revia.jar` — si absent → appelle `build.ps1 -SkipTests`
3. Lit `.env` racine (UTF-8) : `KEY=value` → variables d’environnement **si pas déjà définies**
4. `REVIA_DATA_DIR` défaut = `%LOCALAPPDATA%\RevIA\data` (évite OneDrive)
5. Affiche fournisseur IA et comptes démo
6. Job : ouvre navigateur après 18 s sur `:8085`
7. `java -jar $jar`

### 15.2 `scripts/build.ps1`

1. `ng build --configuration production` dans `frontend/` (`npm ci` si pas de node_modules)
2. Supprime et recopie `frontend/dist/frontend/browser/*` → `backend/src/main/resources/static/`
3. `mvn -B package` (ou `-DskipTests` si `-SkipTests`)
4. Sortie : `backend/target/revia.jar`

**Important :** `static/` est gitignored — toujours regénérer via build.

---

## 16. Build, exécution, modes dev / JAR / Docker

### 16.1 Prérequis (README §3.1)

`java -version` (17+), `mvn -v`, `node -v` (20+), optionnel Docker, Chrome pour Karma.

### 16.2 Mode JAR unique (démo standard)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1
powershell -ExecutionPolicy Bypass -File scripts\start.ps1
```

URL : **http://localhost:8085**

### 16.3 Mode dev hot-reload

Terminal 1 : `cd backend; mvn spring-boot:run`  
Terminal 2 : `cd frontend; npm install; npx ng serve` → **http://localhost:4200** (proxy API)

Reset H2 : supprimer `REVIA_DATA_DIR`.

### 16.4 Gemini local

Fichier `.env` :

```
REVIA_AI_PROVIDER=gemini
REVIA_AI_API_KEY=<clé>
```

Ou variables session PowerShell puis `start.ps1`.

### 16.5 Docker Compose

```powershell
copy .env.example .env
# Renseigner REVIA_JWT_SECRET (32+ chars), DATABASE_PASSWORD, REVIA_AI_API_KEY, etc.
docker compose up -d --build
```

URL : **http://localhost:8080** (nginx front ; PostgreSQL 16 **non exposé** host).

Services : `db`, `backend`, `frontend` — voir `docker-compose.yml`.

### 16.6 JAR prod manuel

Variables prod + `java -jar backend\target\revia.jar` (README §3.5).

---

## 17. Déploiement Render, Vercel, CI GitHub Actions

### 17.1 CI (`.github/workflows/ci.yml`)

| Job | Actions |
|-----|---------|
| backend | checkout, Java 17, `mvn -B verify`, artifact surefire, summary jeu-essai |
| frontend | Node 20, `npm ci`, `ng test` headless, `ng build` prod |
| docker | `docker build ./backend`, `docker build ./frontend` (needs backend+frontend) |

### 17.2 Render (`render.yaml`)

- Service web Docker `revia-api`, root `backend`
- Health : `/actuator/health`
- DB managée `revia-db` → injecte DATABASE_HOST/PORT/NAME/USER/PASSWORD
- Secrets manuels dashboard : `REVIA_ADMIN_PASSWORD`, `REVIA_AI_API_KEY`
- `REVIA_CORS_ORIGINS` = URL Vercel (ex. `https://revia.vercel.app`)

### 17.3 Vercel

- Root directory : `frontend`
- `vercel.json` : rewrites API vers Render

### 17.4 Checklist déploiement agent

1. CI verte sur branche
2. Render blueprint appliqué ; health OK
3. Vercel déployé ; URL front connue
4. `REVIA_CORS_ORIGINS` = origin exacte Vercel
5. `vercel.json` destination = URL Render réelle
6. Test login + 1 revue depuis URL publique

---

## 18. Jeu d'essai IA — manifeste et objectifs

Fichiers : `backend/src/main/resources/jeu-essai/*` + `manifest.json`.

| Type | Fichiers | Objectif détection |
|------|----------|-------------------|
| Injection SQL | 01–04 | 4/4 |
| Secrets en dur | 05–07 | 3/3 |
| Ressources non fermées | 08–10 | 3/3 (Gemini : 10 manqué ligne exacte) |
| Bornes | 11–14 | 4/4 |
| XSS | 15–17 | 3/3 |
| Autres | 18–20 | 3/3 |
| Sains | 21–23 | 0 faux positif grave |

**Test automatisé :** `JeuEssaiIATest` → rapport `backend/target/jeu-essai-rapport.md`.

**Critère Gemini manuel :** problème à ±2 lignes du défaut attendu (`rapport-jeu-essai-gemini.md`).

---

## 19. Rapport Gemini — synthèse et leçons

Source : `docs/rapport-jeu-essai-gemini.md`.

| Métrique | Valeur |
|----------|--------|
| Modèle | gemini-flash-lite-latest |
| Détection | 19/20 (95 %) |
| Faux positifs sains | 0/3 |
| Latence moyenne | ~1,5 s |
| Échec | `10_ressource_journal.c` — fuite détectée L8 pas L4 |

**Leçons pour l’agent qui touche au prompt :**

1. Règle `***MASQUÉ***` = secret en dur → indispensable (sinon 17/20).
2. Consigne « français correct » → évite deformations type « externaliser ».
3. Ne pas repasser à `gemini-flash-latest` en prod gratuite (quota 20/j).

Historique essais : v1 → v1+MASQUÉ → final = 14→17→18→19 détections.

---

## 20. Tests automatisés — inventaire détaillé

**Commande :** `cd backend; mvn -B test`

| Classe | Tests | Focus |
|--------|-------|-------|
| MasqueurDonneesTest | 7 | Masquage secrets, emails, JDBC, idempotence |
| AnalyseurIATest | 10 | JSON, masquage envoyé, timeout 504, 429→503, retry JSON |
| RevueTest | 6 | Score 36, faux positif, CSV injection |
| JeuEssaiIATest | 1 | 23 extraits manifest |
| LlmConfigTest | 2 | Providers |
| RedacteurRevueTest | 5 | Texte demo déterministe |
| ApiIntegrationTest | 7 | Auth, revue, isolation, quota, admin |
| ErreursApiTest | 4 | Erreurs LLM, Bucket4j |
| AssistantIATest | 9 | Chat/outils IA unitaire |
| AssistantApiTest | 3 | MockMvc assistant + SSE |

**Total actuel attendu : 54 tests** (README mentionne 42 — documenter l’écart si README non mis à jour).

**Front :** `npx ng test --watch=false --browsers=ChromeHeadless` — **14 tests** (README).

**Windows Chrome :**  
`$env:CHROME_BIN="C:\Program Files\Google\Chrome\Application\chrome.exe"`

---

## 21. Parcours de test manuel (15 étapes)

Reprise README §4.1 — l’agent doit cocher chaque étape après smoke test :

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Ouvrir :8085 ou :8080 | Page connexion thème RevIA |
| 2 | sarra@revia.local / Sarra2026! | Nouvelle revue + quota sidebar |
| 3 | Exemple + Lancer revue | Animation, score, badges, marqueurs Monaco |
| 4 | Clic carte problème | Scroll ligne éditeur |
| 5 | Filtres catégories | Liste filtrée |
| 6 | Voir correction | Diff, OWASP, contexte modèle |
| 7 | Copier, corrigé, utile, faux positif | Notifications ; score mis à jour si FP |
| 8 | Modifier code + relancer | Nouvelle entrée historique |
| 9 | Import fichier | Titre/langage remplis |
| 10 | Historique | Filtres, CSV, pagination |
| 11 | Tableau de bord | Graphiques, récurrents |
| 12 | Paramètres | Profil, MDP, infos IA |
| 13 | admin@revia.local | Quotas, blocage, prompts |
| 14 | Code vide ou >2000 lignes | Message 400 |
| 15 | Quota 1 + 2 revues | 429 quota |

**Bonus agent :** tester `/assistant` et un outil (`explication`) si Gemini/demo configuré.

---

## 22. UI / UX — maquettes, couleurs, captures

### 22.1 Couleurs (README §13)

| Token | Hex |
|-------|-----|
| Fond | `#0f1222` |
| Indigo principal | `#5b5bf6` |
| Accent IA | `#a5b4fc` |
| Gravités | rouge / orange / jaune |

Thème Monaco custom : `revia-sombre`.

### 22.2 Captures de référence (`docs/captures/`)

| Fichier | Écran |
|---------|-------|
| 01_connexion.png | Login |
| 02_revue.png | Rapport revue |
| 03_correction.png | Page correction |
| 04_historique.png | Historique |
| 05_tableau.png | Tableau de bord |
| 06_nouvelle_revue.png | Éditeur nouvelle revue |
| 07_admin.png | Administration |

Après refonte UI : **regénérer les captures** ou mettre à jour README §11.5.

---

## 23. Arborescence complète du dépôt

```
Revia_PE/
├── README.md
├── RESUME.md
├── .env.example
├── .gitignore
├── docker-compose.yml
├── render.yaml
├── scripts/
│   ├── build.ps1
│   └── start.ps1
├── docs/
│   ├── PROMPT_AGENT_IA_DETAILLE.md   ← ce fichier
│   ├── rapport-jeu-essai-gemini.md
│   └── captures/*.png
├── .github/workflows/ci.yml
├── backend/
│   ├── pom.xml, Dockerfile
│   └── src/main/java/com/revia/ …
│   └── src/main/resources/
│       ├── application*.yml
│       ├── prompts/revue-system.st
│       ├── db/migration/V1__schema_initial.sql
│       └── jeu-essai/
└── frontend/
    ├── angular.json, package.json
    ├── proxy.conf.json, nginx.conf, vercel.json, Dockerfile
    └── src/app/{core,layout,shared,pages}/
```

---

## 24. Dépannage (README §16)

| Problème | Action agent |
|----------|--------------|
| Port 8085 occupé | Tuer process ou `$env:PORT=8090` |
| Monaco charge indéfiniment | `build.ps1` ; vérifier `/assets/monaco/...` |
| Service analyse indisponible | Clé manquante ou provider payant sans clé → `.env` ou `demo` |
| Session expirée | JWT 2h — reconnecter |
| Données demo corrompues | Supprimer `%LOCALAPPDATA%\RevIA\data` |
| Karma sans Chrome | `CHROME_BIN` |
| esbuild npm warning | Ignorer ou `npm rebuild esbuild` |
| CORS prod | Aligner `REVIA_CORS_ORIGINS` |
| Docker backend crash | `REVIA_JWT_SECRET` manquant dans `.env` |

---

## 25. Checklist « projet prêt » — 120 points

Cocher **OK/KO** dans le rapport final de l’agent.

### A. Documentation et gouvernance (1–10)

1. RESUME.md lu et toujours exact  
2. README.md § pertinent lu avant changement  
3. rapport-jeu-essai-gemini.md consulté si toucher IA  
4. Captures docs/captures comparées si changement UI  
5. Aucune clé API dans `git status`  
6. `.env` présent localement si Gemini requis  
7. `.env.example` à jour des variables utilisées  
8. Pas de secrets dans logs commités  
9. Remote git identifié (Revia_PE)  
10. Ce prompt suivi pour le rapport final  

### B. Environnement outils (11–20)

11. Java 17+ installé  
12. Maven 3.9+ installé  
13. Node 20+ installé  
14. npm fonctionne dans frontend  
15. Docker disponible si tâche P prod  
16. Chrome disponible si tests front locaux  
17. PORT 8085 libre ou alternatif documenté  
18. REVIA_DATA_DIR writable  
19. Ollama optionnel documenté si utilisé  
20. PowerShell ExecutionPolicy bypass OK pour scripts  

### C. Build artifacts (21–30)

21. `npm ci` réussit  
22. `ng build --configuration production` réussit  
23. static/ copié dans backend resources  
24. `mvn package` réussit  
25. `backend/target/revia.jar` existe  
26. JAR démarre sans exception Flyway (profil adapté)  
27. Assets Monaco servis par JAR  
28. Taille bundle Angular acceptable (pas de régression énorme)  
29. build.ps1 -SkipTests seulement si tests lancés séparément  
30. Pas de node_modules commité  

### D. Tests backend (31–45)

31. mvn test BUILD SUCCESS  
32. MasqueurDonneesTest 7/7  
33. AnalyseurIATest 10/10  
34. RevueTest 6/6  
35. JeuEssaiIATest 1/1  
36. LlmConfigTest 2/2  
37. RedacteurRevueTest 5/5  
38. ApiIntegrationTest 7/7  
39. ErreursApiTest 4/4  
40. AssistantIATest 9/9  
41. AssistantApiTest 3/3  
42. Rapport jeu-essai généré si JeuEssaiIATest run  
43. Aucun test ignoré sans raison  
44. Pas d’appel réseau LLM réel en CI  
45. Couverture régression masquage OK  

### E. Tests frontend (46–55)

46. npm ci frontend  
47. ng test 14/14  
48. CHROME_BIN configuré si Windows  
49. ng build prod OK  
50. proxy.conf.json pointe 8085 en dev  
51. api.service routes alignées backend  
52. flux-ia SSE testé si modifié  
53. Guards auth/admin/dev OK  
54. Pas d’erreur lint bloquante introduite  
55. Monaco s’affiche en dev et prod build  

### F. Sécurité (56–70)

56. JWT requis sur routes protégées  
57. 401 sans token  
58. 403 admin pour non-admin  
59. 404 revue autre user  
60. BCrypt mots de passe  
61. Rate limit 429 + Retry-After  
62. Quota jour before LLM  
63. Masqueur appliqué avant LLM (test)  
64. /api/systeme/info sans clé  
65. CORS restrictif prod  
66. CSV formula injection protégée  
67. stacktrace jamais exposée client  
68. multipart désactivé (upload via JSON texte)  
69. Compte bloqué → 401 immédiat  
70. Secrets masqués dans prompt LLM  

### G. Fonctionnel revue code (71–85)

71. Login sarra OK  
72. Login admin OK  
73. Inscription OK  
74. Exemple intégré charge  
75. Revue demo ou gemini OK  
76. Score affiché cohérent formule  
77. Faux positif recalcule score  
78. Correction diff visible  
79. OWASP affiché si sécurité  
80. Import fichier OK  
81. Historique filtres OK  
82. Export CSV téléchargeable  
83. Tableau de bord charge  
84. Paramètres changement nom OK  
85. Admin quota + prompt version OK  

### H. Assistant IA (86–92)

86. Route /assistant accessible dev  
87. POST chat avec JWT OK  
88. SSE flux démarre (si testé)  
89. Outil explication OK  
90. Outil inconnu erreur claire  
91. Rate limit assistant OK  
92. Pas de fuite clé via assistant  

### I. Docker / prod locale (93–102)

93. .env docker complet  
94. docker compose build OK  
95. postgres healthy  
96. backend prod profile  
97. frontend nginx proxy /api  
98. http://localhost:8080 login OK  
99. Flyway V1 appliquée  
100. REVIA_JWT_SECRET ≥32  
101. Gemini ou demo fonctionne en container  
102. Ollama host.docker.internal documenté si besoin  

### J. Cloud CI (103–110)

103. Push déclenche CI  
104. Job backend vert  
105. Job frontend vert  
106. Job docker vert  
107. render.yaml cohérent  
108. vercel.json URL API correcte  
109. REVIA_CORS_ORIGINS prod correct  
110. Health Render /actuator/health  

### K. Conformité sujet (111–120)

111. API IA gratuite utilisée (Gemini) en démo jury  
112. Mode demo disponible sans clé  
113. 5 étapes IA respectées  
114. 11 cas d’usage manuels possibles  
115. Diagramme classes intact  
116. Messages erreur annexe  
117. Jeu essai demo ≥85%  
118. Gemini doc ≥85% si retest  
119. Rapport technique README à jour  
120. Definition of done §8 phrase finale vraie  

---

## 26. Plan de travail agent — 12 phases

| Phase | Objectif | Livrable |
|-------|----------|----------|
| 0 | Lecture RESUME + README sections 1–4, 8–10, 16 | Notes risques |
| 1 | Vérifier prérequis outils | Log versions |
| 2 | Cloner/pull + état git propre secrets | git status |
| 3 | Créer .env local si Gemini | .env gitignored |
| 4 | build.ps1 complet | revia.jar |
| 5 | mvn verify + ng test + ng build | Logs SUCCESS |
| 6 | start.ps1 smoke | Capture ou notes 15 étapes |
| 7 | Corriger échecs tests | Diff minimal |
| 8 | JeuEssai + lire jeu-essai-rapport.md | OK/KO |
| 9 | Docker compose si scope prod | :8080 OK |
| 10 | CI + cloud si scope deploy | URLs |
| 11 | Checklist §25 | Table 120 points |
| 12 | Rapport utilisateur FR : fait, reste, commandes | Markdown |

---

## 27. Conventions de code pour l’agent

- **Java :** style existant, records DTOs, validation Jakarta, pas de log de données sensibles.
- **Angular :** standalone components, lazy routes, Material, SCSS global `styles.scss`.
- **Nommage :** français métier (Revue, Probleme, gravité enum, messages utilisateur FR).
- **Erreurs :** `ApiException` + `ErrorCode` + message utilisateur (pas stack trace).
- **IA :** toute règle nouvelle demo → `AnalyseurStatique` + test ; LLM → `revue-system.st` + JeuEssai.
- **Commits :** l’agent ne commit **que si l’utilisateur le demande** explicitement.
- **Diff :** une feature / un bugfix par série de changements cohérente.

---

## 28. Écarts assumés et limites connues

Reprise README §14 + RESUME :

| Sujet | Détail |
|-------|--------|
| Score maquette 38 vs 36 | Formule officielle = 36 |
| Correction JSON | Objet {codeAvant, codeApres, justification} |
| Admin vs dev UC | Code = diagramme classes |
| Demo vs LLM | Demo ne comprend pas métier complexe |
| Gemini gratuit | Quotas ; message 503 si saturé |
| OneDrive | node_modules lourd — préférer clone hors sync |
| README 42 tests | Peut être 54 avec Assistant — mettre README à jour si demandé |
| 10_ressource_journal.c | Gemini ligne ±2 parfois raté |
| `SpaController` (corrigé) | `/assistant` et `/outils` manquaient dans `web/SpaController.java` : en mode JAR (:8085), F5 ou lien direct renvoyait 404. Les 2 routes ont été ajoutées ; **toute nouvelle route Angular de premier niveau doit être ajoutée à ce `@GetMapping`**. Docker (nginx `try_files`) et Vercel n'étaient pas concernés |
| Sens du transfert de code | L'historique (prompt 44) parle d'un bouton « Envoyer en revue » (outils/chat → revue). Le code livré fait l'inverse : **revue → assistant / outils** (`revue.component.ts` `ouvrir()`). Voir §31 |
| Docker Compose local | `docker-compose.yml` fixe `POSTGRES_PASSWORD: revia123` et `REVIA_ADMIN_PASSWORD: revia123` en dur, alors que `DATABASE_PASSWORD` et `REVIA_JWT_SECRET` viennent de `.env` : si `DATABASE_PASSWORD` ≠ `revia123`, le backend échoue (« password authentication failed for user "revia" »), nginx renvoie **502** sur `/api/**` |

---

## 29. Historique réel des prompts de construction (44 prompts, 10 phases)

Source verbatim : `docs/historique-prompts.md`. Les prompts ci-dessous sont **recopiés tels quels** (fautes et minuscules comprises : c'est la trace réelle), dans des blocs à 4 backticks pour que les ``` internes ne cassent pas le rendu. Chaque prompt est complété de trois rubriques ajoutées pour l'agent :

- **Résultat** : ce que le LLM a produit (relevé de l'historique, quand il est connu) ;
- **Ancrage** : fichiers du dépôt qui matérialisent le prompt (vérifiés dans le code) ;
- **À retenir** : la règle à ne pas casser ou la leçon de prompting.

**Fil conducteur :** au début les prompts sont vagues (« tu peux me faire ça ? »), ensuite ils deviennent précis (versions, contraintes, format de sortie, erreur collée, cas de test). C'est exactement la progression à reproduire quand on pilote un LLM : contexte → contraintes → format attendu → erreur réelle collée → critère de validation.

**Ordre de reconstruction possible** (si l'agent doit refaire le projet de zéro) : Phase 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10, en lançant `mvn test` / `ng test` à la fin de chaque phase.

| Phase | Thème | Prompts | Livrable principal |
|-------|-------|---------|--------------------|
| 1 | Idée et démarrage | 1–4 | Structure, `pom.xml`, projet Angular |
| 2 | Backend de base | 5–8 | Entités, repositories, `RevueService` |
| 3 | Authentification | 9–11 | JWT, `SecurityConfig`, `DataInitializer` |
| 4 | Brancher l'IA | 12–17 | `LlmClient`, prompt `revue-system.st`, `ReponseIA` |
| 5 | Sécurité des données envoyées | 18–22 | `MasqueurDonnees`, `AnalyseurStatique` |
| 6 | Frontend | 23–26 | Pages Angular, design, gestion d'erreurs |
| 7 | Admin, quotas, prompts versionnés | 27–29 | `PromptSysteme`, `AdminController`, `QuotaService`, `RateLimitFilter` |
| 8 | Tests et jeu d'essai | 30–33 | `jeu-essai/`, `JeuEssaiIATest`, compte démo, rapport |
| 9 | Déploiement | 34–36 | Docker Compose, `application-prod.yml`, CI, README/RESUME |
| 10 | Assistant IA et Outils | 37–44 | `AssistantIA`, SSE, Markdown, 6 outils, transfert de code |

---

### 29.1 Phase 1 : l'idée et le démarrage

#### Prompt 1

````text
salut, je dois faire un projet en IA pour mon cours. je veux faire un site qui fait la revue de code avec l'ia.
tu peux me faire ça ?
````

- **Résultat :** une seule page HTML avec un textarea et un appel direct à une API depuis le navigateur (clé visible dans le code). Pas utilisable.
- **Ancrage :** aucun (rejeté).
- **À retenir :** origine de l'interdiction « jamais d'appel LLM depuis le navigateur / jamais de clé côté client » (§1, §2.3, §11).

#### Prompt 2

````text
non je veux un vrai projet avec un frontend et un backend. le prof veut angular et spring boot.
c'est quoi les étapes pour commencer ?
````

- **Résultat :** liste d'étapes générale, sans structure de dossiers ni versions.
- **Ancrage :** stack imposée par le sujet → §4.
- **À retenir :** sans versions précises dans le prompt, le LLM reste générique.

#### Prompt 3

````text
ok. donne moi la structure des dossiers du projet avec angular 17 pour le front et spring boot 3 (java 17) pour le back.
le projet s'appelle RevIA. explique moi chaque dossier en 1 ligne.
````

- **Ancrage :** arborescence `backend/` + `frontend/` → §23 ; packages `com.revia.{web,service,ai,domain,repository,security,error,config}` → §6.3.

#### Prompt 4

````text
donne moi le pom.xml du backend avec les dépendances : web, jpa, h2 pour le dev, postgresql pour la prod, security, validation.
et la commande pour créer le projet angular avec le routing.
````

- **Ancrage :** `backend/pom.xml` (ensuite complété : `spring-ai-client-chat` / `spring-ai-openai` / `spring-ai-anthropic` 1.1.8, `spring-retry`, jjwt 0.12.6, `bucket4j_jdk17-core` 8.14.0, Flyway 11.7.2 + `flyway-database-postgresql`, actuator ; le `Flux` du streaming vient de Reactor via Spring AI) → §4.2 ; `frontend/package.json`, `angular.json`.
- **À retenir :** H2 en dev / PostgreSQL en prod = origine des profils `dev` / `prod` (§14.1).

---

### 29.2 Phase 2 : le backend de base

#### Prompt 5

````text
fais moi les entités : un utilisateur, une revue de code, et les problèmes trouvés dans la revue.
````

- **Résultat :** entités en anglais (User, Review, Issue), sans gravité ni catégorie.
- **À retenir :** un prompt sans nommage ni attributs produit un modèle générique inutilisable.

#### Prompt 6

````text
refais les entités en français stp (Utilisateur, Revue, Probleme).
Utilisateur doit être abstrait avec 2 types : Developpeur et Administrateur.
Un Probleme a : ligne, categorie (securite, bug, bonne_pratique, performance), gravite (critique, majeure, mineure, info),
titre, explication, et une correction (code avant, code après, justification).
Une Revue a : titre, langage, code, score sur 100, résumé, date, et une liste de problèmes.
Utilise des enums pour la catégorie et la gravité.
````

- **Ancrage :** `domain/Utilisateur.java` (abstract, SINGLE_TABLE), `Developpeur.java`, `Administrateur.java`, `Revue.java`, `Probleme.java`, `Correction.java`, `Categorie.java`, `Gravite.java`, `db/migration/V1__schema_initial.sql` → §7.
- **À retenir :** le nommage français et les valeurs d'enum de ce prompt sont le **contrat JSON** du LLM (champs `categorie`, `gravite` du prompt système). Ne pas les renommer sans mettre à jour `ReponseIA`, le front (`models.ts`, `libelles.ts`) et les tests.

#### Prompt 7

````text
maintenant les repositories et un service RevueService avec créer une revue, lister mes revues, voir une revue, supprimer.
pour l'instant l'analyse peut renvoyer un faux résultat, on branchera l'ia après.
````

- **Ancrage :** `repository/{Utilisateur,Revue,Probleme,PromptSysteme}Repository.java`, `service/RevueService.java`.
- **À retenir :** « lister **mes** revues » → origine de l'isolation par auteur (toutes les requêtes filtrent par auteur ; revue d'un autre = 404, §2.3).

#### Prompt 8

````text
j'ai cette erreur au démarrage :

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'revueService'
... No qualifying bean of type 'com.revia.repository.ProblemeRepository' available

c'est quoi le problème ?
````

- **Ancrage :** `repository/ProblemeRepository.java` (interface manquante / non détectée par le scan JPA).
- **À retenir :** coller la **trace d'erreur complète** donne un diagnostic exact ; première occurrence du réflexe qui deviendra la règle (prompts 16, 20, 26, 32).

---

### 29.3 Phase 3 : l'authentification

#### Prompt 9

````text
il faut que les gens puissent se connecter. comment je fais ?
````

- **À retenir :** question ouverte → réponse théorique ; le prompt 10 précise la technologie.

#### Prompt 10

````text
je veux du JWT avec spring security, pas de session. donne moi :
- JwtService (générer et vérifier le token)
- JwtAuthFilter
- SecurityConfig (les routes /api/auth/** publiques, le reste protégé, /api/admin/** seulement pour ADMIN)
- AuthController avec inscription et connexion
Le mot de passe doit être hashé avec BCrypt. La clé JWT doit venir de application.yml, pas en dur.
````

- **Ancrage :** `security/{JwtService,JwtAuthFilter,SecurityConfig,UtilisateurConnecte}.java`, `web/AuthController.java`, `service/AuthService.java` → §6.1, §11, §12.
- **À retenir :** valeurs finales : BCrypt coût 12, JWT HS256 120 min (`REVIA_JWT_MINUTES`), secret `REVIA_JWT_SECRET` ≥ 32 caractères en prod. Le compte bloqué est rechargé à chaque requête (401 immédiat).

#### Prompt 11

````text
au démarrage il faut créer un compte admin automatiquement s'il n'existe pas.
l'email et le mot de passe viennent des variables d'environnement REVIA_ADMIN_EMAIL et REVIA_ADMIN_PASSWORD.
en dev, si rien n'est défini, mets un mot de passe par défaut. en prod, ne crée rien et affiche un warning.
fais une classe DataInitializer.
````

- **Ancrage :** `config/DataInitializer.java` : `creerAdmin()` ne fait rien si un administrateur existe déjà ; mot de passe vide + profil `prod` → `log.warn` « Aucun administrateur : définissez REVIA_ADMIN_EMAIL et REVIA_ADMIN_PASSWORD… » et aucune création ; sinon défaut **`Admin2026!`**. Le même `run()` appelle `prompts.actif()` (crée la v1 du prompt) et, si `revia.seed-demo-data=true`, `creerDemo()` (voir prompt 33).
- **À retenir :** comptes dev = `admin@revia.local / Admin2026!` et `sarra@revia.local / Sarra2026!` (§1).

---

### 29.4 Phase 4 : brancher l'IA (Gemini)

#### Prompt 12

````text
comment j'appelle gemini depuis spring boot pour analyser le code ?
````

- **Résultat :** appel HTTP brut avec `RestTemplate`, clé dans l'URL, aucune gestion d'erreur.
- **À retenir :** rejeté ; il a motivé le choix de Spring AI (prompt 13).

#### Prompt 13

````text
je préfère utiliser Spring AI. fais une interface LlmClient avec une méthode rediger(systeme, message)
et une implémentation SpringAiLlmClient. Le fournisseur et le modèle doivent être configurables dans application.yml.
Ajoute aussi un fournisseur "demo" qui renvoie des réponses fixes, pour tester sans clé et sans payer.
````

- **Ancrage :** `ai/LlmClient.java`, `ai/SpringAiLlmClient.java`, `ai/LlmConfig.java`, `config/ReviaProperties.java`, bloc `revia.ai` d'`application.yml` (§14.3), fournisseurs §8.1, `RedacteurRevue.java` + `AnalyseurStatique.java` pour le mode demo.
- **À retenir :** l'interface unique `LlmClient` est la raison pour laquelle on change de fournisseur (gemini, groq, ollama, openai, anthropic, demo) **sans toucher au code métier**. L'assistant (§10) l'a ensuite réutilisée avec `redigerEnFlux` et `disponible()`.

#### Prompt 14

````text
c'est quoi le prompt que je dois envoyer à gemini pour qu'il fasse la revue ?
````

- **Résultat :** « Analyse ce code et donne les erreurs. » Réponses en texte libre, impossibles à afficher proprement.
- **À retenir :** un prompt demandé « au LLM » reste générique. Il faut écrire soi-même le rôle, le périmètre et le **format de sortie**.

#### Prompt 15

````text
le prompt doit forcer une réponse que je peux parser en java. voilà ce que j'ai écrit, améliore le si besoin
et mets le dans un fichier resources/prompts/revue-system.st :

Tu es un développeur senior spécialisé en revue
de code et en sécurité applicative (OWASP Top 10).

Analyse l'extrait de code {langage} ci-dessous.
Les lignes sont numérotées.
Repère les bugs, les mauvaises pratiques et
les failles de sécurité. Pour chaque problème, indique :
ligne, catégorie, gravité, explication courte, correction.
N'invente aucun problème : si le code est correct,
renvoie une liste vide.
Réponds uniquement en JSON, sans texte autour

Ajoute à la fin le format JSON exact avec les champs de mon entité Probleme (score, resume, problemes[...]).
Le code doit être envoyé avec les numéros de ligne devant chaque ligne.
````

- **Ancrage :** `backend/src/main/resources/prompts/revue-system.st` (texte final en §9), `ai/PreparateurCode.java` (format `NNN | ligne`, chunks de 300 lignes), `ai/PromptService.java`.
- **À retenir :** c'est le prompt **écrit par l'humain** qui a fonctionné : rôle, périmètre, règle anti-invention (« liste vide »), sortie JSON stricte, lignes numérotées. Le champ `owasp` et la variable `{mode}` ont été ajoutés ensuite (§9).

#### Prompt 16

````text
des fois gemini renvoie le json entouré de ```json ... ``` et ça plante :

com.fasterxml.jackson.core.JsonParseException: Unexpected character ('`' (code 96))

fais une classe ReponseIA qui nettoie la réponse (enlève les balises, prend du premier { au dernier })
et qui valide les champs. si le json est invalide, renvoie une erreur claire à l'utilisateur, pas une stacktrace.
````

- **Ancrage :** `ai/ReponseIA.java` (nettoyage + Bean Validation), `AnalyseurIA` (1 retry : `json-retries: 1`), `ErrorCode.LLM_REPONSE_INVALIDE` (502 : « Analyse incomplète : la réponse de l'IA n'a pas pu être validée. Réessayez. »), `error/GlobalExceptionHandler.java`.
- **À retenir :** ne jamais faire confiance au format de sortie d'un LLM : nettoyer, valider, retenter une fois, puis erreur claire.

#### Prompt 17

````text
problème : sur un code qui est correct, gemini invente quand même 2 ou 3 problèmes mineurs.
et parfois le numéro de ligne n'existe pas dans le code.
ajoute dans le prompt que la ligne doit être exactement un des numéros affichés,
et côté java ignore les problèmes dont la ligne est hors de l'extrait.
````

- **Ancrage :** ligne « Le numéro de "ligne" doit être exactement l'un des numéros affichés devant le code. » dans `revue-system.st` ; filtrage des lignes inexistantes dans `ReponseIA` / `AnalyseurIA` (test `AnalyseurIATest`).
- **À retenir :** **défense en profondeur** : une consigne dans le prompt **et** un contrôle côté code.

---

### 29.5 Phase 5 : sécurité des données envoyées à l'IA

#### Prompt 18

````text
je ne veux pas que les mots de passe ou les clés api qui sont dans le code des utilisateurs partent chez google.
comment faire ?
````

- **À retenir :** exigence de confidentialité à l'origine de `MasqueurDonnees` et de la règle « masquage appliqué avant tout envoi LLM » (§8 étape 02, §25 point 63).

#### Prompt 19

````text
fais une classe MasqueurDonnees qui remplace par ***MASQUÉ*** avant l'envoi :
mots de passe, clés api, tokens, chaînes de connexion, emails, clés privées.
elle renvoie le code masqué + le nombre de masquages (pour l'afficher dans l'interface).
écris aussi les tests JUnit avec des exemples.
````

- **Ancrage :** `ai/MasqueurDonnees.java` (record `Resultat(code, nbMasquages)`), `MasqueurDonneesTest` (7 tests : secrets, emails, JDBC, idempotence). Couvre aussi secrets OpenAI/AWS/GitHub/Stripe/Google/Slack/JWT (§8) et **conserve le nombre de lignes**.
- **À retenir :** `nbMasquages` est affiché à l'utilisateur et renvoyé par l'assistant (`Reponse.nbMasquages`).

#### Prompt 20

````text
depuis le masquage, gemini dit que ***MASQUÉ*** est une erreur de syntaxe.
et pire : sur mon fichier de test avec PASSWORD = "admin123" il ne signale plus le mot de passe en dur,
puisqu'il ne voit plus la valeur.
ajoute une consigne dans le prompt pour ça : ne pas traiter ***MASQUÉ*** comme une erreur,
mais si c'est la valeur d'un mot de passe ou d'un secret dans le code, le signaler en critique (OWASP A07:2021).
````

- **Ancrage :** paragraphe « Les valeurs ***MASQUÉ*** ont été masquées… » de `revue-system.st` (§9).
- **À retenir :** **effet de bord du masquage** sur la détection. Chiffre du rapport Gemini : sans cette règle on tombe à 17/20 détections au lieu de 19/20 (§19 leçon 1).

#### Prompt 21

````text
les explications de gemini mélangent français et anglais ("hardcoded", "externaliser", "fallback").
ajoute au prompt qu'il doit écrire en français correct sans mot d'une autre langue.
````

- **Ancrage :** consigne « Rédige tous les textes en français correct, sans aucun mot d'une autre langue ni d'un autre alphabet… » de `revue-system.st` (§9, §19 leçon 2).
- **À retenir :** donner un **contre-exemple** (« le déplacer dans une variable d'environnement » plutôt que « l'externaliser ») est plus efficace qu'une règle abstraite.

#### Prompt 22

````text
ajoute aussi un AnalyseurStatique en java qui détecte les cas évidents sans IA
(mot de passe en dur, concaténation SQL, System.out, catch vide...) pour compléter la réponse de gemini
et éviter qu'il rate des choses importantes.
````

- **Ancrage :** `ai/AnalyseurStatique.java` (~25 règles), utilisé par le mode `demo` avec `RedacteurRevue` (`RedacteurRevueTest`, 5 tests) ; règle de conduite §27 : « toute règle nouvelle demo → `AnalyseurStatique` + test ».
- **À retenir :** le LLM n'est pas la seule source de détection ; le jeu d'essai demo atteint 20/20 défauts, 0/3 faux positifs (§3.5).

---

### 29.6 Phase 6 : le frontend

#### Prompt 23

````text
maintenant le front angular. je veux une page de connexion et une page où on colle le code et on voit les résultats.
````

- **Résultat :** correct mais très basique, sans design et avec des modules `NgModule`.

#### Prompt 24

````text
refais en standalone components (angular 17), avec les signals.
Pages :
- auth (connexion / inscription)
- revue (éditeur de code + choix du langage + bouton analyser)
- correction (liste des problèmes avec la gravité en couleur, et un diff avant/après pour chaque correction)
- historique (mes revues)
- tableau de bord (stats : score moyen, problèmes par catégorie, évolution dans le temps)
Un ApiService pour les appels, un AuthService qui garde le token, et un intercepteur qui ajoute le header Authorization.
Mets un layout avec une barre de navigation à gauche.
````

- **Ancrage :** `app.routes.ts` (§13.1), `core/{api.service,auth.service,http}.ts`, `layout/shell.component.ts`, `pages/{auth,revue,correction,historique,tableau-de-bord}/`, `shared/{diff,code-editor.component}.ts` (Monaco via `ngx-monaco-editor-v2`), `shared/score-ring.component.ts` (+ spec).
- **À retenir :** « standalone + signals » est une **contrainte de style** à respecter (§27) ; l'intercepteur est `authInterceptor` dans `core/http.ts`.

#### Prompt 25

````text
le design est trop simple. je veux un style moderne et propre :
- un anneau de score coloré (rouge < 50, orange < 75, vert sinon)
- des badges pour la gravité et la catégorie
- un mode sombre
- des icônes svg simples
pas de bibliothèque ui lourde, juste du css.
````

- **Ancrage :** `shared/score-ring.component.ts`, `shared/badges.ts`, `shared/icone.component.ts`, `styles.scss` ; palette §22.1 (fond `#0f1222`, indigo `#5b5bf6`, accent IA `#a5b4fc`) ; thème Monaco `revia-sombre`.
- **À retenir :** seuils de couleur du score : rouge < 50, orange < 75, vert ≥ 75. (Angular Material est tout de même présent dans la stack §4.1 pour certains composants ; le style visuel reste du CSS maison.)

#### Prompt 26

````text
quand le token expire, le front affiche juste une page blanche. et les erreurs du backend s'affichent en json brut.
fais en sorte que :
- si 401, on retourne à la page de connexion avec un message
- les erreurs s'affichent dans une notification en français
côté backend, fais un GlobalExceptionHandler avec un ErrorCode (VALIDATION, INTROUVABLE, LLM_TIMEOUT, LLM_CLE_ABSENTE, QUOTA...).
````

- **Ancrage :** `error/{ErrorCode,ApiException,ErreurApi,GlobalExceptionHandler}.java` ; catalogue final : `VALIDATION`, `CODE_LONGUEUR`, `NON_AUTHENTIFIE`, `IDENTIFIANTS_INVALIDES`, `COMPTE_BLOQUE`, `INTERDIT`, `INTROUVABLE`, `EMAIL_DEJA_UTILISE`, `QUOTA_UTILISATEUR`, `DEBIT_LIMITE`, `LLM_TIMEOUT`, `LLM_QUOTA`, `LLM_CLE_ABSENTE`, `LLM_REPONSE_INVALIDE`, `LLM_INDISPONIBLE`, `INTERNE` (messages exacts en §11.2). Front : `authInterceptor` (sur 401 hors `/api/auth/` → `auth.deconnexion()`), `messageErreur()` (jamais d'erreur technique brute, repli hors-ligne « Impossible de joindre le serveur RevIA… »), `core/notif.service.ts`, gardes `connecteGuard` / `adminGuard` / `developpeurGuard` / `inviteGuard`.
- **À retenir :** les messages d'erreur viennent de l'annexe « Gestion des erreurs de l'API » de la présentation ; le détail technique part dans les journaux, jamais dans la réponse.

---

### 29.7 Phase 7 : admin, quotas et prompts versionnés

#### Prompt 27

````text
il faut une page admin. qu'est-ce que je peux mettre dedans ?
````

- **À retenir :** prompt ouvert utile ici : on demande des **idées**, puis on sélectionne (prompt 28).

#### Prompt 28

````text
ok je garde ça :
1. liste des utilisateurs (activer / désactiver)
2. gestion des versions du prompt système : l'admin peut publier une nouvelle version et choisir laquelle est active.
   la v1 est créée au démarrage depuis le fichier revue-system.st.
   refuse une nouvelle version si elle ne contient pas {langage}.
3. statistiques globales
Fais l'entité PromptSysteme, PromptService, AdminController et la page admin angular.
````

- **Ancrage :** `domain/PromptSysteme.java`, `repository/PromptSystemeRepository.java`, `ai/PromptService.java` (`actif()` crée la v1 depuis le fichier), `web/AdminController.java` (`/api/admin/**`), `service/AdminService.java`, `pages/admin/admin.component.ts`, `TableauDeBord`/`TableauDeBordService`.
- **À retenir :** règle de validation : **une nouvelle version sans `{langage}` est refusée**. Autre piège (§8.2) : si le prompt en base est obsolète en dev, publier le contenu du fichier via l'admin ou réinitialiser H2. Admin : 403 pour non-admin ; l'admin **ne soumet pas** de revues (diagramme de classes).

#### Prompt 29

````text
pour limiter les coûts de l'api :
- un quota de revues par jour et par développeur (configurable)
- un RateLimitFilter sur les appels à l'ia
- un timeout sur l'appel au llm, avec un message clair si ça dépasse
Ajoute une page paramètres où l'utilisateur voit son quota restant.
````

- **Ancrage :** `service/QuotaService.java` (défaut 50 revues/jour, `REVIA_QUOTA_JOUR`, contrôlé **avant** l'appel LLM), `security/RateLimitFilter.java` (Bucket4j : 10 analyses/min/utilisateur, 10 connexions/min/IP ; réponse 429 + `Retry-After` ; s'applique aussi à `/api/assistant/*`), timeout 30 s (`REVIA_AI_TIMEOUT`) → `LLM_TIMEOUT` 504, `pages/parametres/parametres.component.ts`, `GET /api/me` (quota du jour).
- **À retenir :** trois protections distinctes : quota journalier (coût), débit par minute (abus), timeout (disponibilité). Quota par développeur modifiable par l'admin (`PUT /api/admin/utilisateurs/{id}/quota`).

---

### 29.8 Phase 8 : tests et jeu d'essai

#### Prompt 30

````text
je dois prouver que l'ia marche bien pour mon rapport. comment tester ça ?
````

- **À retenir :** origine de la campagne de benchmark (§18, §19).

#### Prompt 31

````text
crée un jeu d'essai dans resources/jeu-essai avec des fichiers qui contiennent chacun un problème connu :
injection sql, secret en dur, ressource non fermée, xss en typescript, NullPointerException, comparaison de String avec ==,
erreur de bornes, et un fichier sain sans aucun problème.
ajoute un manifest.json avec pour chaque fichier le problème attendu.
puis un test JeuEssaiIATest qui vérifie que l'analyse trouve bien le problème attendu
(et rien de critique sur le fichier sain).
````

- **Ancrage :** `backend/src/main/resources/jeu-essai/*` + `manifest.json` (aujourd'hui **23 extraits** : 20 défauts + 3 sains, voir tableau §18), `backend/src/test/java/com/revia/ai/JeuEssaiIATest.java` (rapport généré dans `backend/target/jeu-essai-rapport.md`).
- **À retenir :** critères d'acceptation de la présentation : ≥ 85 % de détection, ≤ 1/3 de faux positif sur les fichiers sains.

#### Prompt 32

````text
le test sur le fichier sain échoue encore une fois sur 3, gemini trouve un "problème mineur" inventé.
voici la réponse : [réponse collée]
est-ce que je dois changer le prompt ou la température ?
````

- **Ancrage :** température fixée à **0,2** (`revia.ai.temperature`), règle « N'invente aucun problème » du prompt système, `modele gemini-flash-lite-latest`.
- **À retenir :** question de **choix** posée avec la réponse réelle collée ; c'est le bon réflexe pour arbitrer prompt vs paramètre. Le test automatisé en CI utilise le LLM **mocké/demo**, jamais l'API réelle (§25 point 44) ; le benchmark Gemini réel est un essai manuel documenté.

#### Prompt 33

````text
utilise le jeu d'essai pour créer un compte de démo "Sarra A." avec un historique de revues sur 30 jours,
seulement avec le fournisseur demo pour ne rien payer. ça servira pour la soutenance.
écris aussi un rapport docs/rapport-jeu-essai-gemini.md avec les résultats.
````

- **Ancrage :** `DataInitializer.creerDemo()` : compte `sarra@revia.local / Sarra2026!` (constantes `DEMO_EMAIL`, `DEMO_MDP`), créé seulement si `revia.seed-demo-data=true` (profil dev) **et** fournisseur = `demo` (sinon log « Données de démonstration ignorées… pas de coût d'API ») ; `docs/rapport-jeu-essai-gemini.md` (§19 : 19/20, 0/3 faux positifs, ~1,5 s).
- **À retenir :** pour la soutenance, utiliser la démo sans clé ; le rapport Gemini sert de preuve des chiffres.

---

### 29.9 Phase 9 : déploiement

#### Prompt 34

````text
fais moi un docker-compose avec le backend, postgres, et le front servi par spring boot (SpaController).
et un profil application-prod.yml où tous les secrets viennent des variables d'environnement.
````

- **Ancrage :** `docker-compose.yml` (services `db` postgres:16-alpine, `backend`, `frontend`), `backend/Dockerfile` (build Maven 3.9.9 / Java 17, utilisateur non-root `revia`, port 8085), `backend/src/main/resources/application-prod.yml`, `web/SpaController.java`.
- **À retenir / écart :** la version finale sert le front par **nginx** dans le conteneur `frontend` (port hôte 8080, proxy `/api/` → `backend:8085`, §13.3) ; `SpaController` reste utilisé en **mode JAR** (:8085, front embarqué dans `static/`). Les deux modes coexistent (§16). Voir aussi la lacune de `SpaController` en §28.

#### Prompt 35

````text
ajoute une github action qui compile le backend, lance les tests java et les tests angular à chaque push sur main.
````

- **Ancrage :** `.github/workflows/ci.yml` — jobs `backend` (`mvn -B verify`), `frontend` (Node 20, `ng test` headless, `ng build` prod) et `docker` (build des deux images) → §17.1.

#### Prompt 36

````text
écris le README : présentation, architecture, comment lancer en dev et avec docker,
les variables d'environnement, et les comptes de démo. et un RESUME.md court pour le prof.
````

- **Ancrage :** `README.md` (16 sections), `RESUME.md`, `.env.example` (§14.4). Ce document (§1 à §28) en est le prolongement pour un agent IA.

---

### 29.10 Phase 10 : assistant IA et page Outils (2e commit)

Commit : `65d1d36 Assistant IA et page Outils : chat en flux, rendu Markdown, transfert de code vers la revue` (le 1er commit `e8bccb4` contient l'application de revue).

#### Prompt 37

````text
je veux ajouter un chat avec l'ia dans le site, pour poser des questions sur son code.
````

#### Prompt 38

````text
précise : fais une classe AssistantIA qui réutilise le même LlmClient, le même masquage des secrets
et la même gestion des erreurs que la revue. la réponse est du markdown.
Le système doit dire que c'est RevIA, qu'il répond en français, que le code est dans des blocs ```,
qu'il ne devine jamais les ***MASQUÉ***, et qu'il refuse poliment ce qui n'est pas de la programmation.
On envoie les 20 derniers messages maximum.
````

- **Ancrage :** `ai/AssistantIA.java` (`CONSIGNES_COMMUNES`, `MAX_MESSAGES = 20`), `ai/AssistantDemo.java`, `web/AssistantController.java` (`/api/assistant/chat`, `/chat/flux`) ; texte exact des consignes en §10.5.
- **À retenir :** les 4 exigences du prompt se retrouvent **mot pour mot** dans `CONSIGNES_COMMUNES`.

#### Prompt 39

````text
le chat répond parfois au premier message de la conversation au lieu du dernier. corrige le prompt.
````

- **Ancrage :** `CONSIGNE_CHAT` (« Réponds à la dernière question de l'utilisateur… ») et la phrase finale ajoutée à chaque requête : « Réponds au dernier message de l'utilisateur. » Le dernier message doit avoir le rôle `utilisateur`.
- **À retenir :** pour les modèles à contexte long, **répéter la consigne à la fin** du message (effet de récence).

#### Prompt 40

````text
attendre 15 secondes la réponse complète c'est long. je veux que le texte s'affiche au fur et à mesure comme chatgpt.
fais le streaming avec Flux côté spring (SSE) et un service FluxIaService côté angular.
le timeout doit s'appliquer entre deux morceaux, pas sur toute la réponse.
````

- **Ancrage :** `AssistantIA.diffuser(Demande)` → `Flux<String>` avec `.timeout(Duration.ofSeconds(config.timeoutSec()))` (appliqué entre deux éléments du flux) ; routes SSE `/api/assistant/chat/flux` et `/api/assistant/outils/{outil}/flux` (événements `morceau`, `fin`, `erreur`) ; `core/flux-ia.service.ts` (classe `FluxIaService`, `fetch` + `ReadableStream`, `AbortController`), `flux-ia.service.spec.ts`. nginx : lecture proxy 120 s (§13.3).
- **À retenir :** sur `Flux`, `.timeout(d)` est un délai **entre deux signaux**, ce qui donne exactement la sémantique demandée.

#### Prompt 41

````text
la réponse s'affiche en markdown brut (avec les ** et les ```). fais un petit rendu markdown dans angular
(titres, gras, listes, blocs de code avec bouton copier) sans librairie externe, et protège contre le xss.
````

- **Ancrage :** `shared/markdown.ts` (`markdownVersHtml` : échappement **avant** mise en forme), `shared/reponse-ia.component.ts`, `shared/markdown.spec.ts`.
- **À retenir :** la sortie d'un LLM est une **entrée non fiable** : on échappe d'abord, on formate ensuite.

#### Prompt 42

````text
ajoute une page Outils avec 6 outils qui prennent du code en entrée, chacun avec sa propre consigne :
1. génération de tests (le bon framework selon le langage)
2. documentation automatique (sans changer la logique)
3. explication pour un débutant (avec les numéros de ligne)
4. message de commit (Conventional Commits) + description de PR
5. refactoring (même comportement, code complet)
6. conversion vers un autre langage (choisir le langage cible)
Limite à 500 lignes. Fais un enum Outil dans AssistantIA.
````

- **Ancrage :** `AssistantIA.Outil` (`tests`, `documentation`, `explication`, `commit`, `refactoring`, `conversion`), `MAX_LIGNES = 500`, `POST /api/assistant/outils/{outil}` (+ `/flux`, paramètre `cible` pour la conversion), `pages/outils/outils.component.ts`, route `/outils` (§10.3).

#### Prompt 43

````text
quelques retours après test :
- tests : il génère du JUnit 4, je veux JUnit 5 + AssertJ, et il oublie les cas d'erreur
- documentation : il modifie le code en plus de le commenter
- refactoring : il renvoie "... reste inchangé ..." au lieu du code complet
- conversion : c'est traduit mot à mot, pas idiomatique
corrige les consignes de chaque outil pour ces problèmes.
````

- **Ancrage :** consignes de chaque `Outil` (§10.5) : « JUnit 5 + AssertJ », « Couvre le cas nominal, les cas limites et les cas d'erreur », « sans modifier la logique », « Donne le code complet refactoré dans un seul bloc », « de façon idiomatique ».
- **À retenir :** formule efficace = **un défaut observé → une consigne positive précise** pour chaque outil.

#### Prompt 44

````text
dernier truc : depuis la page outils ou le chat, je veux un bouton "Envoyer en revue"
qui ouvre la page revue avec le code déjà collé. fais un TransfertCodeService.
puis écris les tests (AssistantIATest, AssistantApiTest, markdown.spec.ts, flux-ia.service.spec.ts).
````

- **Ancrage :** `core/transfert-code.service.ts` (`deposer` / `prendre`) ; tests `AssistantIATest` (9), `AssistantApiTest` (3, MockMvc + SSE), `markdown.spec.ts`, `flux-ia.service.spec.ts`.
- **⚠ Écart constaté dans le code :** le prompt demande **outils/chat → revue**, mais le code livré fait **revue → assistant / outils** (boutons « 🧪 Outils IA » et « 💬 Demander à l'assistant » dans `revue.component.ts`, `ouvrir()` → `transfert.deposer(code, langage)` ; `assistant.component.ts` et `outils.component.ts` appellent `prendre()`). Le commit s'intitule pourtant « transfert de code vers la revue ». Voir §31.

---

### 29.11 Annexe de l'historique : prompt système de revue (version finale)

Le texte final de `revue-system.st` est celui du **§9**. Il résulte du prompt 15 complété par les ajustements des prompts 17 (ligne exacte), 20 (règle `***MASQUÉ***` → secret en dur, OWASP A07:2021), 21 (français correct) et par les ajouts `{mode}` (Sécurité + qualité / Sécurité seule / Qualité seule) et `owasp`.

Correspondance règle du prompt → prompt d'origine :

| Ligne / règle de `revue-system.st` | Prompt |
|------------------------------------|--------|
| Rôle « développeur senior… OWASP Top 10 » | 15 |
| « Les lignes sont numérotées » | 15 |
| « N'invente aucun problème : liste vide si code correct » | 15 (renforcée par 17 et 32) |
| « Réponds uniquement en JSON, sans texte autour » | 15 (+ nettoyage Java au 16) |
| Format JSON (`score`, `resume`, `problemes[]`, `correction{codeAvant,codeApres,justification}`) | 15 (champs de l'entité du 6) |
| Français correct, sans mot d'une autre langue | 21 |
| Traitement de `***MASQUÉ***` | 20 |
| « Le numéro de "ligne" doit être exactement l'un des numéros affichés » | 17 |
| `{langage}` obligatoire (sinon version refusée) | 28 |
| Périmètre `{mode}` | extension ultérieure (API `mode` sur `POST /api/revues`) |
| Champ `owasp` | extension ultérieure (affiché sur la page correction) |

---

## 30. Matrice prompt → code → test

Pour savoir **quel test relancer** avant de toucher à une zone issue d'un prompt.

| Prompts | Zone | Fichiers principaux | Tests à lancer |
|---------|------|--------------------|----------------|
| 5–7 | Domaine, repositories, service revue | `domain/*`, `repository/*`, `service/RevueService.java` | `RevueTest` (6), `ApiIntegrationTest` (7) |
| 8 | Scan des repositories JPA | `repository/ProblemeRepository.java` | `ApiIntegrationTest` (démarrage contexte) |
| 9–11 | Auth JWT, admin initial, comptes démo | `security/*`, `AuthController`, `AuthService`, `DataInitializer` | `ApiIntegrationTest`, `auth.service.spec.ts` |
| 12–13 | Client LLM, fournisseurs, demo | `LlmClient`, `SpringAiLlmClient`, `LlmConfig`, `AnalyseurStatique`, `RedacteurRevue` | `LlmConfigTest` (2), `RedacteurRevueTest` (5) |
| 14–17 | Prompt de revue, parsing, validation lignes | `revue-system.st`, `ReponseIA`, `AnalyseurIA`, `PreparateurCode` | `AnalyseurIATest` (10), `JeuEssaiIATest` (1) |
| 18–21 | Masquage et consignes associées | `MasqueurDonnees`, `revue-system.st` | `MasqueurDonneesTest` (7), `AnalyseurIATest` (masquage avant envoi), `JeuEssaiIATest` |
| 22 | Analyse statique (mode demo) | `AnalyseurStatique` | `JeuEssaiIATest`, `RedacteurRevueTest` |
| 23–25 | Pages et design Angular | `pages/*`, `shared/*`, `styles.scss` | `ng test` (14), `ng build --configuration production`, `score-ring.component.spec.ts`, `diff.spec.ts` |
| 26 | Erreurs API, 401, notifications | `error/*`, `core/http.ts`, `notif.service.ts` | `ErreursApiTest` (4), `http.spec.ts` |
| 27–28 | Admin, prompts versionnés | `AdminController`, `AdminService`, `PromptService`, `PromptSysteme` | `ApiIntegrationTest` (accès admin), manuel parcours §21 étape 13 |
| 29 | Quotas, rate limit, timeout | `QuotaService`, `RateLimitFilter`, `AnalyseurIA.executer` | `ApiIntegrationTest` (quota), `ErreursApiTest` (Bucket4j), `AnalyseurIATest` (timeout 504, 429→503) |
| 30–33 | Jeu d'essai, démo, rapport | `jeu-essai/*`, `DataInitializer.creerDemo`, `docs/rapport-jeu-essai-gemini.md` | `JeuEssaiIATest` |
| 34–36 | Docker, prod, CI, docs | `docker-compose.yml`, `application-prod.yml`, `ci.yml`, `README.md`, `RESUME.md` | `docker compose up -d --build` + connexion :8080, CI verte |
| 37–39 | Assistant chat | `AssistantIA`, `AssistantController`, `AssistantDemo` | `AssistantIATest` (9), `AssistantApiTest` (3) |
| 40 | Streaming SSE | `AssistantIA.diffuser`, `flux-ia.service.ts` | `AssistantApiTest` (SSE), `flux-ia.service.spec.ts` |
| 41 | Rendu Markdown sûr | `shared/markdown.ts`, `reponse-ia.component.ts` | `markdown.spec.ts` |
| 42–43 | Outils IA | `AssistantIA.Outil`, `pages/outils` | `AssistantIATest`, `AssistantApiTest` (outil inconnu `magie` → introuvable) |
| 44 | Transfert de code | `transfert-code.service.ts`, `revue.component.ts`, `assistant.component.ts`, `outils.component.ts` | manuel : revue → bouton Outils IA / Assistant, vérifier code pré-rempli |

**Totaux de référence :** backend 54 tests (7+10+6+1+2+5+7+4+9+3), frontend 14 (§20) — relever les chiffres réels à l'exécution.

---

## 31. Leçons de prompting et écarts historique / code

### 31.1 Leçons tirées des 44 prompts (à appliquer par l'agent)

1. **Contexte précis dès le départ** : langage, versions, noms de classes (prompts 2→3→6 vs 1 et 5).
2. **Imposer le format de sortie** dès qu'un programme lit la réponse (JSON strict, champs de l'entité) — prompts 14→15.
3. **Coller l'erreur réelle et la trace complète** (prompts 8, 16, 32) plutôt que décrire le symptôme.
4. **Défense en profondeur** : chaque comportement LLM indésirable est traité **dans le prompt ET dans le code** (prompts 16–17 : JSON nettoyé/validé, lignes inexistantes ignorées).
5. **Un effet de bord = une nouvelle consigne** : le masquage (prompt 19) a dégradé la détection des secrets, corrigé par la règle `***MASQUÉ***` (prompt 20).
6. **Donner un contre-exemple de formulation** (prompt 21 : « l'externaliser » → « le déplacer dans une variable d'environnement »).
7. **Un défaut observé → une consigne positive précise** pour chaque outil (prompt 43).
8. **Répéter la consigne clé en fin de message** pour la conversation (prompt 39).
9. **Séparer ce que fait le LLM de ce que fait le code** : score affiché calculé par `Revue.calculerScore()` côté serveur, jamais le score brut du LLM ; contrôle de longueur, masquage, quotas et rate limit hors prompt.
10. **Toujours un mode sans coût** (`demo`) pour les tests, la CI et la soutenance (prompts 13, 33).
11. **Mesurer** : jeu d'essai + manifeste + test automatique + rapport (prompts 31–33) avant de toucher au prompt.
12. **Traiter la sortie du LLM comme non fiable** : validation côté back (prompt 16), échappement avant rendu côté front (prompt 41).

### 31.2 Écarts constatés entre l'historique et le code actuel

| Sujet | Historique | Code actuel | Conséquence pour l'agent |
|-------|-----------|-------------|--------------------------|
| Sens du transfert de code (prompt 44) | « Envoyer en revue » depuis outils / chat vers la revue | **Revue → Assistant / Outils** (`revue.component.ts` `ouvrir()`, `prendre()` dans assistant/outils) | Ne pas supposer qu'un bouton « Envoyer en revue » existe. Si on veut le sens inverse, il faut ajouter un `deposer()` dans assistant/outils, un `prendre()` dans `revue.component.ts`, et un test |
| Service du front (prompt 34) | Front servi par Spring Boot (`SpaController`) | Deux modes : JAR (`SpaController`, :8085) et Docker (nginx, :8080) | Documenter / tester les deux ; `SpaController` ne couvre pas `/assistant` ni `/outils` (§28) |
| Nom du service de transfert (prompt 44) | `TransfertCodeService` | Fichier `core/transfert-code.service.ts`, classe `TransfertCodeService` ✔ | Aucun |
| Service de flux (prompt 40) | `FluxIaService` | `core/flux-ia.service.ts`, classe `FluxIaService` ✔ | Aucun |
| Compte admin dev | Mot de passe par défaut (non précisé) | `Admin2026!` (dev) ; en prod aucun compte sans `REVIA_ADMIN_PASSWORD` | Voir §1 et `DataInitializer` |
| Docker Compose local | Secrets par variables d'environnement (prompt 34) | `POSTGRES_PASSWORD` et `REVIA_ADMIN_PASSWORD` en dur à `revia123` dans le compose local, `DATABASE_PASSWORD` lu depuis `.env` | Cohérence obligatoire entre `DATABASE_PASSWORD` et le mot de passe Postgres, sinon 502 (§28, §24) |
| Nombre de tests | 42 (README) | 54 (avec Assistant) | Annoncer le chiffre réel à l'exécution (§20) |
| Historique complet des essais Gemini | Prompt 32 : « [réponse collée] » non conservée | Seul le résultat final est dans `docs/rapport-jeu-essai-gemini.md` | Pour reproduire, relancer le jeu d'essai avec une clé et consigner la réponse |

### 31.3 Dépannage issu de la mise en route Docker (observé)

| Symptôme | Cause | Action |
|----------|-------|--------|
| `error while interpolating services.backend.environment.REVIA_JWT_SECRET: required variable … missing` | Pas de fichier `.env` (seul `.env.example` existe) | `copy .env.example .env` puis renseigner `REVIA_JWT_SECRET` (≥ 32 car.) |
| `The "DATABASE_PASSWORD" variable is not set` | Idem | Renseigner `DATABASE_PASSWORD` dans `.env` |
| `Conflict. The container name "/revia_pe-db-1" is already in use` | Ancien conteneur du même projet compose | `docker rm -f revia_pe-db-1 revia_pe-backend-1 revia_pe-frontend-1` puis `docker compose up -d` |
| nginx **502 Bad Gateway** sur `/api/auth/login` | Backend en échec : `FATAL: password authentication failed for user "revia"` (mot de passe `.env` ≠ mot de passe Postgres) | Aligner `DATABASE_PASSWORD` sur `POSTGRES_PASSWORD` (le volume Postgres conserve l'ancien mot de passe : supprimer le volume `revia_pe_revia-db` si l'on veut en changer) |
| 401 « E-mail ou mot de passe incorrect. » avec le mot de passe de `.env` | Le compose fixe `REVIA_ADMIN_PASSWORD: revia123` | Se connecter avec `admin@revia.local / revia123` en Docker local |

---

## Definition of done (phrase finale)

RevIA est **prêt** lorsqu’un relecteur peut exécuter `scripts\build.ps1` et `scripts\start.ps1`, se connecter, obtenir une revue validée (demo ou Gemini) avec masquage et JSON contrôlés, passer **tous les tests** backend et frontend, valider la checklist **§25** (au minimum A–G et parcours §21), et déployer optionnellement Docker ou Render/Vercel sans fuite de clé — en restant conforme à **RESUME.md**, **README.md** et **docs/rapport-jeu-essai-gemini.md**.

---

*Fin du document — `docs/PROMPT_AGENT_IA_DETAILLE.md`*
