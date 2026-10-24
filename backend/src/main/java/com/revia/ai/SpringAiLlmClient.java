package com.revia.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

import reactor.core.publisher.Flux;

/** Appel réel au LLM (Groq, Gemini, Ollama, OpenAI ou Claude) via le ChatClient de Spring AI. */
public class SpringAiLlmClient implements LlmClient {

    private final String fournisseur;
    private final String modele;
    private final ChatClient chatClient;
    /** Options ajoutées aux appels de revue pour forcer une réponse JSON (null si le fournisseur ne le gère pas). */
    private final ChatOptions optionsJson;
    /** Options des réponses libres de l'assistant (ex. réflexion minimale chez Gemini, pour répondre vite). */
    private final ChatOptions optionsTexte;

    public SpringAiLlmClient(String fournisseur, String modele, ChatModel chatModel) {
        this(fournisseur, modele, chatModel, null, null);
    }

    public SpringAiLlmClient(String fournisseur, String modele, ChatModel chatModel, ChatOptions optionsJson,
                             ChatOptions optionsTexte) {
        this.fournisseur = fournisseur;
        this.modele = modele;
        this.chatClient = chatModel == null ? null : ChatClient.create(chatModel);
        this.optionsJson = optionsJson;
        this.optionsTexte = optionsTexte;
    }

    @Override
    public String fournisseur() {
        return fournisseur;
    }

    @Override
    public String modele() {
        return modele;
    }

    @Override
    public boolean disponible() {
        return chatClient != null;
    }

    @Override
    public String completer(String promptSysteme, String messageUtilisateur) {
        return requete(promptSysteme, messageUtilisateur, optionsJson).call().content();
    }

    @Override
    public String rediger(String promptSysteme, String messageUtilisateur) {
        return requete(promptSysteme, messageUtilisateur, optionsTexte).call().content();
    }

    @Override
    public Flux<String> redigerEnFlux(String promptSysteme, String messageUtilisateur) {
        return requete(promptSysteme, messageUtilisateur, optionsTexte).stream().content();
    }

    private ChatClient.ChatClientRequestSpec requete(String systeme, String message, ChatOptions options) {
        ChatClient.ChatClientRequestSpec r = chatClient.prompt().system(systeme).user(message);
        return options == null ? r : r.options(options);
    }
}
