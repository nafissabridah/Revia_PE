package com.revia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.revia.domain.PromptUtilisateur;

/** Les prompts personnels sont privés à leur auteur ; seuls les prompts « partagés » sont visibles de tous. */
public interface PromptUtilisateurRepository extends JpaRepository<PromptUtilisateur, Long> {

    @Query("select p from PromptUtilisateur p where p.auteurId = :auteurId or p.partage = true order by p.titre")
    List<PromptUtilisateur> visiblesPar(@Param("auteurId") Long auteurId);

    /** Un prompt personnel de l'auteur (jamais un prompt partagé : seul l'administrateur le modifie). */
    @Query("select p from PromptUtilisateur p where p.id = :id and p.auteurId = :auteurId and p.partage = false")
    Optional<PromptUtilisateur> findPersonnel(@Param("id") Long id, @Param("auteurId") Long auteurId);

    List<PromptUtilisateur> findByPartageTrueOrderByTitreAsc();

    Optional<PromptUtilisateur> findByIdAndPartageTrue(Long id);

    @Query("select count(p) from PromptUtilisateur p where p.auteurId = :auteurId and p.partage = false")
    long countPersonnels(@Param("auteurId") Long auteurId);
}
