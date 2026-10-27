package com.revia.ai;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.revia.config.ReviaProperties;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;

import reactor.core.publisher.Flux;

/**
 * Au-delà de la revue : assistant IA (chat) et outils qui accélèrent le travail du développeur
 * (tests, documentation, explication, message de commit, refactoring, conversion).
 * Même LLM, même masquage des secrets et même gestion des erreurs que la revue ;
 * la réponse est du texte libre en Markdown.
 */
@Service
public class AssistantIA {

    private static final Logger log = LoggerFactory.getLogger(AssistantIA.class);

    /** Au-delà, la réponse (tests, code documenté…) risque d'être tronquée par le modèle. */
    public static final int MAX_LIGNES = 500;
    public static final int MAX_MESSAGES = 20;
    /** Budget de contexte affiché à l'utilisateur (barre « contexte utilisé » de l'Assistant IA), en tokens estimés. */
    public static final int CONTEXTE_MAX_TOKENS = 12_000;

    static final String CONSIGNES_COMMUNES = """
            Tu es RevIA, un assistant de développement logiciel pour des étudiants et des développeurs.
            Rédige en français correct, en Markdown (le code et ses identifiants restent dans leur langue).
            Mets tout code dans un bloc ``` suivi du nom du langage.
            Les valeurs ***MASQUÉ*** sont des secrets masqués volontairement avant l'envoi : ne les devine jamais,
            garde ***MASQUÉ*** ou remplace-les par une variable d'environnement.
            Si la demande n'a aucun rapport avec la programmation, réponds poliment que tu ne traites que le développement logiciel.
            """;

    /** Les outils de la boîte à outils IA, avec leur consigne propre. */
    public enum Outil {
        TESTS("tests", "Génération de tests", """
                Outil : génération de tests.
                Écris des tests unitaires prêts à copier pour le code fourni, avec le framework standard du langage
                (JUnit 5 + AssertJ pour Java, pytest pour Python, Jest pour JavaScript/TypeScript, PHPUnit pour PHP,
                xUnit pour C#, le paquet testing pour Go, sinon le plus courant).
                Couvre le cas nominal, les cas limites et les cas d'erreur.
                Structure : une phrase d'introduction, un seul bloc de code complet, puis une liste « Ce qui est testé ».
                """),
        DOCUMENTATION("documentation", "Documentation automatique", """
                Outil : documentation automatique.
                Renvoie le code complet avec des commentaires de documentation au format standard du langage
                (Javadoc, docstrings Python, JSDoc/TSDoc, PHPDoc, commentaires XML C#…) sur chaque classe,
                fonction et méthode, sans modifier la logique.
                Ajoute ensuite une section « README » courte : rôle du code, utilisation, exemple d'appel.
                """),
        EXPLICATION("explication", "Explication de code", """
                Outil : explication de code.
                Explique le code à un développeur débutant, avec des phrases courtes :
                1. « En bref » : ce que fait le code, en deux phrases ;
                2. « Pas à pas » : les étapes, en citant les numéros de ligne (le code est numéroté) ;
                3. « Points d'attention » : bugs ou risques éventuels (aucun s'il n'y en a pas).
                """),
        COMMIT("commit", "Message de commit et PR", """
                Outil : message de commit et description de pull request.
                Le texte fourni est un diff (ou le code modifié).
                Rédige :
                1. un message de commit au format Conventional Commits (type(portée): résumé, 72 caractères maximum),
                   suivi d'un corps en puces, dans un bloc ```text ;
                2. une description de pull request avec les titres ## Résumé, ## Changements, ## Comment tester, ## Risques.
                """),
        REFACTORING("refactoring", "Refactoring", """
                Outil : refactoring.
                Propose une version plus lisible, plus simple et plus sûre du code, sans changer son comportement.
                Donne le code complet refactoré dans un seul bloc, puis une liste « Ce qui a changé et pourquoi ».
                """),
        CONVERSION("conversion", "Conversion de langage", """
                Outil : conversion de langage.
                Convertis le code dans le langage cible indiqué, de façon idiomatique (conventions et bibliothèque
                standard du langage cible), en conservant le comportement.
                Donne le code converti dans un seul bloc, puis une liste « Points d'attention »
                (différences de comportement, dépendances à installer).
                """);

        private final String id;
        private final String libelle;
        private final String consigne;

        Outil(String id, String libelle, String consigne) {
            this.id = id;
            this.libelle = libelle;
            this.consigne = consigne;
        }

        public String id() {
            return id;
        }

        public String libelle() {
            return libelle;
        }

        public static Outil depuis(String id) {
            String v = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(o -> o.id.equals(v)).findFirst()
                    .orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE, "Outil IA inconnu : " + id));
        }
    }

    static final String CONSIGNE_COMPACTAGE = """
            Outil : compactage de la conversation.
            Résume la conversation ci-dessous pour pouvoir la poursuivre sans perdre le fil.
            Garde : l'objectif de l'utilisateur, les décisions prises, les erreurs rencontrées, les questions encore ouvertes
            et les extraits de code importants (en blocs ```). Supprime les politesses et les répétitions.
            Écris en Markdown, à la troisième personne, en 250 mots maximum (hors code).
            """;

    static final String CONSIGNE_CHAT = """
            Outil : assistant (chat).
            Réponds à la dernière question de l'utilisateur en t'appuyant sur la conversation et, s'il est fourni,
            sur le code joint. Sois concis et concret, avec un court exemple de code quand c'est utile.
            """;

    /** Un message de la conversation : rôle « utilisateur » ou « assistant ». */
    public record Message(String role, String contenu) {
    }

    /** Réponse de l'assistant ou d'un outil. */
    public record Reponse(String contenu, String langage, int nbMasquages, long dureeMs, String fournisseur, String modele) {
    }

    private final LlmClient llm;
    private final MasqueurDonnees masqueur;
    private final PreparateurCode preparateur;
    private final ExecutorService executor;
    private final ReviaProperties.Ai config;

    public AssistantIA(LlmClient llm, MasqueurDonnees masqueur, PreparateurCode preparateur,
                       @Qualifier("analyseExecutor") ExecutorService executor, ReviaProperties props) {
        this.llm = llm;
        this.masqueur = masqueur;
        this.preparateur = preparateur;
        this.executor = executor;
        this.config = props.ai();
    }

    /** Demande prête à envoyer au modèle : consignes, message (code masqué) et informations pour la réponse. */
    public record Demande(String systeme, String message, String langage, int nbMasquages, long debut) {
    }

    /** Exécute un outil de la boîte à outils sur un extrait de code. */
    public Reponse outil(Outil outil, String code, String langage, String cible) {
        return repondre(preparerOutil(outil, code, langage, cible));
    }

    /** Assistant IA : répond au dernier message en tenant compte de la conversation et du code joint. */
    public Reponse discuter(List<Message> messages, String code, String langage) {
        return repondre(preparerChat(messages, code, langage));
    }

    /** Réponse complète en un seul bloc. */
    public Reponse repondre(Demande d) {
        String contenu = AnalyseurIA.executer(() -> llm.rediger(d.systeme(), d.message()), executor, config.timeoutSec());
        return new Reponse(contenu.trim(), d.langage(), d.nbMasquages(), System.currentTimeMillis() - d.debut(),
                llm.fournisseur(), llm.modele());
    }

    /**
     * Réponse diffusée morceau par morceau (streaming) : le texte s'affiche dès que le modèle écrit.
     * Délai maximal entre deux morceaux = délai d'analyse ; les erreurs sont traduites en messages clairs.
     */
    public Flux<String> diffuser(Demande d) {
        return llm.redigerEnFlux(d.systeme(), d.message())
                .timeout(Duration.ofSeconds(config.timeoutSec()))
                .onErrorMap(e -> e instanceof TimeoutException
                        ? new ApiException(ErrorCode.LLM_TIMEOUT, ErrorCode.LLM_TIMEOUT.message(), e)
                        : AnalyseurIA.traduire(e));
    }

    public String fournisseur() {
        return llm.fournisseur();
    }

    public String modele() {
        return llm.modele();
    }

    public Demande preparerOutil(Outil outil, String code, String langage, String cible) {
        verifierDisponible();
        verifierTaille(code);
        if (outil == Outil.CONVERSION && (cible == null || cible.isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION, "Choisissez le langage cible de la conversion.");
        }
        long debut = System.currentTimeMillis();
        String lang = preparateur.detecterLangage(langage, null, code);
        MasqueurDonnees.Resultat masque = masqueur.masquer(code);
        String extrait = outil == Outil.EXPLICATION
                ? PreparateurCode.numeroter(PreparateurCode.lignes(masque.code()), 1)
                : masque.code();
        StringBuilder message = new StringBuilder("Langage : ").append(lang).append('\n');
        if (outil == Outil.CONVERSION) {
            message.append("Langage cible : ").append(cible.trim()).append('\n');
        }
        message.append(outil == Outil.COMMIT ? "Diff :\n" : "Code :\n").append(bloc(lang, extrait));
        log.info("Outil IA {} : {} ligne(s), {} masquage(s)", outil.id, PreparateurCode.lignes(code).size(), masque.nbMasquages());
        return new Demande(CONSIGNES_COMMUNES + "\n" + outil.consigne, message.toString(), lang, masque.nbMasquages(), debut);
    }

    public Demande preparerChat(List<Message> messages, String code, String langage) {
        verifierDisponible();
        if (messages == null || messages.isEmpty() || !"utilisateur".equals(messages.get(messages.size() - 1).role())) {
            throw new ApiException(ErrorCode.VALIDATION, "Écrivez votre question.");
        }
        long debut = System.currentTimeMillis();
        int masquages = 0;
        StringBuilder message = new StringBuilder();
        String lang = null;
        if (code != null && !code.isBlank()) {
            verifierTaille(code);
            lang = preparateur.detecterLangage(langage, null, code);
            MasqueurDonnees.Resultat m = masqueur.masquer(code);
            masquages += m.nbMasquages();
            message.append("Code joint (langage : ").append(lang).append(") :\n").append(bloc(lang, m.code())).append('\n');
        }
        message.append("Conversation :\n");
        List<Message> recents = messages.subList(Math.max(0, messages.size() - MAX_MESSAGES), messages.size());
        for (Message msg : recents) {
            MasqueurDonnees.Resultat m = masqueur.masquer(msg.contenu());
            masquages += m.nbMasquages();
            message.append("assistant".equals(msg.role()) ? "Assistant : " : "Utilisateur : ").append(m.code().trim()).append("\n\n");
        }
        message.append("Réponds au dernier message de l'utilisateur.");
        return new Demande(CONSIGNES_COMMUNES + "\n" + CONSIGNE_CHAT, message.toString(), lang, masquages, debut);
    }

    /** Demande de résumé de toute la conversation (au moins deux messages) : le front remplace ensuite l'historique par ce résumé. */
    public Demande preparerCompactage(List<Message> messages) {
        verifierDisponible();
        if (messages == null || messages.size() < 2) {
            throw new ApiException(ErrorCode.VALIDATION, "Il faut au moins deux messages pour compacter la conversation.");
        }
        long debut = System.currentTimeMillis();
        int masquages = 0;
        StringBuilder message = new StringBuilder("Conversation à résumer :\n");
        for (Message msg : messages) {
            MasqueurDonnees.Resultat m = masqueur.masquer(msg.contenu());
            masquages += m.nbMasquages();
            message.append("assistant".equals(msg.role()) ? "Assistant : " : "Utilisateur : ").append(m.code().trim()).append("\n\n");
        }
        return new Demande(CONSIGNES_COMMUNES + "\n" + CONSIGNE_COMPACTAGE, message.toString(), null, masquages, debut);
    }

    private void verifierDisponible() {
        if (!llm.disponible()) {
            log.error("Assistant IA refusé : clé d'API absente pour le fournisseur {}", llm.fournisseur());
            throw new ApiException(ErrorCode.LLM_CLE_ABSENTE);
        }
    }

    private static void verifierTaille(String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION, "Collez votre code.");
        }
        if (PreparateurCode.lignes(code).size() > MAX_LIGNES) {
            throw new ApiException(ErrorCode.VALIDATION,
                    "Les outils IA acceptent " + MAX_LIGNES + " lignes au maximum : collez une partie du code.");
        }
    }

    private static String bloc(String langage, String code) {
        return "```" + ("autre".equals(langage) ? "" : langage) + "\n" + code.stripTrailing() + "\n```\n";
    }
}
