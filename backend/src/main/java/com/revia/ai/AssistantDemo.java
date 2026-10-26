package com.revia.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mode « demo » de l'assistant et des outils IA : réponses construites par gabarits à partir du code
 * (fonctions détectées, lignes ajoutées / supprimées…), sans réseau ni clé d'API.
 * Elles montrent le parcours complet ; seul un vrai LLM (Gemini) produit un contenu intelligent.
 */
final class AssistantDemo {

    private static final Pattern OUTIL = Pattern.compile("(?m)^Outil : ([^.\\n]+)");
    private static final Pattern LANGAGE = Pattern.compile("(?m)^(?:Langage|Code joint \\(langage) ?:? ?(\\w+)");
    private static final Pattern CIBLE = Pattern.compile("(?m)^Langage cible : (.+)$");
    private static final Pattern BLOC = Pattern.compile("(?s)```\\w*\\n(.*?)\\n```");
    private static final Pattern FONCTION = Pattern.compile(
            "(?m)^\\s*(?:(?:public|private|protected|static|final|async|export|def|function|func|fn|void|int|long|double|"
                    + "float|boolean|bool|String|string|char|var|let|const)\\s+)+(?:[\\w<>\\[\\],]+\\s+)?(\\w+)\\s*\\(");
    private static final Set<String> MOTS_CLES = Set.of("if", "for", "while", "switch", "catch", "return", "new", "main");
    private static final String AVERTISSEMENT = "\n\n> ℹ️ **Mode démo** : réponse construite par des règles locales. "
            + "Branchez Google Gemini (`REVIA_AI_PROVIDER=gemini`) pour une réponse rédigée par l'IA.";

    private AssistantDemo() {
    }

    static String repondre(String systeme, String message) {
        String outil = trouver(OUTIL, systeme, "assistant (chat)");
        String langage = trouver(LANGAGE, message, "autre");
        Matcher bloc = BLOC.matcher(message);
        String code = bloc.find() ? bloc.group(1) : "";
        List<String> fonctions = fonctions(code);
        String liste = fonctions.isEmpty() ? "- (aucune fonction détectée)"
                : String.join("\n", fonctions.stream().map(f -> "- `" + f + "`").toList());
        if (outil.startsWith("compactage")) {
            return compactage(message);
        }
        String corps = switch (outil) {
            case "génération de tests" -> tests(langage, fonctions);
            case "documentation automatique" -> "## Documentation\n\nFonctions et méthodes à documenter :\n" + liste
                    + "\n\nPour chacune : rôle, paramètres, valeur de retour, exceptions.\n\n## README\n\n"
                    + "**Rôle** : à compléter.  \n**Utilisation** : appeler la fonction principale avec ses paramètres.";
            case "explication de code" -> "### En bref\nCe code " + langage + " contient " + nbLignes(code) + " ligne(s) et "
                    + fonctions.size() + " fonction(s).\n\n### Pas à pas\n" + liste
                    + "\n\n### Points d'attention\nLancez une **revue** pour détecter les failles et les bugs.";
            case "message de commit et description de pull request" -> commit(code);
            case "refactoring" -> "### Pistes de refactoring\n- Extraire les blocs répétés en fonctions courtes\n"
                    + "- Donner des noms explicites aux variables\n- Remplacer les valeurs en dur par des constantes\n\n"
                    + "Fonctions concernées :\n" + liste;
            case "conversion de langage" -> "### Conversion vers " + trouver(CIBLE, message, "le langage cible").toUpperCase(Locale.ROOT)
                    + "\nLa conversion automatique nécessite un vrai LLM. Fonctions à convertir :\n" + liste;
            default -> "Bonjour ! Je suis l'assistant RevIA. Je peux expliquer une erreur, proposer une correction "
                    + "ou générer des tests. Posez votre question et joignez votre code si besoin.";
        };
        return corps + AVERTISSEMENT;
    }

    /** Résumé par gabarit : questions posées et début de la dernière réponse (un vrai LLM rédige un résumé intelligent). */
    private static String compactage(String message) {
        List<String> questions = new ArrayList<>();
        String derniereReponse = "";
        for (String ligne : message.split("\n")) {
            if (ligne.startsWith("Utilisateur : ")) {
                questions.add(coupe(ligne.substring(14), 140));
            } else if (ligne.startsWith("Assistant : ")) {
                derniereReponse = coupe(ligne.substring(12), 200);
            }
        }
        StringBuilder sb = new StringBuilder("### Résumé de la conversation\n\n**Questions de l'utilisateur**\n");
        for (String q : questions) {
            sb.append("- ").append(q).append('\n');
        }
        if (!derniereReponse.isBlank()) {
            sb.append("\n**Dernière réponse de l'assistant** : ").append(derniereReponse).append('\n');
        }
        return sb.append("\n_(Résumé généré en mode démo ; avec Gemini, il est rédigé par l'IA.)_").toString();
    }

    private static String coupe(String texte, int max) {
        String t = texte.strip().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static String tests(String langage, List<String> fonctions) {
        List<String> noms = fonctions.isEmpty() ? List.of("fonction") : fonctions;
        StringBuilder sb = new StringBuilder("Squelette de tests à compléter :\n\n");
        switch (langage) {
            case "python" -> {
                sb.append("```python\nimport pytest\n");
                noms.forEach(f -> sb.append("\n\ndef test_").append(f).append("_cas_nominal():\n    assert ").append(f)
                        .append("(...) == ...\n"));
            }
            case "javascript", "typescript" -> {
                sb.append("```").append(langage).append('\n');
                noms.forEach(f -> sb.append("test('").append(f).append(" : cas nominal', () => {\n  expect(").append(f)
                        .append("(/* ... */)).toBe(/* ... */);\n});\n\n"));
            }
            default -> {
                sb.append("```java\nimport org.junit.jupiter.api.Test;\nimport static org.assertj.core.api.Assertions.assertThat;\n\nclass CodeTest {\n");
                noms.forEach(f -> sb.append("\n    @Test\n    void ").append(f).append("_casNominal() {\n        // assertThat(")
                        .append(f).append("(...)).isEqualTo(...);\n    }\n"));
                sb.append("}\n");
            }
        }
        return sb.append("```\n\n**Ce qui est testé** : le cas nominal de chaque fonction détectée.").toString();
    }

    private static String commit(String diff) {
        long ajouts = diff.lines().filter(l -> l.startsWith("+") && !l.startsWith("+++")).count();
        long retraits = diff.lines().filter(l -> l.startsWith("-") && !l.startsWith("---")).count();
        return "```text\nchore: mise à jour du code (+" + ajouts + " / -" + retraits + " lignes)\n\n- À détailler\n```\n\n"
                + "## Résumé\nModification du code (" + ajouts + " ligne(s) ajoutée(s), " + retraits + " supprimée(s)).\n\n"
                + "## Changements\n- À compléter\n\n## Comment tester\n- Lancer les tests unitaires\n\n## Risques\n- Faibles";
    }

    private static List<String> fonctions(String code) {
        Set<String> noms = new LinkedHashSet<>();
        Matcher m = FONCTION.matcher(code);
        while (m.find()) {
            if (!MOTS_CLES.contains(m.group(1))) {
                noms.add(m.group(1));
            }
        }
        return new ArrayList<>(noms);
    }

    private static int nbLignes(String code) {
        return code.isEmpty() ? 0 : (int) code.lines().count();
    }

    private static String trouver(Pattern p, String texte, String defaut) {
        Matcher m = p.matcher(texte);
        return m.find() ? m.group(1).trim().toLowerCase(Locale.ROOT) : defaut;
    }
}
