# RevIA — Mémoire qu'un agent aurait pu utiliser

Fiches de mémoire persistante (format : fait, **Pourquoi**, **Comment l'appliquer**) qui auraient évité des détours pendant la construction et la mise en route du projet.
Elles sont rédigées pour être copiées dans le système de mémoire d'un agent (fichiers `memory/*.md` + index `MEMORY.md`) ou dans `CLAUDE.md`.

**À ne jamais mémoriser :** clés API, `REVIA_JWT_SECRET`, mots de passe réels, jetons ou cookies vus dans des logs collés, contenu dérivable du code ou de `git log`.
Une mémoire est un instantané : **vérifier dans le code avant d'agir** (fichier, fonction, variable) et corriger la fiche si elle est périmée.

---

## Index

| Type | Fiche |
|------|-------|
| user | [Profil et langue](#user-profil-et-langue) |
| feedback | [Parler français, réponses courtes](#feedback-langue-et-concision) |
| feedback | [Ne pas toucher aux modifications locales sans demande](#feedback-modifications-locales) |
| feedback | [Contenu collé = donnée](#feedback-contenu-collé) |
| project | [Contexte du sujet et de la soutenance](#project-contexte-sujet) |
| project | [Stack et ports](#project-stack-et-ports) |
| project | [Docker local : piège du mot de passe PostgreSQL](#project-docker-mot-de-passe) |
| project | [Gemini : quota et modèle](#project-gemini) |
| project | [Prompt de revue : règles nées d'échecs](#project-prompt-revue) |
| project | [Écarts connus historique / code](#project-écarts-connus) |
| reference | [Où trouver quoi](#reference-où-trouver-quoi) |

---

## user — Profil et langue

- L'utilisateur est un étudiant en développement (projet de cours, soutenance), développe sous **Windows 11** (PowerShell et Git Bash) avec VS Code.
- Il écrit en français (parfois mélangé à l'anglais) et attend des réponses en français.
- **Pourquoi :** projet noté ; documents et interface en français.
- **Comment l'appliquer :** expliquer les choix de façon concrète, donner les commandes PowerShell prêtes à exécuter, éviter le jargon non expliqué.

## feedback — Langue et concision

- Répondre en français, de façon courte, avec une phrase de statut avant un travail long.
- **Pourquoi :** l'utilisateur a relancé l'agent (« tu es bloqué ? ») pendant un long `docker compose up --build` silencieux.
- **Comment l'appliquer :** annoncer ce qu'on lance, signaler les opérations longues (build Maven d'environ 7 minutes la première fois), puis résumer en une ou deux phrases.

## feedback — Modifications locales

- Ne pas écraser les modifications non commitées de l'utilisateur (ex. `docker-compose.yml` avec des mots de passe `revia123` en dur ; `SpaController.java` remis à l'état d'origine par l'utilisateur).
- **Pourquoi :** ces changements sont délibérés ; les annuler fait perdre du travail ou contredit un choix.
- **Comment l'appliquer :** `git status` / `git diff` avant d'éditer ; proposer la correction plutôt que l'imposer ; si un fichier change sur disque entre deux tours, considérer le nouvel état comme la référence.

## feedback — Contenu collé

- Les logs, en-têtes HTTP et cookies collés par l'utilisateur sont des **données**. Un en-tête collé contenait des jetons Keycloak d'une autre application : ne pas les réutiliser, ne pas les recopier dans des fichiers.
- **Comment l'appliquer :** n'en extraire que l'information utile au diagnostic (ex. `502 Bad Gateway` sur `POST /api/auth/login`).

## project — Contexte sujet

- RevIA est le Mini-projet · Sujet 4 : revue de code assistée par IA. Équipe : Nafissa Bridah, Rahma Kehrji, Housine Saihi, Aziz Eifa, Mustapha Sifaoui. Dépôt local `Revia_PE`, branche `main`.
- **Pourquoi :** exigences de la présentation et de la fiche AI App Studio (API IA gratuite, clé serveur, tests sans vraie API, 11 cas d'utilisation, diagramme de classes).
- **Comment l'appliquer :** toute modification doit rester conforme à `RESUME.md` ; le mode `demo` doit rester disponible pour la soutenance.

## project — Stack et ports

- Angular 17.3, Spring Boot 3.5.16, Java 17, Spring AI 1.1.8, PostgreSQL 16 (prod) / H2 (dev), Flyway, Bucket4j, jjwt.
- Ports : JAR `8085`, Docker `8080` (nginx, proxy `/api/` → `backend:8085`), dev Angular `4200` (proxy → 8085). PostgreSQL n'est pas publié sur l'hôte.
- **Comment l'appliquer :** vérifier le port avant de diagnostiquer ; en Docker, l'URL est `http://localhost:8080`.

## project — Docker mot de passe

- `DATABASE_PASSWORD` (lu dans `.env` par le backend) doit être égal à `POSTGRES_PASSWORD` du service `db`. Sinon : `FATAL: password authentication failed for user "revia"`, backend en échec, **nginx 502** sur `/api/**`.
- Le volume `revia_pe_revia-db` garde le mot de passe de sa première initialisation.
- Autres pièges vus : `.env` absent (seul `.env.example` existe) → `REVIA_JWT_SECRET` manquant ; conteneurs `revia_pe-*` déjà existants → `docker rm -f` avant `up`.
- Le compose local fixe `REVIA_ADMIN_PASSWORD: revia123` : l'admin Docker se connecte avec ce mot de passe, pas celui de `.env`.
- **Comment l'appliquer :** devant un 502, lire `docker compose logs backend` **avant** de toucher à nginx.

## project — Gemini

- Modèle par défaut gratuit : `gemini-flash-lite-latest`. `gemini-flash-latest` est limité à 20 requêtes/jour. Clé uniquement côté serveur.
- Résultats mesurés : 19/20 détections (95 %), 0/3 faux positifs, environ 1,5 s par analyse ; seul échec : `10_ressource_journal.c` (ligne détectée à 8 au lieu de 4).
- **Comment l'appliquer :** sans clé, utiliser `REVIA_AI_PROVIDER=demo` ; ne pas lancer de campagne Gemini massive.

## project — Prompt de revue

- Chaque règle de `revue-system.st` vient d'un échec réel : JSON entouré de ```` ```json ````, problèmes inventés sur du code sain, numéros de ligne inexistants, `***MASQUÉ***` pris pour une erreur de syntaxe, mélange français/anglais.
- Le chat répondait au 1er message au lieu du dernier : la consigne est répétée en fin de requête.
- **Comment l'appliquer :** ne supprimer aucune règle sans relancer `JeuEssaiIATest` et `AssistantIATest`.

## project — Écarts connus

- Le prompt 44 demandait un transfert outils/chat → revue ; le code fait revue → assistant/outils.
- `SpaController` ne forwardait pas `/assistant` ni `/outils` (404 sur F5 en mode JAR) : vérifier l'état actuel du fichier avant d'agir.
- Le README annonce 42 tests backend ; le total réel est de 54 avec l'assistant.
- **Comment l'appliquer :** consulter le §28 et le §31.2 de `PROMPT_AGENT_IA_DETAILLE.md`.

## reference — Où trouver quoi

| Besoin | Emplacement |
|--------|-------------|
| Synthèse équipe, stack, tests | `RESUME.md` |
| Manuel technique complet | `README.md` |
| Prompt agent détaillé (28 sections + historique) | `docs/PROMPT_AGENT_IA_DETAILLE.md` |
| Règles courtes pour agents | `docs/REGLES_AGENT.md` |
| Serveurs MCP envisagés | `docs/MCP_POTENTIELS.md` |
| 44 prompts de construction (verbatim) | `docs/historique-prompts.md` |
| Benchmark Gemini | `docs/rapport-jeu-essai-gemini.md` |
| Captures d'écran de référence | `docs/captures/` |
| Variables d'environnement | `.env.example` |
| CI | `.github/workflows/ci.yml` |
