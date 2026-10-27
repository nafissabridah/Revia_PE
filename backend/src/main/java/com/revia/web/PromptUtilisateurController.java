package com.revia.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.revia.security.UtilisateurConnecte;
import com.revia.service.PromptUtilisateurService;
import com.revia.web.dto.Dtos;

import jakarta.validation.Valid;

/** « Mes prompts » : bibliothèque personnelle, réutilisable dans l'Assistant IA. */
@RestController
@RequestMapping("/api/mes-prompts")
public class PromptUtilisateurController {

    private final PromptUtilisateurService prompts;

    public PromptUtilisateurController(PromptUtilisateurService prompts) {
        this.prompts = prompts;
    }

    @GetMapping
    public List<Dtos.PromptUtilisateurDto> lister(@AuthenticationPrincipal UtilisateurConnecte u) {
        return prompts.lister(u.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.PromptUtilisateurDto creer(@AuthenticationPrincipal UtilisateurConnecte u,
                                           @Valid @RequestBody Dtos.PromptUtilisateurRequest req) {
        return prompts.creer(u.id(), req);
    }

    @PutMapping("/{id}")
    public Dtos.PromptUtilisateurDto modifier(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                                              @Valid @RequestBody Dtos.PromptUtilisateurRequest req) {
        return prompts.modifier(u.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        prompts.supprimer(u.id(), id);
    }
}
