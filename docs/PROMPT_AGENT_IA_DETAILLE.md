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
> - `C:\Users\Mustapha.Sifaoui\Downloads\Revia_PE\docs\historique-prompts.md` — historique réel des 44 prompts de construction (intégré **dans chaque section** sous « Prompts de construction » ; le fichier source reste la référence verbatim)

**Règle de priorité en cas de conflit :** tests automatisés (`backend/src/test`, `frontend/**/*.spec.ts`) > `README.md` > `RESUME.md` > ce prompt.

**Convention « Prompts de construction ».** Chaque section technique se termine par un bloc qui contient :
1. un **prompt consolidé** au même format que le bloc §1 (CONTEXTE / MISSION / CONTRAINTES / VÉRIFICATION), réutilisable tel quel par un agent pour refaire l'étape ;
2. les **échanges réels** avec l'IA (aller-retour : prompt → réponse → correction → nouvelle réponse), numérotés comme dans `docs/historique-prompts.md` (« Prompt N »).

Chaque réponse de l'IA porte une étiquette : **[historique]** = relevée dans `historique-prompts.md` ; **[reconstitué]** = l'historique ne l'a pas conservée, elle est résumée à partir du code livré (jamais recopiée mot pour mot).

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
Chaque section technique de docs/PROMPT_AGENT_IA_DETAILLE.md se termine par un bloc « Prompts de construction » : prompt consolidé au même format que ce bloc (CONTEXTE / MISSION / CONTRAINTES / VÉRIFICATION), puis les échanges réels avec l'IA (prompt, réponse, correction, nouvelle réponse), tirés de docs/historique-prompts.md. Chaque règle du prompt système (revue-system.st, AssistantIA) est née d'un échec réel : ne pas la supprimer sans relancer JeuEssaiIATest et AssistantIATest.
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


### 2.4 Prompts de construction — idée et cadrage (prompts 1 et 2)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur full-stack senior.

CONTEXTE
Projet de cours en IA : une application de revue de code assistée par IA. Le sujet impose Angular (front) et Spring Boot (back).

MISSION
Proposer les étapes de démarrage d'un vrai projet front + back, avec pour chaque étape son livrable.

CONTRAINTES
- Aucun appel à un LLM depuis le navigateur ; la clé d'API reste sur le serveur.
- Versions imposées : Angular 17, Spring Boot 3 (Java 17).

VÉRIFICATION
Chaque étape se valide par une commande (build, test, démarrage).
```

**Échanges réels**

*Prompt 1*
````text
salut, je dois faire un projet en IA pour mon cours. je veux faire un site qui fait la revue de code avec l'ia.
tu peux me faire ça ?
````
- **Réponse IA [historique] :** une seule page HTML avec un `textarea` et un appel direct à une API depuis le navigateur, clé visible dans le code. Pas utilisable.
- **Retour :** rejet, car il manque un backend (d'où l'interdiction « pas de LLM depuis le navigateur », §1 et §2.3).

*Prompt 2*
````text
non je veux un vrai projet avec un frontend et un backend. le prof veut angular et spring boot.
c'est quoi les étapes pour commencer ?
````
- **Réponse IA [historique] :** une liste d'étapes générale, sans structure de dossiers ni versions.
- **Retour :** il faut imposer les versions et demander la structure de dossiers (prompts 3 et 4, §4.3).
- **À retenir :** sans versions ni contraintes dans le prompt, le LLM reste générique.

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
| Back-end | 42/42 dans README ; **61/61** possible après ajout Assistant (voir §20) | `cd backend && mvn test` |
| Front-end | 14/14 | `cd frontend && npx ng test --watch=false --browsers=ChromeHeadless` |
| Jeu d’essai demo | 20/20 défauts, 0/3 faux positifs sains | `JeuEssaiIATest` |
| Jeu d’essai Gemini | 19/20 (95 %), 0/3 faux positifs | `docs/rapport-jeu-essai-gemini.md` |
| CI | Verte | push + GitHub Actions |
| Parcours navigateur | 0 erreur console | README §4.1 |
| Prod PostgreSQL | Flyway + revue OK | README §11 |

**L’agent doit toujours relever le nombre exact de tests** dans la sortie Maven / Karma, pas se fier uniquement au chiffre 42 du README.


### 3.6 Prompts de construction — documentation (prompt 36)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur full-stack senior chargé de la documentation de RevIA.

CONTEXTE
Application terminée (Angular 17 + Spring Boot 3 + Spring AI + PostgreSQL). Le prof lit d'abord un résumé, l'équipe consulte ensuite le manuel.

MISSION
Écrire README.md (manuel technique) et RESUME.md (synthèse courte pour le prof).

CONTENU OBLIGATOIRE
README : présentation, architecture, lancement en dev et avec Docker, variables d'environnement, comptes de démo, tests, dépannage.
RESUME : phrase produit, équipe, LLM retenu, pipeline IA, résultats de tests, conformité à la présentation.

CONTRAINTES
- Chiffres issus des exécutions réelles (tests, benchmark Gemini), pas inventés.
- Aucun secret dans les exemples.

VÉRIFICATION
Un relecteur peut suivre le README du clone au premier écran de connexion.
```

**Échanges réels**

*Prompt 36*
````text
écris le README : présentation, architecture, comment lancer en dev et avec docker,
les variables d'environnement, et les comptes de démo. et un RESUME.md court pour le prof.
````
- **Réponse IA [reconstitué] :** `README.md` en 16 sections (stack, lancement, API, sécurité, tests, déploiement, dépannage §16) et `RESUME.md` synthétique (tableaux de stack, pipeline en 5 étapes, résultats de tests, conformité).
- **Retour :** les chiffres de tests (42 dans le README) n'ont pas été remis à jour après l'ajout de l'assistant (54 réels), voir l'écart en §28.

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


### 4.3 Prompts de construction — structure du projet et dépendances (prompts 3 et 4)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur full-stack senior.

CONTEXTE
Projet RevIA : front Angular 17, back Spring Boot 3 (Java 17). PostgreSQL en production, H2 en développement.

MISSION
1) Donner l'arborescence du dépôt (backend/, frontend/) et expliquer chaque dossier en une ligne.
2) Fournir le pom.xml du backend : web, jpa, h2 (dev), postgresql (prod), security, validation.
3) Donner la commande de création du projet Angular avec routing.

CONTRAINTES
- Versions exactes de la section 4 (Spring Boot 3.5.x, Angular 17.3.x).
- Packages Java en com.revia ; nommage français du domaine.

VÉRIFICATION
mvn -B package et npm ci && ng build passent sur le squelette.
```

**Échanges réels**

*Prompt 3*
````text
ok. donne moi la structure des dossiers du projet avec angular 17 pour le front et spring boot 3 (java 17) pour le back.
le projet s'appelle RevIA. explique moi chaque dossier en 1 ligne.
````
- **Réponse IA [reconstitué] :** arborescence `backend/` (couches `web`, `service`, `ai`, `domain`, `repository`, `security`, `error`, `config`) et `frontend/src/app/{core,layout,shared,pages}` : voir §6.3 et §23.

*Prompt 4*
````text
donne moi le pom.xml du backend avec les dépendances : web, jpa, h2 pour le dev, postgresql pour la prod, security, validation.
et la commande pour créer le projet angular avec le routing.
````
- **Réponse IA [reconstitué] :** `pom.xml` avec les starters demandés, puis `ng new frontend --routing`.
- **Retour (compléments ajoutés au fil du projet) :** `spring-ai-client-chat`, `spring-ai-openai`, `spring-ai-anthropic`, `spring-retry`, jjwt 0.12.6, `bucket4j_jdk17-core` 8.14.0, Flyway 11.7.2 + `flyway-database-postgresql`, actuator.
- **À retenir :** H2 en dev et PostgreSQL en prod sont l'origine des profils `dev` / `prod` (§14.1).

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


### 7.1 Prompts de construction — entités, repositories et service (prompts 5 à 8)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur backend senior (Spring Boot 3, JPA).

CONTEXTE
RevIA (package com.revia). Domaine en français.

MISSION
Créer les entités JPA, les repositories Spring Data et RevueService.

ENTITÉS
- Utilisateur (abstract, SINGLE_TABLE) avec 2 sous-types : Developpeur et Administrateur.
- Revue : titre, langage, code, score /100, résumé, date, liste de problèmes.
- Probleme : ligne, categorie (securite, bug, bonne_pratique, performance), gravite (critique, majeure, mineure, info), titre, explication, correction.
- Correction : codeAvant, codeApres, justification.
- Enums Categorie et Gravite.

SERVICE
RevueService : créer une revue, lister MES revues, voir une revue, supprimer. L'analyse peut renvoyer un faux résultat tant que l'IA n'est pas branchée.

CONTRAINTES
- Toutes les requêtes sont filtrées par auteur (la revue d'un autre = 404).
- Score serveur : 100 - 20×critiques - 6×majeures - 2×mineures, borné [0,100], faux positifs exclus.
- Schéma géré par Flyway (V1__schema_initial.sql), ddl-auto: validate.

VÉRIFICATION
Le contexte Spring démarre ; RevueTest (6 tests) et ApiIntegrationTest passent.
```

**Échanges réels**

*Prompt 5*
````text
fais moi les entités : un utilisateur, une revue de code, et les problèmes trouvés dans la revue.
````
- **Réponse IA [historique] :** entités en anglais (`User`, `Review`, `Issue`), sans gravité ni catégorie.
- **Retour :** nommage et attributs à préciser (prompt 6).

*Prompt 6*
````text
refais les entités en français stp (Utilisateur, Revue, Probleme).
Utilisateur doit être abstrait avec 2 types : Developpeur et Administrateur.
Un Probleme a : ligne, categorie (securite, bug, bonne_pratique, performance), gravite (critique, majeure, mineure, info),
titre, explication, et une correction (code avant, code après, justification).
Une Revue a : titre, langage, code, score sur 100, résumé, date, et une liste de problèmes.
Utilise des enums pour la catégorie et la gravité.
````
- **Réponse IA [reconstitué] :** `Utilisateur` (abstract), `Developpeur`, `Administrateur`, `Revue`, `Probleme`, `Correction`, `Categorie`, `Gravite` et le schéma `V1__schema_initial.sql`.
- **À retenir :** les noms de champs et les valeurs d'enum de ce prompt sont le **contrat JSON** du LLM et du front (`models.ts`, `libelles.ts`) : ne pas les renommer sans mettre à jour `ReponseIA`, le front et les tests.

*Prompt 7*
````text
maintenant les repositories et un service RevueService avec créer une revue, lister mes revues, voir une revue, supprimer.
pour l'instant l'analyse peut renvoyer un faux résultat, on branchera l'ia après.
````
- **Réponse IA [reconstitué] :** `UtilisateurRepository`, `RevueRepository`, `ProblemeRepository`, `RevueService` avec une analyse factice.
- **À retenir :** « lister **mes** revues » est l'origine de l'isolation par auteur.

*Prompt 8 (erreur collée)*
````text
j'ai cette erreur au démarrage :

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'revueService'
... No qualifying bean of type 'com.revia.repository.ProblemeRepository' available

c'est quoi le problème ?
````
- **Réponse IA [reconstitué] :** le bean `ProblemeRepository` n'est pas détecté : l'interface doit étendre `JpaRepository` et se trouver dans un package scanné (`com.revia.repository`).
- **À retenir :** coller la **trace complète** donne un diagnostic précis ; c'est le réflexe réutilisé aux prompts 16, 20, 26 et 32.

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


### 8.3 Prompts de construction — client LLM, validation, masquage, analyse statique (prompts 12, 13, 16, 18, 19, 22)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur backend senior (Spring Boot 3, Spring AI 1.1).

CONTEXTE
RevIA envoie du code à un LLM (Gemini gratuit par défaut) et doit obtenir un JSON exploitable. Le code des utilisateurs peut contenir des secrets qui ne doivent jamais partir chez le fournisseur.

MISSION
1) LlmClient : interface rediger(systeme, message) + SpringAiLlmClient ; fournisseur et modèle configurables dans application.yml ; fournisseur « demo » sans clé ni coût.
2) ReponseIA : nettoyer la réponse (retirer les balises ```json, garder du premier { au dernier }), valider les champs (Bean Validation), ignorer les problèmes dont la ligne n'existe pas, retenter une fois si le JSON est invalide.
3) MasqueurDonnees : remplacer par ***MASQUÉ*** mots de passe, clés d'API, jetons, chaînes de connexion, e-mails, clés privées ; renvoyer le code masqué + le nombre de masquages ; conserver le nombre de lignes.
4) AnalyseurStatique : détecter sans IA les cas évidents (mot de passe en dur, concaténation SQL, System.out, catch vide…) pour le mode demo et pour compléter le LLM.

CONTRAINTES
- La clé d'API reste côté serveur (REVIA_AI_API_KEY). Température 0,2, délai 30 s.
- Erreur claire en français pour l'utilisateur, jamais de stacktrace (ErrorCode LLM_REPONSE_INVALIDE, 502).
- Le masquage s'applique AVANT tout envoi au LLM.

VÉRIFICATION
MasqueurDonneesTest (7), AnalyseurIATest (10), LlmConfigTest (2), RedacteurRevueTest (5), JeuEssaiIATest (1) passent.
```

**Échanges réels**

*Prompt 12*
````text
comment j'appelle gemini depuis spring boot pour analyser le code ?
````
- **Réponse IA [historique] :** un appel HTTP brut avec `RestTemplate`, la clé dans l'URL, aucune gestion d'erreur.
- **Retour :** rejeté, d'où le choix de Spring AI et d'une interface (prompt 13).

*Prompt 13*
````text
je préfère utiliser Spring AI. fais une interface LlmClient avec une méthode rediger(systeme, message)
et une implémentation SpringAiLlmClient. Le fournisseur et le modèle doivent être configurables dans application.yml.
Ajoute aussi un fournisseur "demo" qui renvoie des réponses fixes, pour tester sans clé et sans payer.
````
- **Réponse IA [reconstitué] :** `LlmClient`, `SpringAiLlmClient`, `LlmConfig` (choix du fournisseur : gemini, groq, ollama, openai, anthropic, demo), bloc `revia.ai` d'`application.yml` (§14.3).
- **À retenir :** l'interface unique permet de changer de fournisseur sans toucher au code métier ; l'assistant (§10) la réutilise.

*Prompt 16 (erreur collée)*
````text
des fois gemini renvoie le json entouré de ```json ... ``` et ça plante :

com.fasterxml.jackson.core.JsonParseException: Unexpected character ('`' (code 96))

fais une classe ReponseIA qui nettoie la réponse (enlève les balises, prend du premier { au dernier })
et qui valide les champs. si le json est invalide, renvoie une erreur claire à l'utilisateur, pas une stacktrace.
````
- **Réponse IA [reconstitué] :** `ReponseIA` (nettoyage, désérialisation, validation), un retry (`json-retries: 1`) dans `AnalyseurIA`, erreur `LLM_REPONSE_INVALIDE` (502) : « Analyse incomplète : la réponse de l'IA n'a pas pu être validée. Réessayez. »
- **À retenir :** ne jamais faire confiance au format de sortie d'un LLM : nettoyer, valider, retenter une fois, puis erreur claire.

*Prompt 18*
````text
je ne veux pas que les mots de passe ou les clés api qui sont dans le code des utilisateurs partent chez google.
comment faire ?
````
- **Réponse IA [reconstitué] :** proposer un masquage côté serveur avant l'envoi, par expressions régulières, avec un compteur de masquages.

*Prompt 19*
````text
fais une classe MasqueurDonnees qui remplace par ***MASQUÉ*** avant l'envoi :
mots de passe, clés api, tokens, chaînes de connexion, emails, clés privées.
elle renvoie le code masqué + le nombre de masquages (pour l'afficher dans l'interface).
écris aussi les tests JUnit avec des exemples.
````
- **Réponse IA [reconstitué] :** `MasqueurDonnees` (record `Resultat(code, nbMasquages)`) et `MasqueurDonneesTest` (7 tests : secrets, e-mails, JDBC, idempotence, nombre de lignes conservé).
- **Retour :** le masquage dégrade la détection des secrets en dur : correction dans le prompt système (prompt 20, §9.1).

*Prompt 22*
````text
ajoute aussi un AnalyseurStatique en java qui détecte les cas évidents sans IA
(mot de passe en dur, concaténation SQL, System.out, catch vide...) pour compléter la réponse de gemini
et éviter qu'il rate des choses importantes.
````
- **Réponse IA [reconstitué] :** `AnalyseurStatique` (environ 25 règles), utilisé par le mode demo avec `RedacteurRevue` ; le jeu d'essai demo atteint 20/20 défauts et 0/3 faux positifs (§3.5).
- **À retenir :** le LLM n'est pas la seule source de détection ; toute règle demo nouvelle va dans `AnalyseurStatique` avec un test (§27).

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


### 9.1 Prompts de construction — le prompt système de revue, version après version (prompts 14, 15, 17, 20, 21)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur spécialisé en prompt engineering pour un module de revue de code.

CONTEXTE
RevIA envoie à Gemini du code dont les lignes sont numérotées (NNN | ligne) et attend un JSON strict (score, resume, problemes[]) que Java sait parser.

MISSION
Écrire et maintenir backend/src/main/resources/prompts/revue-system.st avec les placeholders {langage} et {mode}.

RÈGLES DU PROMPT (chacune est née d'un échec réel)
1) Rôle : développeur senior, revue de code et sécurité applicative (OWASP Top 10).
2) N'invente aucun problème : liste vide si le code est correct.
3) Réponse en JSON uniquement, sans texte autour, au format exact de l'entité Probleme.
4) La « ligne » doit être exactement l'un des numéros affichés.
5) ***MASQUÉ*** n'est pas une erreur de syntaxe ; mais si c'est la valeur d'un mot de passe, d'une clé ou d'un secret écrit dans le code, le signaler (securite, critique, OWASP A07:2021) et proposer une variable d'environnement.
6) Français correct, sans mot d'une autre langue ni d'un autre alphabet (contre-exemple : « le déplacer dans une variable d'environnement » plutôt que « l'externaliser »).

VÉRIFICATION
JeuEssaiIATest ; benchmark Gemini 19/20, 0/3 faux positifs. Après modification : publier/activer une version en admin (le prompt actif est en base) ou réinitialiser H2 en dev.
```

**Échanges réels**

*Prompt 14*
````text
c'est quoi le prompt que je dois envoyer à gemini pour qu'il fasse la revue ?
````
- **Réponse IA [historique] :** « Analyse ce code et donne les erreurs. » Réponses en texte libre, impossibles à afficher proprement.
- **Retour :** l'humain écrit lui-même le prompt (prompt 15).

*Prompt 15 — version v1 du prompt système*
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
- **Réponse IA [reconstitué] :** création de `revue-system.st` avec le bloc de format JSON (`score`, `resume`, `problemes[]` avec `correction{codeAvant, codeApres, justification}`), et `PreparateurCode` qui préfixe chaque ligne par son numéro (chunks de 300 lignes).
- **À retenir :** c'est le prompt écrit par l'humain qui a fonctionné : rôle, périmètre, règle anti-invention, sortie JSON stricte, lignes numérotées.

*Prompt 17 — v2 : ligne exacte et faux positifs*
````text
problème : sur un code qui est correct, gemini invente quand même 2 ou 3 problèmes mineurs.
et parfois le numéro de ligne n'existe pas dans le code.
ajoute dans le prompt que la ligne doit être exactement un des numéros affichés,
et côté java ignore les problèmes dont la ligne est hors de l'extrait.
````
- **Réponse IA [reconstitué] :** ajout de « Le numéro de "ligne" doit être exactement l'un des numéros affichés devant le code » et filtrage des lignes hors extrait dans `ReponseIA` (couvert par `AnalyseurIATest`).
- **À retenir :** défense en profondeur : consigne dans le prompt **et** contrôle côté code.

*Prompt 20 — v3 : règle `***MASQUÉ***` (erreur de logique collée)*
````text
depuis le masquage, gemini dit que ***MASQUÉ*** est une erreur de syntaxe.
et pire : sur mon fichier de test avec PASSWORD = "admin123" il ne signale plus le mot de passe en dur,
puisqu'il ne voit plus la valeur.
ajoute une consigne dans le prompt pour ça : ne pas traiter ***MASQUÉ*** comme une erreur,
mais si c'est la valeur d'un mot de passe ou d'un secret dans le code, le signaler en critique (OWASP A07:2021).
````
- **Réponse IA [reconstitué] :** ajout du paragraphe « Les valeurs ***MASQUÉ*** ont été masquées avant l'envoi… (catégorie securite, gravité critique, titre « Mot de passe en dur » ou « Secret en dur dans le code », OWASP A07:2021) ».
- **Mesure (rapport Gemini) :** sans cette règle 17/20 détections ; avec elle 19/20 (§19).

*Prompt 21 — v4 : langue*
````text
les explications de gemini mélangent français et anglais ("hardcoded", "externaliser", "fallback").
ajoute au prompt qu'il doit écrire en français correct sans mot d'une autre langue.
````
- **Réponse IA [reconstitué] :** ajout de « Rédige tous les textes en français correct, sans aucun mot d'une autre langue ni d'un autre alphabet (pour un secret, écris « le déplacer dans une variable d'environnement » plutôt que « l'externaliser ») ».
- **À retenir :** un contre-exemple de formulation est plus efficace qu'une règle abstraite.

**Version finale :** le texte intégral est celui du bloc ci-dessus (§9), complété ensuite par `{mode}` (Sécurité + qualité / Sécurité seule / Qualité seule) et par le champ `owasp`.

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

### 10.5 bis Barre de contexte et compactage de la conversation

- **Barre « Contexte utilisé »** (écran Assistant IA) : estimation ≈ 4 caractères par token (messages + code joint + saisie en cours) rapportée au budget `AssistantIA.CONTEXTE_MAX_TOKENS` = 12 000. Vert sous 70 %, orange dès 70 %, rouge dès 90 % ; au-delà de 100 %, avertissement (le serveur n'envoie de toute façon que les 20 derniers messages).
- **Bouton « Compacter »** : `POST /api/assistant/compacter` → `AssistantIA.preparerCompactage` (consigne « Outil : compactage de la conversation » : objectif, décisions, erreurs, questions ouvertes, code important, 250 mots maximum). Les messages passent par le masquage des secrets. Le front remplace tout l'historique par un seul message « 📌 Résumé de la conversation précédente » (mis en évidence, conservé en `sessionStorage`).
- **Mode démo :** `AssistantDemo.compactage` produit un résumé par gabarit (questions posées + début de la dernière réponse).
- **Quota :** un compactage compte comme une requête assistant ; la jauge du menu latéral est rafraîchie après chaque réponse ou compactage.

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


### 10.7 Prompts de construction — assistant, streaming, Markdown, outils, transfert (prompts 37 à 44)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur full-stack senior (Spring Boot 3 / Angular 17).

CONTEXTE
RevIA propose déjà la revue de code. On ajoute un assistant IA (chat) et une boîte à outils, en réutilisant LlmClient, MasqueurDonnees et la gestion d'erreurs de la revue.

MISSION
1) AssistantIA : chat en Markdown ; dire qu'il s'appelle RevIA, répondre en français, mettre le code dans des blocs ```, ne jamais deviner les ***MASQUÉ***, refuser poliment ce qui n'est pas de la programmation ; 20 derniers messages maximum.
2) Streaming : Flux côté Spring (SSE : événements morceau, fin, erreur) et FluxIaService côté Angular ; timeout entre deux morceaux, pas sur toute la réponse.
3) Rendu Markdown dans Angular sans librairie externe (titres, gras, listes, blocs de code avec bouton copier), protégé contre le XSS.
4) Page Outils, 6 outils (enum Outil) : tests, documentation, explication (numéros de ligne), commit + PR (Conventional Commits), refactoring (code complet), conversion (langage cible). 500 lignes maximum.
5) TransfertCodeService : passer le code d'une page à l'autre sans l'envoyer au serveur.

CONTRAINTES
- Mêmes garanties que la revue : masquage avant envoi, timeout, ErrorCode, quotas et rate limit sur /api/assistant/*.
- Le dernier message de la conversation doit être de l'utilisateur ; la consigne « réponds au dernier message » est répétée en fin de requête.

VÉRIFICATION
AssistantIATest (9), AssistantApiTest (3), markdown.spec.ts, flux-ia.service.spec.ts.
```

**Échanges réels**

*Prompt 37*
````text
je veux ajouter un chat avec l'ia dans le site, pour poser des questions sur son code.
````
- **Réponse IA [reconstitué] :** une proposition générique de chat ; il manque le rôle du système, le masquage et la limite d'historique (prompt 38).

*Prompt 38*
````text
précise : fais une classe AssistantIA qui réutilise le même LlmClient, le même masquage des secrets
et la même gestion des erreurs que la revue. la réponse est du markdown.
Le système doit dire que c'est RevIA, qu'il répond en français, que le code est dans des blocs ```,
qu'il ne devine jamais les ***MASQUÉ***, et qu'il refuse poliment ce qui n'est pas de la programmation.
On envoie les 20 derniers messages maximum.
````
- **Réponse IA [reconstitué] :** `AssistantIA` avec `CONSIGNES_COMMUNES`, `MAX_MESSAGES = 20`, `AssistantController` (`/api/assistant/chat`), `AssistantDemo` pour le mode demo. Texte exact des consignes : §10.5.

*Prompt 39 (défaut observé)*
````text
le chat répond parfois au premier message de la conversation au lieu du dernier. corrige le prompt.
````
- **Réponse IA [reconstitué] :** `CONSIGNE_CHAT` « Réponds à la dernière question de l'utilisateur… » et phrase finale ajoutée à chaque requête : « Réponds au dernier message de l'utilisateur. » ; le dernier message doit avoir le rôle `utilisateur`.
- **À retenir :** répéter la consigne clé à la fin du message (effet de récence).

*Prompt 40*
````text
attendre 15 secondes la réponse complète c'est long. je veux que le texte s'affiche au fur et à mesure comme chatgpt.
fais le streaming avec Flux côté spring (SSE) et un service FluxIaService côté angular.
le timeout doit s'appliquer entre deux morceaux, pas sur toute la réponse.
````
- **Réponse IA [reconstitué] :** `AssistantIA.diffuser(Demande)` renvoie un `Flux<String>` avec `.timeout(Duration.ofSeconds(timeoutSec))` (délai entre deux éléments), routes `/chat/flux` et `/outils/{outil}/flux` ; côté Angular `FluxIaService` lit le flux avec `fetch` + `ReadableStream` (HttpClient ne lit pas un SSE morceau par morceau) ; nginx lit en proxy pendant 120 s.

*Prompt 41*
````text
la réponse s'affiche en markdown brut (avec les ** et les ```). fais un petit rendu markdown dans angular
(titres, gras, listes, blocs de code avec bouton copier) sans librairie externe, et protège contre le xss.
````
- **Réponse IA [reconstitué] :** `shared/markdown.ts` (`markdownVersHtml` : texte échappé **avant** mise en forme), `reponse-ia.component.ts`, `markdown.spec.ts`.
- **À retenir :** la sortie d'un LLM est une entrée non fiable.

*Prompt 42*
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
- **Réponse IA [reconstitué] :** `AssistantIA.Outil` (`tests`, `documentation`, `explication`, `commit`, `refactoring`, `conversion`), `MAX_LIGNES = 500`, `POST /api/assistant/outils/{outil}`, `pages/outils/outils.component.ts`.

*Prompt 43 (retours après test)*
````text
quelques retours après test :
- tests : il génère du JUnit 4, je veux JUnit 5 + AssertJ, et il oublie les cas d'erreur
- documentation : il modifie le code en plus de le commenter
- refactoring : il renvoie "... reste inchangé ..." au lieu du code complet
- conversion : c'est traduit mot à mot, pas idiomatique
corrige les consignes de chaque outil pour ces problèmes.
````
- **Réponse IA [reconstitué] :** consignes corrigées (tableau « Consignes par outil » du §10.5) : JUnit 5 + AssertJ et cas d'erreur, « sans modifier la logique », « code complet dans un seul bloc », conversion « idiomatique ».
- **À retenir :** un défaut observé = une consigne positive précise par outil.

*Prompt 44*
````text
dernier truc : depuis la page outils ou le chat, je veux un bouton "Envoyer en revue"
qui ouvre la page revue avec le code déjà collé. fais un TransfertCodeService.
puis écris les tests (AssistantIATest, AssistantApiTest, markdown.spec.ts, flux-ia.service.spec.ts).
````
- **Réponse IA [reconstitué] :** `transfert-code.service.ts` (signal, `deposer()` puis `prendre()` à lecture unique) et les 4 fichiers de tests.
- **Écart constaté dans le code :** le sens livré est **revue → assistant / outils** (boutons « 🧪 Outils IA » et « 💬 Demander à l'assistant » dans `revue.component.ts`), et non « Envoyer en revue ». Le sens demandé au prompt n'est pas implémenté ; voir §28.

---

## 11. Sécurité, quotas, messages utilisateur

### 11.1 Mesures (README §8)

| Mesure | Détail |
|--------|--------|
| Masquage LLM | MasqueurDonnees |
| Validation sortie | Bean Validation + lignes |
| Clé API | Env only ; `/api/systeme/info` sans clé |
| Quota jour | 50 revues/dev + 100 requêtes assistant/outils par dev (configurables, modifiables dans Admin) **avant le LLM** ; l'administrateur n'est pas limité |
| Bucket4j | 10 analyses/min/user ; 10 connexions/min/IP ; en-têtes `X-RateLimit-Limit` / `X-RateLimit-Remaining` sur chaque réponse limitée, limites affichées dans Admin > Quotas |
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


**Évolution : administration assistée par IA (profil, quotas conseillés, assistant en langage naturel, alertes)** (prompt rédigé après la livraison initiale)

*Prompt consolidé (format agent)*

```text
Tu es un ingénieur full-stack senior (Spring Boot 3 / Angular 17).

CONTEXTE
La page Admin gère comptes, rôles et quotas. RevIA enregistre déjà les revues (score, problèmes), l'usage de l'assistant, les quotas, la dernière connexion et limite le débit avec Bucket4j.

MISSION
1) Profil de compétences d'un développeur : failles récurrentes, évolution du score, 3 axes de progression (fiche dans la page Admin).
2) Quotas conseillés : analyse de la consommation des 30 derniers jours, quota proposé par développeur, bouton « Appliquer » (et « Tout appliquer »).
3) Assistant d'administration en langage naturel : l'IA traduit « bloque les comptes inactifs depuis 30 jours » en actions ; l'administrateur confirme avant exécution.
4) Détection d'usage anormal : pic de requêtes, secrets en dur répétés, connexions échouées répétées, quota presque atteint.

CONTRAINTES
- Les chiffres sont calculés par le serveur ; l'IA ne fait que rédiger ou traduire. Elle ne reçoit jamais de code source, seulement des agrégats (compteurs, titres de problèmes, noms de comptes).
- L'IA ne contrôle jamais l'exécution : actions limitées à BLOQUER, DEBLOQUER, CHANGER_ROLE, DEFINIR_QUOTA ; les comptes sont résolus par le serveur ; chaque action est revalidée à l'exécution ; un administrateur n'est jamais bloqué ; son propre rôle n'est pas modifiable ; pas de suppression ni de mot de passe par l'assistant.
- Sans IA (mode demo, clé absente, timeout), des règles de secours produisent le même format de réponse.
- Réservé au rôle ADMIN (403 sinon).

VÉRIFICATION
ApiIntegrationTest.adminProfilQuotasEtAlertes et assistantAdministration ; ng build production ; parcours : page Admin → saisir une commande → confirmer.
```

*Échanges réels*

*Prompt (brainstorm)*
````text
WHAT CAN WE ADD TO USERMANAHEMTN DONE A FUNCTIOANNLITY WITH AI BRAINSTORM
````
- **Réponse IA [reconstitué] :** dix idées classées (profil de compétences, quotas conseillés, assistant d'administration, détection d'usage anormal, mot de passe lisible, résumé hebdomadaire, comptes inactifs, parcours d'apprentissage…), avec des précautions : analyses réservées à l'admin, confirmation humaine avant toute action sur un compte, masquage des secrets.

*Prompt (décision)*
````text
do it  [liste collée : profil de compétences, recommandation de quotas, assistant d'administration en langage naturel, détection d'usage anormal]
````
- **Réponse IA [reconstitué] :** `ai/ConseilIA.java` (appel IA avec texte de secours), `service/AdminIAService.java` (profil, recommandations, alertes, interprétation, exécution), `security/JournalConnexions.java` (échecs de connexion en mémoire, vidés au redémarrage), endpoints `/api/admin/...` ci-dessus ; côté interface : barre « Assistant d'administration » et bouton « Profil » (onglet Utilisateurs), bloc « Quotas conseillés par l'IA » (onglet Quotas), onglet « Alertes » et indicateur dans le bandeau.
- **À retenir / limites :** les noms de comptes (sans e-mail) sont envoyés au LLM pour l'interprétation d'une commande ; les échecs de connexion ne sont conservés qu'en mémoire (15 minutes de fenêtre, perdus au redémarrage) ; le résumé IA des alertes n'est demandé qu'à l'ouverture de l'onglet pour garder la page rapide.

**Évolution : bibliothèque « Mes prompts » appelable depuis l'Assistant IA** (prompt rédigé après la livraison initiale)

*Prompt consolidé (format agent)*

```text
Tu es un ingénieur full-stack senior (Spring Boot 3 / Angular 17).

CONTEXTE
L'écran Assistant IA propose 4 suggestions fixes. Les développeurs réutilisent souvent les mêmes consignes (« explique en simple », « liste les risques »…).

MISSION
1) Backend : bibliothèque de prompts PERSONNELS (distincte du prompt système de revue, §9) : créer, lister, modifier, supprimer ; titre 2 à 80 caractères, contenu jusqu'à 4 000 ; 50 prompts maximum par utilisateur ; migration Flyway V3.
2) Écran « Mes prompts » (/prompts) : liste, formulaire de création / modification, suppression avec confirmation, bouton « Utiliser dans l'assistant ».
3) Écran Assistant IA : menu « Mes prompts » dans la barre, et les prompts affichés comme suggestions sur l'écran d'accueil ; un clic place le prompt dans la zone de saisie (à la suite du texte déjà écrit) pour pouvoir le compléter avant d'envoyer ; /assistant?prompt=ID le préremplit.

CONTRAINTES
- Les prompts d'un utilisateur sont privés : toute requête filtre sur l'auteur (prompt d'un autre = 404, comme pour les revues).
- Le prompt inséré passe ensuite par le circuit normal de l'assistant : masquage des secrets, quota assistant, rate limit.
- Aucun appel direct à un LLM depuis ce module.

VÉRIFICATION
ApiIntegrationTest.mesPrompts ; ng build production ; parcours : créer un prompt, ouvrir /assistant, l'insérer, envoyer.
```

*Échanges réels*

*Prompt*
````text
can we add a screen for proimoprt ? where we define prompt and we can call them in the screen Assitant IA
````
- **Réponse IA [reconstitué] :** nouvelle entité `PromptUtilisateur` (table `prompt_utilisateur`, V3), `PromptUtilisateurService` / `PromptUtilisateurController` sur `/api/mes-prompts`, page `pages/prompts/prompts.component.ts` (route `/prompts`, entrée « Mes prompts » dans le menu), menu déroulant et suggestions dans `assistant.component.ts`. Les prompts sont **privés** ; le partage d'un prompt entre utilisateurs, ou des prompts fournis par l'administrateur, n'est pas inclus.
- **À retenir :** « Mes prompts » (§12, `/api/mes-prompts`) n'a rien à voir avec les **versions du prompt système** de la page Admin (`/api/admin/prompts`) : le premier est une aide à la saisie de l'utilisateur, le second pilote la revue de code.
- **À retenir :** la route `/prompts` est déclarée dans `SpaController` (comme `/assistant` et `/outils`, §28) pour que F5 fonctionne en mode JAR.

### 11.3 Prompts de construction — authentification, erreurs, quotas (prompts 9, 10, 11, 26, 29)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur sécurité / backend senior (Spring Security 6.5).

CONTEXTE
RevIA est une API stateless : JWT, aucun cookie de session. Les coûts d'API LLM doivent être maîtrisés.

MISSION
1) Auth : JwtService (génération et vérification), JwtAuthFilter, SecurityConfig (/api/auth/** publique, /api/admin/** réservé ADMIN, le reste authentifié), AuthController (inscription, connexion). BCrypt (coût 12), secret JWT lu dans la configuration.
2) DataInitializer : créer l'administrateur au démarrage s'il n'existe pas (REVIA_ADMIN_EMAIL / REVIA_ADMIN_PASSWORD) ; en dev mot de passe par défaut, en prod rien + warning.
3) Erreurs : GlobalExceptionHandler + ErrorCode (VALIDATION, INTROUVABLE, LLM_TIMEOUT, LLM_CLE_ABSENTE, QUOTA…) ; côté Angular : 401 → retour à la connexion avec message, erreurs en notification en français.
4) Coûts : quota de revues par jour et par développeur (configurable, contrôlé AVANT l'appel LLM), RateLimitFilter (Bucket4j) sur les appels IA, timeout du LLM avec message clair, page Paramètres avec le quota restant.

CONTRAINTES
- Jamais de secret en dur ; JWT 120 min ; un compte bloqué est refusé à la requête suivante.
- Aucune stacktrace dans la réponse ; messages exacts de l'annexe (§11.2).

VÉRIFICATION
ApiIntegrationTest (7), ErreursApiTest (4), AnalyseurIATest (timeout 504, 429 → 503).
```

**Échanges réels**

*Prompt 9*
````text
il faut que les gens puissent se connecter. comment je fais ?
````
- **Réponse IA [reconstitué] :** options générales (sessions, JWT, OAuth) sans choix. Il faut préciser la technologie (prompt 10).

*Prompt 10*
````text
je veux du JWT avec spring security, pas de session. donne moi :
- JwtService (générer et vérifier le token)
- JwtAuthFilter
- SecurityConfig (les routes /api/auth/** publiques, le reste protégé, /api/admin/** seulement pour ADMIN)
- AuthController avec inscription et connexion
Le mot de passe doit être hashé avec BCrypt. La clé JWT doit venir de application.yml, pas en dur.
````
- **Réponse IA [reconstitué] :** `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `UtilisateurConnecte`, `AuthController`, `AuthService`. Valeurs finales : BCrypt coût 12, HS256, 120 min (`REVIA_JWT_MINUTES`), secret `REVIA_JWT_SECRET` ≥ 32 caractères en prod.

*Prompt 11*
````text
au démarrage il faut créer un compte admin automatiquement s'il n'existe pas.
l'email et le mot de passe viennent des variables d'environnement REVIA_ADMIN_EMAIL et REVIA_ADMIN_PASSWORD.
en dev, si rien n'est défini, mets un mot de passe par défaut. en prod, ne crée rien et affiche un warning.
fais une classe DataInitializer.
````
- **Réponse IA [reconstitué] :** `DataInitializer.creerAdmin()` : ne fait rien si un administrateur existe ; mot de passe vide + profil `prod` → warning « Aucun administrateur : définissez REVIA_ADMIN_EMAIL et REVIA_ADMIN_PASSWORD… » ; sinon défaut `Admin2026!`. Le même `run()` crée la v1 du prompt système et, si `revia.seed-demo-data=true`, le compte démo (§18).

*Prompt 26 (défauts observés)*
````text
quand le token expire, le front affiche juste une page blanche. et les erreurs du backend s'affichent en json brut.
fais en sorte que :
- si 401, on retourne à la page de connexion avec un message
- les erreurs s'affichent dans une notification en français
côté backend, fais un GlobalExceptionHandler avec un ErrorCode (VALIDATION, INTROUVABLE, LLM_TIMEOUT, LLM_CLE_ABSENTE, QUOTA...).
````
- **Réponse IA [reconstitué] :** backend : `ErrorCode` (16 codes, messages exacts de §11.2), `ApiException`, `ErreurApi`, `GlobalExceptionHandler`. Frontend : `authInterceptor` (401 hors `/api/auth/` → `auth.deconnexion()`), `messageErreur()` (jamais d'erreur technique brute, repli « Impossible de joindre le serveur RevIA… »), `notif.service.ts`, gardes `connecteGuard` / `adminGuard` / `developpeurGuard` / `inviteGuard`.

*Prompt 29*
````text
pour limiter les coûts de l'api :
- un quota de revues par jour et par développeur (configurable)
- un RateLimitFilter sur les appels à l'ia
- un timeout sur l'appel au llm, avec un message clair si ça dépasse
Ajoute une page paramètres où l'utilisateur voit son quota restant.
````
- **Réponse IA [reconstitué] :** `QuotaService` (défaut 50/jour, `REVIA_QUOTA_JOUR`, contrôle avant le LLM), `RateLimitFilter` (Bucket4j : 10 analyses/min/utilisateur, 10 connexions/min/IP, 429 + `Retry-After`, y compris `/api/assistant/*`), timeout 30 s → `LLM_TIMEOUT` (504), `parametres.component.ts` et `GET /api/me` (quota du jour).
- **À retenir :** trois protections distinctes : quota journalier (coût), débit par minute (abus), timeout (disponibilité).

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
| POST | `/api/admin/utilisateurs` | ADMIN | Créer un compte `{nom, email, motDePasse, role: DEVELOPPEUR\|ADMIN, quota?}` → 201 ; e-mail déjà pris → 409 |
| PUT | `/api/admin/utilisateurs/{id}` | ADMIN | Modifier `{nom, email, motDePasse?, quota?}` (le rôle n'est pas modifiable) |
| DELETE | `/api/admin/utilisateurs/{id}` | ADMIN | Supprimer le compte et ses revues (cascade BDD) → 204 ; son propre compte → 400 |
| PUT | `/api/admin/utilisateurs/{id}/quota` | ADMIN | Quotas `{quota, quotaAssistant?}` |
| PUT | `/api/admin/utilisateurs/{id}/role` | ADMIN | Changer le rôle `{role: DEVELOPPEUR\|ADMIN}` (requête native sur la colonne discriminante) ; son propre rôle → 400 |
| POST | `/api/admin/utilisateurs/{id}/reinitialisation-mot-de-passe` | ADMIN | Génère un mot de passe temporaire de 12 caractères, renvoyé **une seule fois** `{motDePasse}` |
| GET / PUT | `/api/admin/parametres` , `/api/admin/parametres/quotas` | ADMIN | Quotas par défaut `{quotaRevuesDefaut, quotaAssistantDefaut, appliquerATous}` |
| PUT | `/api/admin/utilisateurs/{id}/blocage` | ADMIN | Bloquer |
| GET/POST/PUT | `/api/admin/prompts` … `/activer` | ADMIN | Prompts |
| POST | `/api/assistant/chat` (+ `/flux`) | Non | Chat IA |
| POST | `/api/assistant/outils/{outil}` (+ `/flux`) | Non | Outils IA |
| POST | `/api/assistant/compacter` | Non | Compacte la conversation `{messages}` (2 minimum, 40 maximum) en un résumé Markdown ; compte comme une requête assistant. `GET /api/systeme/info` publie `contexteMaxTokens` (12 000) |
| GET / POST | `/api/mes-prompts` | Non | Bibliothèque de prompts personnels (liste triée par titre / création `{titre, contenu}`, 50 max par utilisateur) |
| PUT / DELETE | `/api/mes-prompts/{id}` | Non | Modifier / supprimer ; le prompt d'un autre utilisateur ou un prompt partagé → 404. `GET` renvoie aussi les prompts partagés (`partage: true`, `modifiable: false`) |
| GET | `/api/admin/utilisateurs/{id}/profil` | ADMIN | Profil de compétences d'un développeur (statistiques calculées par le serveur + synthèse rédigée par l'IA) |
| GET | `/api/admin/recommandations-quotas` | ADMIN | Quotas conseillés (pic des 30 derniers jours + 25 %, arrondi aux 5 supérieurs) + commentaire IA |
| GET | `/api/admin/alertes[?resume=false]` | ADMIN | Alertes d'usage anormal (pic d'activité, secrets en dur répétés, connexions échouées, quota proche) ; `resume=false` évite l'appel IA |
| POST | `/api/admin/assistant` | ADMIN | Interprète une commande en français → plan d'actions (rien n'est exécuté) |
| POST | `/api/admin/assistant/executer` | ADMIN | Exécute les actions confirmées ; chacune est revalidée par le serveur |
| GET / POST / PUT / DELETE | `/api/admin/prompts-partages[/{id}]` | ADMIN | Prompts « officiels » visibles de tous les développeurs (onglet « Prompts partagés » de la page Admin) |
| GET | `/api/systeme/info` | Oui | Version, provider, model |
| GET | `/actuator/health` | Oui | Sonde |

**Contrat front :** `frontend/src/app/core/api.service.ts` — toute rupture de contrat exige mise à jour + tests.


### 12.1 Prompts de construction — administration et prompts versionnés (prompts 27 et 28)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur full-stack senior (Spring Boot 3 / Angular 17).

CONTEXTE
RevIA a besoin d'une administration pour piloter les comptes, les quotas et le prompt système sans redéployer.

MISSION
1) Liste des utilisateurs : activer / désactiver (blocage immédiat), modifier le quota journalier.
2) Versions du prompt système : l'admin publie une nouvelle version et choisit laquelle est active ; la v1 est créée au démarrage depuis revue-system.st ; une version sans {langage} est refusée.
3) Statistiques globales.
Livrer : entité PromptSysteme, PromptService, AdminController (/api/admin/**), AdminService, page Angular admin.

CONTRAINTES
- /api/admin/** réservé au rôle ADMIN (403 sinon). L'administrateur ne soumet pas de revues.
- Un compte bloqué est refusé dès la requête suivante.

VÉRIFICATION
ApiIntegrationTest (accès admin, quota) et parcours manuel §21 étape 13.
```

**Échanges réels**

*Prompt 27*
````text
il faut une page admin. qu'est-ce que je peux mettre dedans ?
````
- **Réponse IA [reconstitué] :** une liste d'idées (gestion des comptes, quotas, versions du prompt, statistiques, journal). L'équipe en sélectionne trois au prompt suivant.
- **À retenir :** un prompt ouvert est utile pour obtenir des idées, puis on décide soi-même.

*Prompt 28*
````text
ok je garde ça :
1. liste des utilisateurs (activer / désactiver)
2. gestion des versions du prompt système : l'admin peut publier une nouvelle version et choisir laquelle est active.
   la v1 est créée au démarrage depuis le fichier revue-system.st.
   refuse une nouvelle version si elle ne contient pas {langage}.
3. statistiques globales
Fais l'entité PromptSysteme, PromptService, AdminController et la page admin angular.
````
- **Réponse IA [reconstitué] :** `PromptSysteme`, `PromptSystemeRepository`, `PromptService` (`actif()` crée la v1 depuis le fichier), `AdminController`, `AdminService`, `pages/admin/admin.component.ts`, `TableauDeBordService` pour les statistiques.
- **À retenir :** si le prompt en base est obsolète en dev, publier le contenu du fichier via l'admin ou réinitialiser H2 (§8.2).

**Évolution : gestion complète des utilisateurs, rôles, quotas et mots de passe** (prompts rédigés après la livraison initiale)

*Prompt consolidé (format agent)*

```text
Tu es un ingénieur full-stack senior (Spring Boot 3 / Angular 17).

CONTEXTE
La page Admin ne permettait que de lister les comptes, changer un quota et bloquer. Les rôles sont deux sous-classes JPA (SINGLE_TABLE, colonne discriminante type).

MISSION
1) CRUD utilisateurs : créer (nom, e-mail, mot de passe, rôle, quotas), modifier (nom, e-mail, mot de passe facultatif, quotas), supprimer (revues supprimées en cascade).
2) Rôles : changer le rôle d'un compte existant ; jamais son propre rôle.
3) Quotas : l'assistant et les 6 outils IA comptent dans un quota journalier distinct (100 par défaut) ; quotas par défaut modifiables en base depuis l'interface, avec « appliquer à tous ».
4) Réinitialisation du mot de passe : mot de passe temporaire généré (12 caractères, au moins une lettre et un chiffre), affiché une seule fois à l'administrateur.

CONTRAINTES
- Migration Flyway V2 (colonne quota_assistant, tables usage_assistant et parametre) ; ddl-auto: validate.
- Réservé au rôle ADMIN ; un administrateur ne peut ni se supprimer ni changer son propre rôle ; e-mail unique (409).
- Le quota assistant est vérifié après validation de la requête et avant l'appel au LLM (429 avec message clair) ; l'administrateur n'est pas limité.
- Aucun mot de passe en clair dans les logs ni en base (BCrypt).

VÉRIFICATION
ApiIntegrationTest : crudUtilisateurs et quotasAssistantRolesEtReinitialisation ; ng build production ; ng test.
```

*Échanges réels*

*Prompt (constat)*
````text
I think that crud user managemtn isn't currently working, can you double check and allow user to add eelted and edit users ?
````
- **Réponse IA [reconstitué] :** audit du code : `AdminController` n'expose que `GET /utilisateurs`, `PUT .../quota` et `PUT .../blocage` ; ajout de `POST /utilisateurs`, `PUT /utilisateurs/{id}`, `DELETE /utilisateurs/{id}`, du formulaire Angular (création / modification) et des boutons Modifier / Supprimer avec confirmation. Le rôle n'est pas modifiable (héritage JPA).

*Prompt (rôles et quotas)*
````text
The user management you created, can we add roles to users or not ?

talking quota management , what could be improved ????????
````
- **Réponse IA [reconstitué] :** trois options pour les rôles (changement par requête native sur la colonne `type`, nouveau rôle par sous-classe + migration, refonte du domaine à éviter) et sept améliorations de quotas, dont la plus importante : l'assistant et les outils IA coûtent un appel LLM mais n'étaient limités que par le débit, pas par un quota journalier.

*Prompt (décision)*
````text
ouiiii
````
- **Réponse IA [reconstitué] :** implémentation du quota assistant séparé, des quotas par défaut modifiables en base, et du changement de rôle (`UtilisateurRepository.passerAdministrateur` / `passerDeveloppeur`).

*Prompt (réinitialisation)*
````text
can we add password reinitialisation if not already exsiting
````
- **Réponse IA [reconstitué] :** il n'existait que le changement de mot de passe connecté (`PUT /api/me/mot-de-passe`) et la saisie d'un nouveau mot de passe par l'admin dans le formulaire de modification. Ajout de `POST /api/admin/utilisateurs/{id}/reinitialisation-mot-de-passe` et du bouton « Mot de passe » qui affiche le mot de passe temporaire une seule fois.
- **Refonte de la page Admin (`pages/admin/admin.component.ts`) :** en-tête avec l'état du module IA, 6 indicateurs (comptes, développeurs, administrateurs, bloqués, revues et requêtes assistant du jour), 3 onglets — **Utilisateurs** (recherche, filtre par rôle / bloqués, tableau avec avatar, rôle modifiable, statut, jauges d'activité du jour, boutons Modifier / Mot de passe et menu « ⋯ » pour Bloquer / Supprimer), **Quotas** (quotas par défaut + quotas par développeur) et **Prompt système** (versions, publication, activation).
- **À retenir / limites :** pas de « mot de passe oublié » en libre-service (aucun envoi d'e-mail configuré) ; le jeton JWT déjà émis reste valable jusqu'à son expiration (120 min) après une réinitialisation : bloquer le compte pour couper l'accès immédiatement. Le quota assistant est décompté dès la requête, même si le LLM échoue ensuite.

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
| `/prompts` | connecte + developpeur | Mes prompts (bibliothèque personnelle) |
| `/parametres` | connecte | Paramètres |
| `/admin` | connecte + admin | Admin |

### 13.2 Dev proxy

`frontend/proxy.conf.json` : `/api` et `/actuator` → `http://localhost:8085`.

### 13.3 Prod nginx

`frontend/nginx.conf` : proxy `/api/` → `http://backend:8085`, SPA fallback `index.html`, timeout lecture **120 s** (analyses longues / SSE).

### 13.4 Vercel

`frontend/vercel.json` : rewrite `/api/:path*` → `https://revia-api.onrender.com/api/:path*` ( **à mettre à jour** si URL Render change).


### 13.5 Prompts de construction — pages, design et gestion d'erreurs (prompts 23, 24, 25 et partie front du 26)

**Prompt consolidé (format agent)**

```text
Tu es un développeur front-end senior (Angular 17).

CONTEXTE
RevIA consomme l'API REST (/api/*) avec un jeton JWT.

MISSION
Créer l'interface en composants standalone avec signals :
- auth (connexion / inscription), revue (éditeur de code, choix du langage, bouton analyser), correction (liste des problèmes avec gravité en couleur, diff avant/après), historique, tableau de bord (score moyen, problèmes par catégorie, évolution dans le temps).
- ApiService, AuthService (garde le jeton), intercepteur qui ajoute Authorization.
- Layout avec barre de navigation à gauche.
Design : anneau de score (rouge < 50, orange < 75, vert sinon), badges de gravité et de catégorie, mode sombre, icônes SVG, CSS maison sans bibliothèque UI lourde.
Erreurs : 401 → retour à la connexion avec message ; erreurs affichées en notification en français.

CONTRAINTES
- Aucun appel direct à un LLM ; routes protégées par gardes (§13.1).
- Textes en français.

VÉRIFICATION
ng test (14) et ng build --configuration production ; parcours manuel §21.
```

**Échanges réels**

*Prompt 23*
````text
maintenant le front angular. je veux une page de connexion et une page où on colle le code et on voit les résultats.
````
- **Réponse IA [historique] :** correct mais très basique, sans design et avec des modules `NgModule`.

*Prompt 24*
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
- **Réponse IA [reconstitué] :** `app.routes.ts` (§13.1), `core/{api.service,auth.service,http}.ts`, `layout/shell.component.ts`, `pages/{auth,revue,correction,historique,tableau-de-bord}`, `shared/{diff,code-editor.component}.ts` (Monaco).

*Prompt 25*
````text
le design est trop simple. je veux un style moderne et propre :
- un anneau de score coloré (rouge < 50, orange < 75, vert sinon)
- des badges pour la gravité et la catégorie
- un mode sombre
- des icônes svg simples
pas de bibliothèque ui lourde, juste du css.
````
- **Réponse IA [reconstitué] :** `score-ring.component.ts`, `badges.ts`, `icone.component.ts`, `styles.scss`, palette §22.1 (fond `#0f1222`, indigo `#5b5bf6`, accent IA `#a5b4fc`), thème Monaco `revia-sombre`.

*Prompt 26 (partie front, voir aussi §11.3)*
- **Retour :** page blanche à l'expiration du jeton et JSON brut affiché.
- **Réponse IA [reconstitué] :** `authInterceptor` (401 → déconnexion), `messageErreur()`, `notif.service.ts` ; le catalogue d'erreurs côté serveur est décrit en §11.3.

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
| REVIA_QUOTA_JOUR | 50 (revues par jour, valeur initiale ; modifiable dans Admin) |
| REVIA_QUOTA_ASSISTANT | 100 (requêtes assistant/outils par jour, valeur initiale ; modifiable dans Admin) |
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
    assistant-par-jour: ${REVIA_QUOTA_ASSISTANT:100}
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


### 16.7 Prompts de construction — Docker Compose et profil de production (prompt 34)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur DevOps / backend senior.

CONTEXTE
RevIA doit tourner en local « comme en production » : PostgreSQL + Spring Boot + front Angular.

MISSION
1) docker-compose.yml : services db (postgres:16-alpine, healthcheck), backend (build ./backend, profil prod), frontend (build ./frontend, port 8080).
2) application-prod.yml : tous les secrets lus dans les variables d'environnement.

CONTRAINTES
- Aucun secret en dur dans le dépôt : .env (copié de .env.example) fournit DATABASE_PASSWORD, REVIA_JWT_SECRET, REVIA_AI_API_KEY…
- Le mot de passe PostgreSQL de la base doit être celui que le backend utilise (même variable).
- PostgreSQL non publié sur l'hôte.

VÉRIFICATION
docker compose up -d --build puis connexion sur http://localhost:8080 ; Flyway V1 appliquée ; backend « healthy ».
```

**Échanges réels**

*Prompt 34*
````text
fais moi un docker-compose avec le backend, postgres, et le front servi par spring boot (SpaController).
et un profil application-prod.yml où tous les secrets viennent des variables d'environnement.
````
- **Réponse IA [reconstitué] :** `docker-compose.yml` (db / backend / frontend), `backend/Dockerfile` (build Maven 3.9.9 puis JRE 17 Alpine, utilisateur `revia`, port 8085), `application-prod.yml`, `SpaController` pour servir `index.html` en mode JAR.
- **Retour (évolution) :** le front est finalement servi par un conteneur **nginx** (port 8080, proxy `/api/` → `backend:8085`) ; `SpaController` reste pour le mode JAR (:8085). Les deux modes coexistent (§16).
- **Retour (mise en route, observé) :** sans `.env`, `docker compose up` échoue sur `REVIA_JWT_SECRET` ; avec un mot de passe `.env` différent de `POSTGRES_PASSWORD`, le backend échoue (`password authentication failed for user "revia"`) et nginx répond 502 : voir §24.

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


### 17.5 Prompts de construction — intégration continue (prompt 35)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur DevOps senior (GitHub Actions).

CONTEXTE
RevIA : backend Maven (Java 17) + frontend Angular (Node 20).

MISSION
Écrire .github/workflows/ci.yml déclenché sur chaque push vers main : job backend (mvn -B verify), job frontend (npm ci, ng test headless, ng build production), job docker (build des deux images).

CONTRAINTES
- Aucun appel à un vrai LLM en CI (fournisseur demo / mocks).
- Pas de secret dans le workflow.

VÉRIFICATION
Les trois jobs sont verts sur main.
```

**Échanges réels**

*Prompt 35*
````text
ajoute une github action qui compile le backend, lance les tests java et les tests angular à chaque push sur main.
````
- **Réponse IA [reconstitué] :** `ci.yml` avec les jobs `backend` (artifact surefire, résumé du jeu d'essai), `frontend` et `docker` (§17.1).

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


### 18.1 Prompts de construction — jeu d'essai et manifeste (prompts 30 et 31)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur qualité spécialisé en évaluation de LLM.

CONTEXTE
RevIA doit prouver que la détection fonctionne (≥ 85 % de détection, ≤ 1/3 de faux positif sur les fichiers sains).

MISSION
Créer backend/src/main/resources/jeu-essai/ : des fichiers contenant chacun un problème connu (injection SQL, secret en dur, ressource non fermée, XSS en TypeScript, NullPointerException, comparaison de String avec ==, erreur de bornes) et des fichiers sains. Ajouter manifest.json (problème attendu par fichier) et JeuEssaiIATest qui vérifie la détection (rien de critique sur les fichiers sains).

CONTRAINTES
- Le test automatisé utilise le fournisseur demo ; le benchmark Gemini réel est manuel et documenté.
- Critère manuel : problème à ±2 lignes du défaut attendu.

VÉRIFICATION
JeuEssaiIATest génère backend/target/jeu-essai-rapport.md.
```

**Échanges réels**

*Prompt 30*
````text
je dois prouver que l'ia marche bien pour mon rapport. comment tester ça ?
````
- **Réponse IA [reconstitué] :** proposer un jeu de fichiers à défauts connus et mesurer détection et faux positifs.

*Prompt 31*
````text
crée un jeu d'essai dans resources/jeu-essai avec des fichiers qui contiennent chacun un problème connu :
injection sql, secret en dur, ressource non fermée, xss en typescript, NullPointerException, comparaison de String avec ==,
erreur de bornes, et un fichier sain sans aucun problème.
ajoute un manifest.json avec pour chaque fichier le problème attendu.
puis un test JeuEssaiIATest qui vérifie que l'analyse trouve bien le problème attendu
(et rien de critique sur le fichier sain).
````
- **Réponse IA [reconstitué] :** le dossier `jeu-essai/` (aujourd'hui 23 extraits : 20 défauts + 3 sains, tableau ci-dessus), `manifest.json` et `JeuEssaiIATest`.

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


### 19.1 Prompts de construction — faux positifs et compte de démonstration (prompts 32 et 33)

**Prompt consolidé (format agent)**

```text
Tu es un ingénieur qualité IA.

CONTEXTE
Sur le fichier sain du jeu d'essai, Gemini invente parfois un « problème mineur » (1 fois sur 3). Il faut un compte de démo pour la soutenance, sans coût d'API.

MISSION
1) Décider, preuves à l'appui (réponse collée), s'il faut changer le prompt ou la température ; appliquer le correctif.
2) Créer le compte « Sarra A. » avec un historique de revues sur 30 jours, uniquement avec le fournisseur demo.
3) Écrire docs/rapport-jeu-essai-gemini.md avec les résultats.

CONTRAINTES
- Température 0,2 ; règle « N'invente aucun problème » conservée.
- Données de démo créées seulement avec revia.seed-demo-data=true ET fournisseur demo.

VÉRIFICATION
Rapport : 19/20 détections, 0/3 faux positifs, environ 1,5 s par analyse.
```

**Échanges réels**

*Prompt 32 (réponse du LLM collée)*
````text
le test sur le fichier sain échoue encore une fois sur 3, gemini trouve un "problème mineur" inventé.
voici la réponse : [réponse collée]
est-ce que je dois changer le prompt ou la température ?
````
- **Réponse IA [reconstitué] :** privilégier le prompt (règle anti-invention, ligne exacte, filtrage côté Java) et garder une température basse (0,2) plutôt que de jouer sur la température seule ; la réponse collée n'a pas été conservée dans l'historique.
- **À retenir :** poser une question de **choix** avec la réponse réelle collée.

*Prompt 33*
````text
utilise le jeu d'essai pour créer un compte de démo "Sarra A." avec un historique de revues sur 30 jours,
seulement avec le fournisseur demo pour ne rien payer. ça servira pour la soutenance.
écris aussi un rapport docs/rapport-jeu-essai-gemini.md avec les résultats.
````
- **Réponse IA [reconstitué] :** `DataInitializer.creerDemo()` : compte `sarra@revia.local / Sarra2026!`, créé seulement si `revia.seed-demo-data=true` (profil dev) et fournisseur = `demo` (sinon log « Données de démonstration ignorées… pas de coût d'API ») ; `docs/rapport-jeu-essai-gemini.md`.

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
| ApiIntegrationTest | 14 | Auth, revue, isolation, quota, admin, CRUD utilisateurs, rôles, quota assistant, réinitialisation, mes prompts, prompts partagés, dernière connexion, en-têtes Bucket4j, compactage, admin IA (profil, quotas conseillés, alertes, assistant d'administration) |
| ErreursApiTest | 4 | Erreurs LLM, Bucket4j |
| AssistantIATest | 9 | Chat/outils IA unitaire |
| AssistantApiTest | 3 | MockMvc assistant + SSE |

**Total actuel attendu : 61 tests** (README mentionne 42 — documenter l’écart si README non mis à jour).

**Front :** `npx ng test --watch=false --browsers=ChromeHeadless` — **21 tests** (relevé à l'exécution ; le README en annonce 14).

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
| `SpaController` (corrigé) | `/assistant`, `/outils` et `/prompts` manquaient dans `web/SpaController.java` : en mode JAR (:8085), F5 ou lien direct renvoyait 404. Les 3 routes sont maintenant ajoutées (si ce fichier est restauré à une ancienne version, les retirer fait réapparaître le problème) ; **toute nouvelle route Angular de premier niveau doit être ajoutée à ce `@GetMapping`**. Docker (nginx `try_files`) et Vercel n'étaient pas concernés |
| Sens du transfert de code | L'historique (prompt 44) parle d'un bouton « Envoyer en revue » (outils/chat → revue). Le code livré fait l'inverse : **revue → assistant / outils** (`revue.component.ts` `ouvrir()`). Voir le bloc « Prompts de construction » du §10 |
| Docker Compose local | `docker-compose.yml` fixe `POSTGRES_PASSWORD: revia123` et `REVIA_ADMIN_PASSWORD: revia123` en dur, alors que `DATABASE_PASSWORD` et `REVIA_JWT_SECRET` viennent de `.env` : si `DATABASE_PASSWORD` ≠ `revia123`, le backend échoue (« password authentication failed for user "revia" »), nginx renvoie **502** sur `/api/**` |

---

## Definition of done (phrase finale)

RevIA est **prêt** lorsqu’un relecteur peut exécuter `scripts\build.ps1` et `scripts\start.ps1`, se connecter, obtenir une revue validée (demo ou Gemini) avec masquage et JSON contrôlés, passer **tous les tests** backend et frontend, valider la checklist **§25** (au minimum A–G et parcours §21), et déployer optionnellement Docker ou Render/Vercel sans fuite de clé — en restant conforme à **RESUME.md**, **README.md** et **docs/rapport-jeu-essai-gemini.md**.

---

*Fin du document — `docs/PROMPT_AGENT_IA_DETAILLE.md`*
