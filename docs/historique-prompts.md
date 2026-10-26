# RevIA : historique des prompts

Ce fichier contient les prompts envoyés au LLM pendant la construction de RevIA, du premier message jusqu'à la fin.
Ils s'améliorent au fil du projet : au début ils sont vagues, ensuite ils deviennent plus précis (contexte, contraintes, format attendu, erreurs collées).

---

## Phase 1 : l'idée et le démarrage

### Prompt 1

```text
salut, je dois faire un projet en IA pour mon cours. je veux faire un site qui fait la revue de code avec l'ia.
tu peux me faire ça ?
```

> Résultat : une seule page HTML avec un textarea et un appel direct à une API depuis le navigateur (clé visible dans le code). Pas utilisable.

### Prompt 2

```text
non je veux un vrai projet avec un frontend et un backend. le prof veut angular et spring boot.
c'est quoi les étapes pour commencer ?
```

> Résultat : une liste d'étapes générale, mais sans structure de dossiers ni versions.

### Prompt 3

```text
ok. donne moi la structure des dossiers du projet avec angular 17 pour le front et spring boot 3 (java 17) pour le back.
le projet s'appelle RevIA. explique moi chaque dossier en 1 ligne.
```

### Prompt 4

```text
donne moi le pom.xml du backend avec les dépendances : web, jpa, h2 pour le dev, postgresql pour la prod, security, validation.
et la commande pour créer le projet angular avec le routing.
```

---

## Phase 2 : le backend de base

### Prompt 5

```text
fais moi les entités : un utilisateur, une revue de code, et les problèmes trouvés dans la revue.
```

> Résultat : les entités étaient en anglais (User, Review, Issue) et il manquait la gravité et la catégorie.

### Prompt 6

```text
refais les entités en français stp (Utilisateur, Revue, Probleme).
Utilisateur doit être abstrait avec 2 types : Developpeur et Administrateur.
Un Probleme a : ligne, categorie (securite, bug, bonne_pratique, performance), gravite (critique, majeure, mineure, info),
titre, explication, et une correction (code avant, code après, justification).
Une Revue a : titre, langage, code, score sur 100, résumé, date, et une liste de problèmes.
Utilise des enums pour la catégorie et la gravité.
```

### Prompt 7

```text
maintenant les repositories et un service RevueService avec créer une revue, lister mes revues, voir une revue, supprimer.
pour l'instant l'analyse peut renvoyer un faux résultat, on branchera l'ia après.
```

### Prompt 8

```text
j'ai cette erreur au démarrage :

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'revueService'
... No qualifying bean of type 'com.revia.repository.ProblemeRepository' available

c'est quoi le problème ?
```

---

## Phase 3 : l'authentification

### Prompt 9

```text
il faut que les gens puissent se connecter. comment je fais ?
```

### Prompt 10

```text
je veux du JWT avec spring security, pas de session. donne moi :
- JwtService (générer et vérifier le token)
- JwtAuthFilter
- SecurityConfig (les routes /api/auth/** publiques, le reste protégé, /api/admin/** seulement pour ADMIN)
- AuthController avec inscription et connexion
Le mot de passe doit être hashé avec BCrypt. La clé JWT doit venir de application.yml, pas en dur.
```

### Prompt 11

```text
au démarrage il faut créer un compte admin automatiquement s'il n'existe pas.
l'email et le mot de passe viennent des variables d'environnement REVIA_ADMIN_EMAIL et REVIA_ADMIN_PASSWORD.
en dev, si rien n'est défini, mets un mot de passe par défaut. en prod, ne crée rien et affiche un warning.
fais une classe DataInitializer.
```

---

## Phase 4 : brancher l'IA (Gemini)

### Prompt 12

```text
comment j'appelle gemini depuis spring boot pour analyser le code ?
```

> Résultat : un appel HTTP brut avec RestTemplate, la clé dans l'URL, pas de gestion d'erreur.

### Prompt 13

```text
je préfère utiliser Spring AI. fais une interface LlmClient avec une méthode rediger(systeme, message)
et une implémentation SpringAiLlmClient. Le fournisseur et le modèle doivent être configurables dans application.yml.
Ajoute aussi un fournisseur "demo" qui renvoie des réponses fixes, pour tester sans clé et sans payer.
```

### Prompt 14

```text
c'est quoi le prompt que je dois envoyer à gemini pour qu'il fasse la revue ?
```

> Résultat : « Analyse ce code et donne les erreurs. » Les réponses étaient en texte libre, impossibles à afficher proprement.

### Prompt 15

```text
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
```

### Prompt 16

```text
des fois gemini renvoie le json entouré de ```json ... ``` et ça plante :

com.fasterxml.jackson.core.JsonParseException: Unexpected character ('`' (code 96))

fais une classe ReponseIA qui nettoie la réponse (enlève les balises, prend du premier { au dernier })
et qui valide les champs. si le json est invalide, renvoie une erreur claire à l'utilisateur, pas une stacktrace.
```

### Prompt 17

```text
problème : sur un code qui est correct, gemini invente quand même 2 ou 3 problèmes mineurs.
et parfois le numéro de ligne n'existe pas dans le code.
ajoute dans le prompt que la ligne doit être exactement un des numéros affichés,
et côté java ignore les problèmes dont la ligne est hors de l'extrait.
```

---

## Phase 5 : sécurité des données envoyées à l'IA

### Prompt 18

```text
je ne veux pas que les mots de passe ou les clés api qui sont dans le code des utilisateurs partent chez google.
comment faire ?
```

### Prompt 19

```text
fais une classe MasqueurDonnees qui remplace par ***MASQUÉ*** avant l'envoi :
mots de passe, clés api, tokens, chaînes de connexion, emails, clés privées.
elle renvoie le code masqué + le nombre de masquages (pour l'afficher dans l'interface).
écris aussi les tests JUnit avec des exemples.
```

### Prompt 20

```text
depuis le masquage, gemini dit que ***MASQUÉ*** est une erreur de syntaxe.
et pire : sur mon fichier de test avec PASSWORD = "admin123" il ne signale plus le mot de passe en dur,
puisqu'il ne voit plus la valeur.
ajoute une consigne dans le prompt pour ça : ne pas traiter ***MASQUÉ*** comme une erreur,
mais si c'est la valeur d'un mot de passe ou d'un secret dans le code, le signaler en critique (OWASP A07:2021).
```

### Prompt 21

```text
les explications de gemini mélangent français et anglais ("hardcoded", "externaliser", "fallback").
ajoute au prompt qu'il doit écrire en français correct sans mot d'une autre langue.
```

### Prompt 22

```text
ajoute aussi un AnalyseurStatique en java qui détecte les cas évidents sans IA
(mot de passe en dur, concaténation SQL, System.out, catch vide...) pour compléter la réponse de gemini
et éviter qu'il rate des choses importantes.
```

---

## Phase 6 : le frontend

### Prompt 23

```text
maintenant le front angular. je veux une page de connexion et une page où on colle le code et on voit les résultats.
```

> Résultat : correct mais très basique, sans design et avec des modules NgModule.

### Prompt 24

```text
refais en standalone components (angular 17), avec les signals.
Pages :
- auth (connexion / inscription)
- revue (éditeur de code + choix du langage + bouton analyser)
- correction (liste des problèmes avec la gravité en couleur, et un diff avant/après pour chaque correction)
- historique (mes revues)
- tableau de bord (stats : score moyen, problèmes par catégorie, évolution dans le temps)
Un ApiService pour les appels, un AuthService qui garde le token, et un intercepteur qui ajoute le header Authorization.
Mets un layout avec une barre de navigation à gauche.
```

### Prompt 25

```text
le design est trop simple. je veux un style moderne et propre :
- un anneau de score coloré (rouge < 50, orange < 75, vert sinon)
- des badges pour la gravité et la catégorie
- un mode sombre
- des icônes svg simples
pas de bibliothèque ui lourde, juste du css.
```

### Prompt 26

```text
quand le token expire, le front affiche juste une page blanche. et les erreurs du backend s'affichent en json brut.
fais en sorte que :
- si 401, on retourne à la page de connexion avec un message
- les erreurs s'affichent dans une notification en français
côté backend, fais un GlobalExceptionHandler avec un ErrorCode (VALIDATION, INTROUVABLE, LLM_TIMEOUT, LLM_CLE_ABSENTE, QUOTA...).
```

---

## Phase 7 : admin, quotas et prompts versionnés

### Prompt 27

```text
il faut une page admin. qu'est-ce que je peux mettre dedans ?
```

### Prompt 28

```text
ok je garde ça :
1. liste des utilisateurs (activer / désactiver)
2. gestion des versions du prompt système : l'admin peut publier une nouvelle version et choisir laquelle est active.
   la v1 est créée au démarrage depuis le fichier revue-system.st.
   refuse une nouvelle version si elle ne contient pas {langage}.
3. statistiques globales
Fais l'entité PromptSysteme, PromptService, AdminController et la page admin angular.
```

### Prompt 29

```text
pour limiter les coûts de l'api :
- un quota de revues par jour et par développeur (configurable)
- un RateLimitFilter sur les appels à l'ia
- un timeout sur l'appel au llm, avec un message clair si ça dépasse
Ajoute une page paramètres où l'utilisateur voit son quota restant.
```

---

## Phase 8 : tests et jeu d'essai

### Prompt 30

```text
je dois prouver que l'ia marche bien pour mon rapport. comment tester ça ?
```

### Prompt 31

```text
crée un jeu d'essai dans resources/jeu-essai avec des fichiers qui contiennent chacun un problème connu :
injection sql, secret en dur, ressource non fermée, xss en typescript, NullPointerException, comparaison de String avec ==,
erreur de bornes, et un fichier sain sans aucun problème.
ajoute un manifest.json avec pour chaque fichier le problème attendu.
puis un test JeuEssaiIATest qui vérifie que l'analyse trouve bien le problème attendu
(et rien de critique sur le fichier sain).
```

### Prompt 32

```text
le test sur le fichier sain échoue encore une fois sur 3, gemini trouve un "problème mineur" inventé.
voici la réponse : [réponse collée]
est-ce que je dois changer le prompt ou la température ?
```

### Prompt 33

```text
utilise le jeu d'essai pour créer un compte de démo "Sarra A." avec un historique de revues sur 30 jours,
seulement avec le fournisseur demo pour ne rien payer. ça servira pour la soutenance.
écris aussi un rapport docs/rapport-jeu-essai-gemini.md avec les résultats.
```

---

## Phase 9 : déploiement

### Prompt 34

```text
fais moi un docker-compose avec le backend, postgres, et le front servi par spring boot (SpaController).
et un profil application-prod.yml où tous les secrets viennent des variables d'environnement.
```

### Prompt 35

```text
ajoute une github action qui compile le backend, lance les tests java et les tests angular à chaque push sur main.
```

### Prompt 36

```text
écris le README : présentation, architecture, comment lancer en dev et avec docker,
les variables d'environnement, et les comptes de démo. et un RESUME.md court pour le prof.
```

---

## Phase 10 : assistant IA et page outils (2e commit)

### Prompt 37

```text
je veux ajouter un chat avec l'ia dans le site, pour poser des questions sur son code.
```

### Prompt 38

```text
précise : fais une classe AssistantIA qui réutilise le même LlmClient, le même masquage des secrets
et la même gestion des erreurs que la revue. la réponse est du markdown.
Le système doit dire que c'est RevIA, qu'il répond en français, que le code est dans des blocs ```,
qu'il ne devine jamais les ***MASQUÉ***, et qu'il refuse poliment ce qui n'est pas de la programmation.
On envoie les 20 derniers messages maximum.
```

### Prompt 39

```text
le chat répond parfois au premier message de la conversation au lieu du dernier. corrige le prompt.
```

### Prompt 40

```text
attendre 15 secondes la réponse complète c'est long. je veux que le texte s'affiche au fur et à mesure comme chatgpt.
fais le streaming avec Flux côté spring (SSE) et un service FluxIaService côté angular.
le timeout doit s'appliquer entre deux morceaux, pas sur toute la réponse.
```

### Prompt 41

```text
la réponse s'affiche en markdown brut (avec les ** et les ```). fais un petit rendu markdown dans angular
(titres, gras, listes, blocs de code avec bouton copier) sans librairie externe, et protège contre le xss.
```

### Prompt 42

```text
ajoute une page Outils avec 6 outils qui prennent du code en entrée, chacun avec sa propre consigne :
1. génération de tests (le bon framework selon le langage)
2. documentation automatique (sans changer la logique)
3. explication pour un débutant (avec les numéros de ligne)
4. message de commit (Conventional Commits) + description de PR
5. refactoring (même comportement, code complet)
6. conversion vers un autre langage (choisir le langage cible)
Limite à 500 lignes. Fais un enum Outil dans AssistantIA.
```

### Prompt 43

```text
quelques retours après test :
- tests : il génère du JUnit 4, je veux JUnit 5 + AssertJ, et il oublie les cas d'erreur
- documentation : il modifie le code en plus de le commenter
- refactoring : il renvoie "... reste inchangé ..." au lieu du code complet
- conversion : c'est traduit mot à mot, pas idiomatique
corrige les consignes de chaque outil pour ces problèmes.
```

### Prompt 44

```text
dernier truc : depuis la page outils ou le chat, je veux un bouton "Envoyer en revue"
qui ouvre la page revue avec le code déjà collé. fais un TransfertCodeService.
puis écris les tests (AssistantIATest, AssistantApiTest, markdown.spec.ts, flux-ia.service.spec.ts).
```

---

## Annexe : prompt système de revue (version finale)

Le prompt du prompt 15, complété par les ajustements des prompts 17, 20 et 21 :

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
