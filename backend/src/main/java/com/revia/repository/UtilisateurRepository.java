package com.revia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.revia.domain.Administrateur;
import com.revia.domain.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select count(a) from Administrateur a")
    long countAdministrateurs();

    @Query("select a from Administrateur a order by a.id")
    List<Administrateur> findAdministrateurs();

    /** Applique les quotas par défaut à tous les développeurs. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Developpeur d set d.quotaJournalier = :revues, d.quotaAssistant = :assistant")
    int appliquerQuotas(@Param("revues") int revues, @Param("assistant") int assistant);

    /**
     * Changement de rôle : le type est la colonne discriminante de l'héritage SINGLE_TABLE,
     * JPA ne sait pas transformer une entité en une autre, d'où la requête native.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update utilisateur set type = 'ADMINISTRATEUR' where id = :id", nativeQuery = true)
    int passerAdministrateur(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update utilisateur set type = 'DEVELOPPEUR', quota_journalier = :revues, quota_assistant = :assistant where id = :id",
            nativeQuery = true)
    int passerDeveloppeur(@Param("id") Long id, @Param("revues") int revues, @Param("assistant") int assistant);
}
