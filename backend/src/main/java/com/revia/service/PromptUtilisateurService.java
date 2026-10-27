package com.revia.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.revia.domain.PromptUtilisateur;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.repository.PromptUtilisateurRepository;
import com.revia.web.dto.Dtos;

/**
 * Bibliothèque de prompts : prompts personnels (créer, modifier, supprimer, toujours limités à l'auteur)
 * et prompts partagés publiés par un administrateur (visibles de tous, modifiables par les administrateurs seulement).
 */
@Service
public class PromptUtilisateurService {

    public static final int MAX_PAR_UTILISATEUR = 50;

    private final PromptUtilisateurRepository prompts;

    public PromptUtilisateurService(PromptUtilisateurRepository prompts) {
        this.prompts = prompts;
    }

    /** Prompts personnels de l'utilisateur + prompts partagés. */
    @Transactional(readOnly = true)
    public List<Dtos.PromptUtilisateurDto> lister(Long auteurId) {
        return prompts.visiblesPar(auteurId).stream().map(p -> dto(p, auteurId)).toList();
    }

    @Transactional
    public Dtos.PromptUtilisateurDto creer(Long auteurId, Dtos.PromptUtilisateurRequest req) {
        if (prompts.countPersonnels(auteurId) >= MAX_PAR_UTILISATEUR) {
            throw new ApiException(ErrorCode.VALIDATION,
                    "Vous pouvez enregistrer " + MAX_PAR_UTILISATEUR + " prompts au maximum : supprimez-en un avant d'en ajouter.");
        }
        return dto(prompts.save(new PromptUtilisateur(auteurId, req.titre().trim(), req.contenu().trim())), auteurId);
    }

    @Transactional
    public Dtos.PromptUtilisateurDto modifier(Long auteurId, Long id, Dtos.PromptUtilisateurRequest req) {
        PromptUtilisateur p = personnel(auteurId, id);
        p.modifier(req.titre().trim(), req.contenu().trim());
        return dto(p, auteurId);
    }

    @Transactional
    public void supprimer(Long auteurId, Long id) {
        prompts.delete(personnel(auteurId, id));
    }

    // ---------- prompts partagés (administrateur)

    @Transactional(readOnly = true)
    public List<Dtos.PromptUtilisateurDto> listerPartages(Long adminId) {
        return prompts.findByPartageTrueOrderByTitreAsc().stream().map(p -> dto(p, adminId)).toList();
    }

    @Transactional
    public Dtos.PromptUtilisateurDto creerPartage(Long adminId, Dtos.PromptUtilisateurRequest req) {
        return dto(prompts.save(new PromptUtilisateur(adminId, req.titre().trim(), req.contenu().trim(), true)), adminId);
    }

    @Transactional
    public Dtos.PromptUtilisateurDto modifierPartage(Long adminId, Long id, Dtos.PromptUtilisateurRequest req) {
        PromptUtilisateur p = partage(id);
        p.modifier(req.titre().trim(), req.contenu().trim());
        return dto(p, adminId);
    }

    @Transactional
    public void supprimerPartage(Long id) {
        prompts.delete(partage(id));
    }

    private PromptUtilisateur personnel(Long auteurId, Long id) {
        return prompts.findPersonnel(id, auteurId).orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE, "Prompt introuvable."));
    }

    private PromptUtilisateur partage(Long id) {
        return prompts.findByIdAndPartageTrue(id).orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE, "Prompt introuvable."));
    }

    private static Dtos.PromptUtilisateurDto dto(PromptUtilisateur p, Long demandeurId) {
        boolean modifiable = p.isPartage() ? false : p.getAuteurId().equals(demandeurId);
        return new Dtos.PromptUtilisateurDto(p.getId(), p.getTitre(), p.getContenu(), p.getCreeLe(), p.getModifieLe(), p.isPartage(), modifiable);
    }
}
