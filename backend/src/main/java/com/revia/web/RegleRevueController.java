package com.revia.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.revia.domain.Gravite;
import com.revia.domain.RegleRevue;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.repository.RegleRevueRepository;
import com.revia.security.UtilisateurConnecte;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CRUD des règles de revue personnalisées (checklist du développeur). */
@RestController
@RequestMapping("/api/regles")
public class RegleRevueController {

    public record RegleRequest(
            @NotBlank @Size(max = 120) String nom,
            @Size(max = 500) String description,
            @NotNull Gravite gravite,
            boolean active) {
    }

    private final RegleRevueRepository regles;

    public RegleRevueController(RegleRevueRepository regles) {
        this.regles = regles;
    }

    @GetMapping
    public List<RegleRevue> lister(@AuthenticationPrincipal UtilisateurConnecte u) {
        return regles.findByUtilisateurIdOrderByCreeLeDesc(u.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegleRevue creer(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody RegleRequest req) {
        RegleRevue r = new RegleRevue(u.id());
        r.modifier(req.nom().trim(), req.description(), req.gravite(), req.active());
        return regles.save(r);
    }

    @PutMapping("/{id}")
    public RegleRevue modifier(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                               @Valid @RequestBody RegleRequest req) {
        RegleRevue r = trouver(u, id);
        r.modifier(req.nom().trim(), req.description(), req.gravite(), req.active());
        return regles.save(r);
    }

    /** Active / désactive la règle en un clic. */
    @PatchMapping("/{id}/basculer")
    public RegleRevue basculer(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        RegleRevue r = trouver(u, id);
        r.setActive(!r.isActive());
        return regles.save(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        regles.delete(trouver(u, id));
    }

    private RegleRevue trouver(UtilisateurConnecte u, Long id) {
        return regles.findByIdAndUtilisateurId(id, u.id()).orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE));
    }
}
