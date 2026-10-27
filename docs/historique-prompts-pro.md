# RevIA : prompts de construction (version professionnelle)

Ce document reformule de façon structurée les 44 prompts envoyés au LLM pendant la construction de RevIA. L'ordre, la numérotation et l'intention de chaque prompt sont ceux de l'historique brut (docs/historique-prompts.md).
Chaque prompt suit le même schéma (contexte, objectif, exigences, livrables) et se termine par une ligne « Livré » qui renvoie au code réel de l'application.

---

## Phase 1 : cadrage et initialisation

### Prompt 1

```text
Contexte : mini-projet de cours (Sujet 4) portant sur une application web d'intelligence artificielle.
Objectif : définir le produit « RevIA », un outil de revue de code assistée par IA.
Principe : un développeur colle un extrait de code ; l'IA détecte les failles de sécurité, les bugs et les
mauvaises pratiques, explique chaque problème et propose une correction.
Attendu :
- les acteurs (développeur, administrateur) et les cas d'utilisation principaux ;
- les contraintes structurantes : clé d'API jamais exposée au navigateur, coût d'utilisation maîtrisé,
  application entièrement en français.
```

### Prompt 2

```text
Contrainte imposée par le cours : front-end Angular, back-end Spring Boot.
Propose l'architecture cible de RevIA :
- une SPA Angular qui ne communique qu'avec une API REST Spring Boot (/api/*) ;
- l'appel au LLM réalisé exclusivement côté serveur.
Liste ensuite les étapes de réalisation dans l'ordre : initialisation, modèle de domaine, authentification,
intégration de l'IA, front-end, tests, déploiement.
```

### Prompt 3

```text
Projet : RevIA. Stack : Angular 17 (composants standalone) et Spring Boot 3 / Java 17.
Donne l'arborescence d'un dépôt unique avec deux dossiers, backend/ et frontend/ :
- back-end, package com.revia : web, service, ai, domain, repository, security, error, config ;
- front-end, src/app : core (services), layout, pages, shared (composants réutilisables).
Explique le rôle de chaque dossier en une ligne.
```

### Prompt 4

```text
Rédige le pom.xml du back-end (Spring Boot 3, Java 17) avec les dépendances :
spring-boot-starter-web, data-jpa, security, validation ; H2 pour le développement ;
PostgreSQL pour la production ; Flyway pour les migrations du schéma.
Donne aussi la commande Angular CLI qui crée le projet front-end avec le routage et les styles SCSS.
```

> Livré : backend/pom.xml, frontend/ (Angular 17.3), migration backend/src/main/resources/db/migration/V1__schema_initial.sql.

---

## Phase 2 : modèle de domaine et persistance

### Prompt 5

```text
À partir du besoin de RevIA, propose le modèle de domaine : les entités (utilisateur, revue de code,
problème détecté, correction proposée), leurs attributs, leurs relations et leurs cardinalités.
Présente-le sous forme de diagramme de classes textuel avant toute implémentation.
```

### Prompt 6

```text
Implémente le modèle de domaine en JPA, avec des noms en français alignés sur le diagramme de classes :
- Utilisateur : classe abstraite (héritage SINGLE_TABLE) spécialisée en Developpeur (quota journalier)
  et Administrateur ;
- Revue : titre, langage, code, mode d'analyse, score sur 100, résumé, date de création,
  liste de Probleme (cascade) ;
- Probleme : ligne, categorie, gravite, titre, explication, référence OWASP, indicateur de faux positif,
  et une Correction (codeAvant, codeApres, justification) ;
- enums Categorie (SECURITE, BUG, BONNE_PRATIQUE, PERFORMANCE) et Gravite (CRITIQUE, MAJEURE, MINEURE, INFO).
Le score est calculé côté serveur : 100 - 20 x critiques - 6 x majeures - 2 x mineures, borné entre 0 et 100,
en excluant les faux positifs.
```

> Livré : domain/Utilisateur.java, Developpeur.java, Administrateur.java, Revue.java (calculerScore), Probleme.java, Correction.java, Categorie.java, Gravite.java.

### Prompt 7

```text
Ajoute la couche d'accès aux données et le service métier :
- repositories Spring Data pour Utilisateur, Revue, Probleme ;
- RevueService : créer une revue, lister mes revues (paginé), consulter une revue, supprimer une revue ;
- chaque requête est filtrée par auteur : un développeur ne voit jamais les revues d'un autre.
L'analyse renvoie pour l'instant un résultat simulé ; l'IA sera branchée à la phase 4.
```

> Livré : repository/*Repository.java, service/RevueService.java, web/RevueController.java.

### Prompt 8

```text
Diagnostic d'erreur au démarrage de l'application :

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'revueService'
... No qualifying bean of type 'com.revia.repository.ProblemeRepository' available

Identifie la cause racine, propose la correction et explique comment éviter ce type d'erreur
(détection des repositories et des entités par Spring Boot).
```

---

## Phase 3 : authentification et sécurité

### Prompt 9

```text
RevIA doit authentifier ses utilisateurs. Compare une authentification par session et une authentification
par jeton JWT pour une SPA Angular qui appelle une API REST sans état.
Recommande une solution et justifie-la (sécurité, simplicité, déploiement).
```

### Prompt 10

```text
Implémente l'authentification JWT sans état avec Spring Security :
- JwtService : génération et vérification du jeton (HS256, durée configurable, 120 minutes par défaut) ;
- JwtAuthFilter : lecture du header Authorization: Bearer ;
- SecurityConfig : /api/auth/** et /api/systeme/info publics, /api/admin/** réservé au rôle ADMIN,
  tout le reste authentifié ;
- AuthController : inscription (/api/auth/register) et connexion (/api/auth/login).
Mots de passe hachés avec BCrypt. Le secret JWT est lu depuis la configuration (REVIA_JWT_SECRET),
jamais écrit dans le code.
```

> Livré : security/JwtService.java, JwtAuthFilter.java, SecurityConfig.java, web/AuthController.java, service/AuthService.java.

### Prompt 11

```text
Crée une classe DataInitializer qui garantit l'existence d'un compte administrateur au démarrage :
- e-mail et mot de passe lus dans REVIA_ADMIN_EMAIL et REVIA_ADMIN_PASSWORD ;
- profil dev : mot de passe par défaut si rien n'est défini ;
- profil prod : aucun compte créé sans variable, avec un avertissement dans les journaux.
```

> Livré : config/DataInitializer.java.

---

## Phase 4 : intégration du LLM (Gemini)

### Prompt 12

```text
Je dois appeler Google Gemini depuis Spring Boot pour analyser du code.
Compare les options d'intégration (appel HTTP direct, Spring AI) selon trois critères :
protection de la clé d'API, gestion des erreurs et des délais, possibilité de changer de fournisseur.
```

### Prompt 13

```text
Intègre le LLM avec Spring AI derrière une abstraction :
- interface LlmClient, méthode rediger(systeme, message) ;
- implémentation SpringAiLlmClient ;
- configuration dans application.yml (bloc revia.ai) : fournisseur, modèle, clé, température 0,2, délai maximal ;
- fournisseurs : gemini (par défaut en production), groq, ollama, openai, anthropic ;
- fournisseur « demo » qui produit des réponses au même format sans appel réseau ni coût,
  pour le développement, les tests et la soutenance.
```

> Livré : ai/LlmClient.java, SpringAiLlmClient.java, LlmConfig.java, config/ReviaProperties.java.

### Prompt 14

```text
Conçois le prompt système qui fera réaliser la revue de code par Gemini.
Il doit fixer le rôle du modèle, la tâche, les catégories et gravités attendues, et un format de sortie
exploitable par un programme. Propose une première version et explique tes choix.
```

### Prompt 15

```text
La réponse du LLM doit être analysée automatiquement en Java. Améliore le prompt suivant et enregistre-le
dans resources/prompts/revue-system.st :

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

Ajoute à la fin le format JSON exact, aligné sur l'entité Probleme (score, resume, problemes[...]).
Le code est envoyé avec son numéro devant chaque ligne.
```

> Livré : backend/src/main/resources/prompts/revue-system.st (version finale en annexe A), ai/PreparateurCode.java.

### Prompt 16

```text
Gemini entoure parfois le JSON de balises Markdown, ce qui fait échouer l'analyse :

com.fasterxml.jackson.core.JsonParseException: Unexpected character ('`' (code 96))

Crée une classe ReponseIA qui :
- nettoie la réponse (retire les balises, conserve le texte du premier { au dernier }) ;
- valide les champs (Bean Validation, catégories et gravités normalisées) ;
- en cas de JSON invalide, relance l'appel une fois puis renvoie une erreur métier claire
  (LLM_REPONSE_INVALIDE, HTTP 502) au lieu d'une trace technique.
```

> Livré : ai/ReponseIA.java, paramètre revia.ai.json-retries: 1.

### Prompt 17

```text
Deux défauts observés :
1. sur un code correct, Gemini signale quand même deux ou trois problèmes mineurs inventés ;
2. certains numéros de ligne n'existent pas dans l'extrait.
Corrige à deux niveaux :
- dans le prompt : le numéro de ligne doit être exactement l'un des numéros affichés devant le code ;
- dans le code Java : ignorer tout problème dont la ligne est hors de l'extrait.
```

> Livré : consigne dans revue-system.st, contrôle des lignes dans ai/AnalyseurIA.java et ReponseIA.java.

---

## Phase 5 : protection des données envoyées à l'IA

### Prompt 18

```text
Le code soumis par les utilisateurs peut contenir des mots de passe ou des clés d'API qui ne doivent pas
être transmis à un fournisseur externe. Propose une stratégie de protection avant l'envoi au LLM
et ses limites.
```

### Prompt 19

```text
Implémente une classe MasqueurDonnees qui remplace par ***MASQUÉ***, avant l'envoi au LLM :
mots de passe, clés d'API, jetons, chaînes de connexion, adresses e-mail et clés privées.
- Elle renvoie le code masqué et le nombre de masquages (record Resultat), affiché dans l'interface.
- Elle conserve le nombre de lignes, pour que les numéros de ligne restent justes.
Écris les tests JUnit correspondants (MasqueurDonneesTest) avec des exemples représentatifs.
```

> Livré : ai/MasqueurDonnees.java, MasqueurDonneesTest.

### Prompt 20

```text
Effets de bord du masquage :
1. Gemini signale ***MASQUÉ*** comme une erreur de syntaxe ;
2. sur un fichier de test contenant PASSWORD = "admin123", le mot de passe en dur n'est plus signalé,
   puisque sa valeur est masquée.
Ajoute au prompt système une consigne qui :
- interdit de traiter ***MASQUÉ*** comme une erreur ;
- impose de signaler ***MASQUÉ*** comme un secret en dur (catégorie securite, gravité critique,
  OWASP A07:2021) lorsqu'il est la valeur d'un mot de passe ou d'un secret écrit dans le code.
```

### Prompt 21

```text
Les explications de Gemini mélangent français et anglais (« hardcoded », « fallback », « externaliser »).
Ajoute au prompt une consigne de rédaction en français correct, sans mot d'une autre langue,
avec un exemple de reformulation : « le déplacer dans une variable d'environnement » plutôt que
« l'externaliser ».
```

### Prompt 22

```text
Ajoute un AnalyseurStatique en Java qui détecte sans IA les cas évidents : mot de passe ou secret en dur,
concaténation dans une requête SQL, mot de passe dans les journaux, ressources non fermées,
dépassement d'indice, faille XSS, etc.
Il complète la réponse du LLM pour ne pas manquer de problème important, et sert de moteur
au fournisseur « demo ».
```

> Livré : ai/AnalyseurStatique.java, ai/RedacteurRevue.java.

---

## Phase 6 : front-end Angular

### Prompt 23

```text
Démarre le front-end Angular de RevIA avec deux écrans : la connexion, et la soumission d'un extrait de code
avec l'affichage des résultats de la revue.
```

### Prompt 24

```text
Restructure le front-end en composants standalone Angular 17, avec les signals.
Pages :
- auth : connexion et inscription ;
- revue : éditeur de code (Monaco), choix du langage et du mode d'analyse, bouton « Analyser » ;
- correction : liste des problèmes avec gravité colorée et diff avant/après pour chaque correction ;
- historique : mes revues ;
- tableau de bord : score moyen, problèmes par catégorie, évolution dans le temps (Chart.js).
Services : ApiService pour les appels REST, AuthService qui conserve le jeton, intercepteur HTTP
qui ajoute le header Authorization. Gardes de routes selon le rôle.
Mise en page : barre de navigation latérale à gauche.
```

> Livré : app.routes.ts, core/api.service.ts, core/auth.service.ts, core/http.ts, layout/shell.component.ts, pages/*, shared/code-editor.component.ts, shared/diff.ts.

### Prompt 25

```text
Applique une charte graphique moderne et sobre :
- anneau de score coloré : rouge sous 50, orange sous 75, vert au-delà ;
- badges de gravité et de catégorie ;
- mode sombre ;
- icônes SVG simples ;
- styles SCSS maison, sans bibliothèque de composants lourde.
```

> Livré : shared/score-ring.component.ts, shared/badges.ts, shared/icone.component.ts, styles.scss.

### Prompt 26

```text
Gestion des erreurs de bout en bout.
Constats : à l'expiration du jeton, le front affiche une page blanche ; les erreurs du back-end
s'affichent en JSON brut.
Front-end :
- réponse 401 : retour à la page de connexion avec un message ;
- toutes les erreurs : notification en français.
Back-end : GlobalExceptionHandler et enum ErrorCode associant un statut HTTP et un message utilisateur
(VALIDATION, INTROUVABLE, QUOTA_UTILISATEUR, DEBIT_LIMITE, LLM_TIMEOUT, LLM_CLE_ABSENTE,
LLM_REPONSE_INVALIDE, ...).
```

> Livré : error/GlobalExceptionHandler.java, ErrorCode.java, ApiException.java, core/http.ts, core/notif.service.ts.

---

## Phase 7 : administration, quotas et prompts versionnés

### Prompt 27

```text
Propose le périmètre de l'espace administrateur de RevIA : quelles fonctionnalités apportent le plus
de valeur pour piloter les utilisateurs, la consommation de l'IA et la qualité des analyses ?
```

### Prompt 28

```text
Périmètre retenu pour l'administration :
1. gestion des utilisateurs : liste, blocage et déblocage, ajustement du quota journalier ;
2. versions du prompt système : l'administrateur publie une nouvelle version et choisit la version active,
   sans redéploiement. La version 1 est créée au démarrage à partir de revue-system.st.
   Toute version qui ne contient pas {langage} est refusée.
Livrables : entité PromptSysteme, PromptService, AdminController (/api/admin/**) et page Angular d'administration.
```

> Livré : domain/PromptSysteme.java, ai/PromptService.java, web/AdminController.java, service/AdminService.java, pages/admin.

### Prompt 29

```text
Maîtrise des coûts et de la charge de l'API d'IA :
- quota de revues par jour et par développeur, configurable (50 par défaut, vérifié avant l'appel au LLM) ;
- RateLimitFilter (Bucket4j) : nombre d'analyses par minute et par utilisateur, et nombre de connexions
  par minute et par adresse IP ;
- délai maximal sur l'appel au LLM, avec un message clair en cas de dépassement (LLM_TIMEOUT, HTTP 504).
Ajoute une page Paramètres où l'utilisateur consulte son quota restant.
```

> Livré : service/QuotaService.java, security/RateLimitFilter.java, pages/parametres.

---

## Phase 8 : évaluation de l'IA et jeu d'essai

### Prompt 30

```text
Je dois démontrer la qualité des analyses de l'IA dans le rapport.
Propose une méthode d'évaluation reproductible : jeu d'essai, indicateurs (taux de détection,
faux positifs sur du code sain), seuils d'acceptation et automatisation.
```

### Prompt 31

```text
Crée un jeu d'essai dans resources/jeu-essai :
- 20 extraits contenant chacun un défaut connu, dans plusieurs langages (Java, Python, PHP, JavaScript,
  TypeScript, C) : injection SQL, secret en dur, ressource non fermée, erreur de bornes, faille XSS,
  NullPointerException, division par zéro, comparaison de String avec == ;
- 3 extraits sains servant de témoins.
Ajoute un manifest.json qui décrit pour chaque fichier le langage, le défaut attendu et sa ligne.
Écris le test JeuEssaiIATest : le défaut attendu doit être trouvé, et aucun problème critique
ne doit être signalé sur les extraits sains.
```

> Livré : backend/src/main/resources/jeu-essai/ (23 fichiers + manifest.json), JeuEssaiIATest.

### Prompt 32

```text
Le test sur un extrait sain échoue environ une fois sur trois : Gemini signale un problème mineur inventé.
Réponse obtenue : [réponse collée]
Analyse la cause et recommande la bonne action : modifier le prompt, ajuster la température, ou les deux.
```

### Prompt 33

```text
À partir du jeu d'essai, crée un compte de démonstration « Sarra A. » avec un historique de revues
réparti sur 30 jours, généré uniquement avec le fournisseur « demo » (aucun coût) et seulement
en profil dev. Ce compte servira à la soutenance.
Rédige aussi docs/rapport-jeu-essai-gemini.md avec les résultats mesurés.
```

> Livré : DataInitializer.creerDemo (sarra@revia.local, revia.seed-demo-data), docs/rapport-jeu-essai-gemini.md.

---

## Phase 9 : industrialisation et déploiement

### Prompt 34

```text
Prépare le déploiement :
- docker-compose.yml : PostgreSQL, back-end Spring Boot et front-end servi par nginx ;
- mode JAR unique : le front-end compilé est servi par Spring Boot (SpaController) ;
- profil application-prod.yml : tous les secrets (base de données, JWT, clé d'IA) proviennent
  des variables d'environnement.
```

> Livré : docker-compose.yml, backend/src/main/resources/application-prod.yml, web/SpaController.java, frontend/nginx.conf.

### Prompt 35

```text
Ajoute une GitHub Action déclenchée à chaque push sur main :
compilation et tests du back-end (mvn verify), tests Angular en ChromeHeadless, build de production
du front-end, construction des images Docker, et publication du rapport du jeu d'essai
dans le résumé de l'exécution.
```

> Livré : .github/workflows/ci.yml.

### Prompt 36

```text
Rédige la documentation :
- README.md : présentation, architecture, lancement en développement et avec Docker,
  variables d'environnement, comptes de démonstration ;
- RESUME.md : synthèse courte destinée à l'enseignant.
```

---

## Phase 10 : assistant IA et boîte à outils

### Prompt 37

```text
Contexte : RevIA réalise déjà la revue de code. Je veux ajouter un assistant conversationnel qui répond
aux questions des développeurs sur leur code.
Propose la conception : intégration au back-end existant, contraintes (sécurité des secrets, coût, langue),
et expérience utilisateur côté Angular.
```

### Prompt 38

```text
Implémente la classe AssistantIA en réutilisant les briques de la revue :
même LlmClient, même MasqueurDonnees (appliqué au code joint et à chaque message),
même PreparateurCode et même traduction des erreurs que AnalyseurIA.
La réponse est du Markdown libre (pas de JSON).
Consignes système communes :
- tu es RevIA, assistant de développement logiciel, tu réponds en français correct ;
- tout code est placé dans un bloc ``` suivi du nom du langage ;
- les valeurs ***MASQUÉ*** ne sont jamais devinées ;
- toute demande sans rapport avec la programmation est poliment refusée.
Contraintes : seuls les 20 derniers messages sont envoyés ; le dernier message doit provenir de l'utilisateur.
Exposer le service via AssistantController (POST /api/assistant/chat) et prévoir des réponses fixes
pour le fournisseur « demo ».
```

> Livré : ai/AssistantIA.java (CONSIGNES_COMMUNES, MAX_MESSAGES = 20), ai/AssistantDemo.java, web/AssistantController.java.

### Prompt 39

```text
Défaut observé : dans une conversation longue, l'assistant répond parfois au premier message
au lieu du dernier.
Corrige le prompt : la consigne du chat précise qu'il faut répondre à la dernière question,
et le message envoyé se termine par un rappel explicite : « Réponds au dernier message de l'utilisateur. »
```

> Livré : AssistantIA.CONSIGNE_CHAT.

### Prompt 40

```text
Le temps d'attente de la réponse complète (environ 15 secondes) dégrade l'expérience.
Implémente l'affichage progressif de la réponse :
- back-end : AssistantIA.diffuser() renvoie un Flux<String>, exposé en Server-Sent Events
  (POST /api/assistant/chat/flux) avec les événements « morceau », « fin » et « erreur » ;
- front-end : un service FluxIaService qui lit le flux avec fetch et ReadableStream ;
- le délai maximal s'applique entre deux morceaux, et non sur la réponse entière.
```

> Livré : AssistantIA.diffuser, AssistantController (endpoints /flux), core/flux-ia.service.ts.

### Prompt 41

```text
Les réponses s'affichent en Markdown brut (**, ```).
Implémente un rendu Markdown léger dans Angular, sans bibliothèque externe : titres, gras, italique,
listes, citations et blocs de code avec étiquette de langage et bouton « Copier ».
Sécurité : tout le texte est échappé avant la mise en forme, pour empêcher toute injection XSS.
```

> Livré : shared/markdown.ts, shared/reponse-ia.component.ts.

### Prompt 42

```text
Ajoute une page « Outils » (boîte à outils IA) qui applique au code fourni l'un des six outils suivants,
chacun avec sa propre consigne :
1. tests : génération de tests avec le framework standard du langage ;
2. documentation : documentation automatique, sans modifier la logique ;
3. explication : explication pour un débutant, en citant les numéros de ligne (code numéroté) ;
4. commit : message Conventional Commits et description de pull request à partir d'un diff ;
5. refactoring : même comportement, code complet ;
6. conversion : conversion vers un langage cible choisi par l'utilisateur (paramètre obligatoire).
Implémentation : enum Outil dans AssistantIA, endpoints POST /api/assistant/outils/{outil} (et /flux),
limite de 500 lignes de code, outil inconnu traité comme INTROUVABLE.
```

> Livré : AssistantIA.Outil, MAX_LIGNES = 500, pages/outils/outils.component.ts.

### Prompt 43

```text
Retours de test sur les consignes des outils, à corriger par des règles précises :
- tests : le modèle produit du JUnit 4 et oublie les cas d'erreur ; imposer JUnit 5 + AssertJ (Java),
  pytest, Jest, PHPUnit, xUnit ou testing (Go), et couvrir cas nominal, cas limites et cas d'erreur ;
- documentation : le modèle modifie le code ; imposer le code complet commenté au format standard
  (Javadoc, docstrings, JSDoc/TSDoc, PHPDoc...) sans aucun changement de logique ;
- refactoring : le modèle renvoie « ... reste inchangé ... » ; imposer le code complet dans un seul bloc ;
- conversion : traduction mot à mot ; imposer un code idiomatique (conventions et bibliothèque standard
  du langage cible) et une liste « Points d'attention ».
```

> Livré : consignes de l'enum AssistantIA.Outil (texte complet en annexe B).

### Prompt 44

```text
Relie la revue de code au module Assistant :
- sur la page Revue, deux boutons « Outils IA » et « Demander à l'assistant » ouvrent la page cible
  avec le code et le langage déjà renseignés ;
- le transfert passe par un TransfertCodeService côté Angular (signal, deposer() puis prendre()
  en lecture unique) : le code ne repasse pas par le serveur.
Écris ensuite les tests : AssistantIATest et AssistantApiTest (back-end), markdown.spec.ts et
flux-ia.service.spec.ts (front-end).
```

> Livré : core/transfert-code.service.ts, pages/revue/revue.component.ts (ouvrir), pages/assistant, pages/outils, tests AssistantIATest, AssistantApiTest, markdown.spec.ts, flux-ia.service.spec.ts.

---

## Annexe A : prompt système de revue (revue-system.st)

Texte réellement utilisé par l'application, issu du prompt 15 et complété par les prompts 17, 20 et 21.

```text
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

## Annexe B : consignes de l'assistant IA (AssistantIA.java)

Textes réellement utilisés par le module Assistant, issus des prompts 38, 39, 42 et 43.

Consignes communes (CONSIGNES_COMMUNES) :

```text
Tu es RevIA, un assistant de développement logiciel pour des étudiants et des développeurs.
Rédige en français correct, en Markdown (le code et ses identifiants restent dans leur langue).
Mets tout code dans un bloc ``` suivi du nom du langage.
Les valeurs ***MASQUÉ*** sont des secrets masqués volontairement avant l'envoi : ne les devine jamais,
garde ***MASQUÉ*** ou remplace-les par une variable d'environnement.
Si la demande n'a aucun rapport avec la programmation, réponds poliment que tu ne traites que le développement logiciel.
```

Consigne du chat (CONSIGNE_CHAT) :

```text
Outil : assistant (chat).
Réponds à la dernière question de l'utilisateur en t'appuyant sur la conversation et, s'il est fourni,
sur le code joint. Sois concis et concret, avec un court exemple de code quand c'est utile.
```

Consignes des six outils (enum Outil) :

```text
Outil : génération de tests.
Écris des tests unitaires prêts à copier pour le code fourni, avec le framework standard du langage
(JUnit 5 + AssertJ pour Java, pytest pour Python, Jest pour JavaScript/TypeScript, PHPUnit pour PHP,
xUnit pour C#, le paquet testing pour Go, sinon le plus courant).
Couvre le cas nominal, les cas limites et les cas d'erreur.
Structure : une phrase d'introduction, un seul bloc de code complet, puis une liste « Ce qui est testé ».

Outil : documentation automatique.
Renvoie le code complet avec des commentaires de documentation au format standard du langage
(Javadoc, docstrings Python, JSDoc/TSDoc, PHPDoc, commentaires XML C#…) sur chaque classe,
fonction et méthode, sans modifier la logique.
Ajoute ensuite une section « README » courte : rôle du code, utilisation, exemple d'appel.

Outil : explication de code.
Explique le code à un développeur débutant, avec des phrases courtes :
1. « En bref » : ce que fait le code, en deux phrases ;
2. « Pas à pas » : les étapes, en citant les numéros de ligne (le code est numéroté) ;
3. « Points d'attention » : bugs ou risques éventuels (aucun s'il n'y en a pas).

Outil : message de commit et description de pull request.
Le texte fourni est un diff (ou le code modifié).
Rédige :
1. un message de commit au format Conventional Commits (type(portée): résumé, 72 caractères maximum),
   suivi d'un corps en puces, dans un bloc ```text ;
2. une description de pull request avec les titres ## Résumé, ## Changements, ## Comment tester, ## Risques.

Outil : refactoring.
Propose une version plus lisible, plus simple et plus sûre du code, sans changer son comportement.
Donne le code complet refactoré dans un seul bloc, puis une liste « Ce qui a changé et pourquoi ».

Outil : conversion de langage.
Convertis le code dans le langage cible indiqué, de façon idiomatique (conventions et bibliothèque
standard du langage cible), en conservant le comportement.
Donne le code converti dans un seul bloc, puis une liste « Points d'attention »
(différences de comportement, dépendances à installer).
```
