package com.revia.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.revia.domain.Gravite;
import com.revia.domain.Revue;
import com.revia.domain.Snippet;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.repository.RevueRepository;
import com.revia.repository.SnippetRepository;
import com.revia.security.UtilisateurConnecte;
import com.revia.service.Mapper;
import com.revia.web.dto.Dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CRUD de la bibliothèque de snippets : chaque développeur ne voit que ses propres snippets.
 * Chaque snippet porte aussi l'état de son dernier check (revue IA) et son historique de checks.
 */
@RestController
@RequestMapping("/api/snippets")
public class SnippetController {

    /** Score minimal (sans problème critique) pour qu'un snippet soit considéré comme validé. */
    static final int SCORE_VALIDATION = 80;

    public record SnippetRequest(
            @NotBlank @Size(max = 120) String titre,
            @NotBlank @Size(max = 30) String langage,
            @Size(max = 200) String tags,
            @NotBlank @Size(max = 100_000) String code) {
    }

    /**
     * Snippet + état du dernier check.
     * etat : non_checke | valide | a_corriger | modifie (code changé depuis le dernier check).
     */
    public record SnippetVue(Long id, String titre, String langage, String tags, String code,
                             LocalDateTime creeLe, LocalDateTime modifieLe,
                             int nbChecks, Long derniereRevueId, Integer dernierScore, Long dernierCritiques,
                             LocalDateTime dernierCheck, String etat) {
    }

    private final SnippetRepository snippets;
    private final RevueRepository revues;

    public SnippetController(SnippetRepository snippets, RevueRepository revues) {
        this.snippets = snippets;
        this.revues = revues;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SnippetVue> lister(@AuthenticationPrincipal UtilisateurConnecte u) {
        // Revues rattachées à un snippet, groupées par snippet (déjà triées de la plus récente à la plus ancienne)
        Map<Long, List<Revue>> parSnippet = revues.findByAuteurIdAndSnippetIdIsNotNullOrderByCreeLeDesc(u.id()).stream()
                .collect(Collectors.groupingBy(Revue::getSnippetId));
        return snippets.findByUtilisateurIdOrderByModifieLeDesc(u.id()).stream()
                .map(s -> vue(s, parSnippet.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public SnippetVue lire(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        Snippet s = trouver(u, id);
        return vue(s, revues.findByAuteurIdAndSnippetIdOrderByCreeLeDesc(u.id(), id));
    }

    /** Historique des checks du snippet, du plus récent au plus ancien. */
    @GetMapping("/{id}/checks")
    @Transactional(readOnly = true)
    public List<Dtos.RevueResume> checks(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        trouver(u, id);
        return revues.findByAuteurIdAndSnippetIdOrderByCreeLeDesc(u.id(), id).stream().map(Mapper::resume).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SnippetVue creer(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody SnippetRequest req) {
        Snippet s = new Snippet(u.id());
        s.modifier(req.titre().trim(), req.langage(), nettoyer(req.tags()), req.code());
        return vue(snippets.save(s), List.of());
    }

    @PutMapping("/{id}")
    @Transactional
    public SnippetVue modifier(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                               @Valid @RequestBody SnippetRequest req) {
        Snippet s = trouver(u, id);
        s.modifier(req.titre().trim(), req.langage(), nettoyer(req.tags()), req.code());
        return vue(snippets.save(s), revues.findByAuteurIdAndSnippetIdOrderByCreeLeDesc(u.id(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        snippets.delete(trouver(u, id));
    }

    /** Calcule l'état du snippet à partir de son dernier check. */
    static SnippetVue vue(Snippet s, List<Revue> checks) {
        if (checks.isEmpty()) {
            return new SnippetVue(s.getId(), s.getTitre(), s.getLangage(), s.getTags(), s.getCode(), s.getCreeLe(),
                    s.getModifieLe(), 0, null, null, null, null, "non_checke");
        }
        Revue der = checks.get(0);
        long critiques = der.getProblemes().stream()
                .filter(p -> !p.isFauxPositif() && p.getGravite() == Gravite.CRITIQUE).count();
        String etat;
        if (!normaliser(der.getCode()).equals(normaliser(s.getCode()))) {
            etat = "modifie";
        } else if ("corrigee".equals(der.statut()) || (der.getScore() >= SCORE_VALIDATION && critiques == 0)) {
            etat = "valide";
        } else {
            etat = "a_corriger";
        }
        return new SnippetVue(s.getId(), s.getTitre(), s.getLangage(), s.getTags(), s.getCode(), s.getCreeLe(),
                s.getModifieLe(), checks.size(), der.getId(), der.getScore(), critiques, der.getCreeLe(), etat);
    }

    private static String normaliser(String code) {
        return code == null ? "" : code.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    /** Un snippet n'est trouvé que s'il appartient à l'utilisateur connecté. */
    private Snippet trouver(UtilisateurConnecte u, Long id) {
        return snippets.findByIdAndUtilisateurId(id, u.id()).orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE));
    }

    private static String nettoyer(String tags) {
        return tags == null || tags.isBlank() ? null : tags.trim();
    }
}
