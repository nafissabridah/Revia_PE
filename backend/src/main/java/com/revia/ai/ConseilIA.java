package com.revia.ai;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.revia.config.ReviaProperties;

/**
 * IA au service de l'administration : synthèse de profil, commentaire sur les quotas, résumé d'alertes,
 * interprétation d'une commande en langage naturel. Ne reçoit que des données agrégées (compteurs, titres de
 * problèmes, noms) : jamais de code source. Si l'IA est indisponible ou en mode démo, un texte de secours
 * construit par des règles est renvoyé : l'administration ne dépend jamais du fournisseur.
 */
@Service
public class ConseilIA {

    private static final Logger log = LoggerFactory.getLogger(ConseilIA.class);

    private static final String SYSTEME = """
            Tu es l'assistant d'administration de RevIA, une application de revue de code.
            Rédige en français correct, sans mot d'une autre langue, de façon concise et factuelle.
            N'invente aucun chiffre : utilise uniquement les données fournies.
            """;

    private final LlmClient llm;
    private final ExecutorService executor;
    private final ReviaProperties.Ai config;

    public ConseilIA(LlmClient llm, @Qualifier("analyseExecutor") ExecutorService executor, ReviaProperties props) {
        this.llm = llm;
        this.executor = executor;
        this.config = props.ai();
    }

    /** Vrai si un vrai modèle (et non le moteur à règles « demo ») peut répondre. */
    public boolean modeleDisponible() {
        return llm.disponible() && !"demo".equals(llm.fournisseur());
    }

    /** Texte rédigé par l'IA, ou le texte de secours si l'IA est indisponible, trop lente ou en erreur. */
    public String rediger(String consigne, String donnees, Supplier<String> secours) {
        String reponse = appeler(consigne, donnees);
        return reponse == null || reponse.isBlank() ? secours.get() : reponse.trim();
    }

    /** Réponse brute du modèle (pour un JSON à valider par l'appelant), ou null si indisponible. */
    public String appeler(String consigne, String donnees) {
        if (!modeleDisponible()) {
            return null;
        }
        try {
            return AnalyseurIA.executer(() -> llm.rediger(SYSTEME + "\n" + consigne, donnees), executor, config.timeoutSec());
        } catch (RuntimeException e) {
            log.warn("IA d'administration indisponible, texte de secours utilisé : {}", e.getMessage());
            return null;
        }
    }
}
