package com.revia.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Prompt réutilisable dans l'Assistant IA (distinct du prompt système de revue).
 * Personnel par défaut ; « partagé » quand un administrateur l'a publié pour tous les développeurs.
 */
@Entity
@Table(name = "prompt_utilisateur")
public class PromptUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_id", nullable = false)
    private Long auteurId;

    @Column(nullable = false, length = 80)
    private String titre;

    @Column(nullable = false, length = 4000)
    private String contenu;

    @Column(nullable = false)
    private boolean partage;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe = LocalDateTime.now();

    protected PromptUtilisateur() {
    }

    public PromptUtilisateur(Long auteurId, String titre, String contenu) {
        this(auteurId, titre, contenu, false);
    }

    public PromptUtilisateur(Long auteurId, String titre, String contenu, boolean partage) {
        this.auteurId = auteurId;
        this.titre = titre;
        this.contenu = contenu;
        this.partage = partage;
    }

    public void modifier(String titre, String contenu) {
        this.titre = titre;
        this.contenu = contenu;
        this.modifieLe = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAuteurId() {
        return auteurId;
    }

    public String getTitre() {
        return titre;
    }

    public String getContenu() {
        return contenu;
    }

    public boolean isPartage() {
        return partage;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public LocalDateTime getModifieLe() {
        return modifieLe;
    }
}
