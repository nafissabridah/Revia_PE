package com.revia.web;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.revia.ai.AssistantIA;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.security.UtilisateurConnecte;
import com.revia.service.QuotaService;
import com.revia.web.dto.Dtos;

import jakarta.validation.Valid;
import reactor.core.Disposable;

/**
 * Assistant IA (chat) et boîte à outils IA : tests, documentation, explication, commit, refactoring, conversion.
 * Deux formes pour chaque appel : réponse complète (JSON) ou diffusion en direct (« /flux », Server-Sent Events)
 * pour afficher le texte dès que le modèle l'écrit.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);

    private final AssistantIA assistant;
    private final QuotaService quotas;

    public AssistantController(AssistantIA assistant, QuotaService quotas) {
        this.assistant = assistant;
        this.quotas = quotas;
    }

    @PostMapping("/chat")
    public Dtos.AssistantReponse discuter(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody Dtos.ChatRequest req) {
        AssistantIA.Demande d = chat(req);
        quotas.consommerAssistant(u.id(), "chat");
        return dto(assistant.repondre(d), "chat");
    }

    /** Résume la conversation pour libérer du contexte ; compte comme une requête assistant. */
    @PostMapping("/compacter")
    public Dtos.AssistantReponse compacter(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody Dtos.CompactageRequest req) {
        AssistantIA.Demande d = assistant.preparerCompactage(
                req.messages().stream().map(m -> new AssistantIA.Message(m.role(), m.contenu())).toList());
        quotas.consommerAssistant(u.id(), "compactage");
        return dto(assistant.repondre(d), "compactage");
    }

    @PostMapping("/outils/{outil}")
    public Dtos.AssistantReponse outil(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable String outil,
                                       @Valid @RequestBody Dtos.OutilRequest req) {
        AssistantIA.Outil o = AssistantIA.Outil.depuis(outil);
        AssistantIA.Demande d = assistant.preparerOutil(o, req.code(), req.langage(), req.cible());
        quotas.consommerAssistant(u.id(), o.id());
        return dto(assistant.repondre(d), o.id());
    }

    @PostMapping(value = "/chat/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter discuterEnDirect(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody Dtos.ChatRequest req) {
        AssistantIA.Demande d = chat(req);
        quotas.consommerAssistant(u.id(), "chat");
        return diffuser(d, "chat");
    }

    @PostMapping(value = "/outils/{outil}/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter outilEnDirect(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable String outil,
                                    @Valid @RequestBody Dtos.OutilRequest req) {
        AssistantIA.Outil o = AssistantIA.Outil.depuis(outil);
        AssistantIA.Demande d = assistant.preparerOutil(o, req.code(), req.langage(), req.cible());
        quotas.consommerAssistant(u.id(), o.id());
        return diffuser(d, o.id());
    }

    private AssistantIA.Demande chat(Dtos.ChatRequest req) {
        return assistant.preparerChat(
                req.messages().stream().map(m -> new AssistantIA.Message(m.role(), m.contenu())).toList(),
                req.code(), req.langage());
    }

    /**
     * Événements envoyés : « morceau » {t} (texte au fil de l'eau), puis « fin » {dureeMs, modele…}
     * ou « erreur » {code, message} avec le même message clair que l'API classique.
     */
    private SseEmitter diffuser(AssistantIA.Demande d, String outil) {
        SseEmitter emetteur = new SseEmitter(120_000L);
        Disposable abonnement = assistant.diffuser(d).subscribe(
                morceau -> envoyer(emetteur, "morceau", Map.of("t", morceau)),
                erreur -> {
                    ErrorCode code = erreur instanceof ApiException a ? a.getCode() : ErrorCode.LLM_INDISPONIBLE;
                    String message = erreur instanceof ApiException a ? a.getMessageUtilisateur() : code.message();
                    log.warn("Diffusion IA interrompue ({}) : {}", outil, erreur.getMessage());
                    envoyer(emetteur, "erreur", Map.of("code", code.name(), "message", message));
                    emetteur.complete();
                },
                () -> {
                    envoyer(emetteur, "fin", Map.of("outil", outil, "langage", d.langage() == null ? "" : d.langage(),
                            "nbMasquages", d.nbMasquages(), "dureeMs", System.currentTimeMillis() - d.debut(),
                            "fournisseur", assistant.fournisseur(), "modele", assistant.modele()));
                    emetteur.complete();
                });
        emetteur.onTimeout(abonnement::dispose);
        emetteur.onError(e -> abonnement.dispose());
        return emetteur;
    }

    private static void envoyer(SseEmitter e, String evenement, Object donnees) {
        try {
            e.send(SseEmitter.event().name(evenement).data(donnees, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            // Le navigateur a fermé la connexion : rien à faire.
        }
    }

    private static Dtos.AssistantReponse dto(AssistantIA.Reponse r, String outil) {
        return new Dtos.AssistantReponse(r.contenu(), outil, r.langage(), r.nbMasquages(), r.dureeMs(), r.fournisseur(), r.modele());
    }
}
