package com.revia.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Règle de revue personnalisée définie par un développeur (ex. « Pas de System.out.println »). */
@Entity
@Table(name = "regle_revue")
public class RegleRevue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gravite gravite;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    protected RegleRevue() {
    }

    public RegleRevue(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public void modifier(String nom, String description, Gravite gravite, boolean active) {
        this.nom = nom;
        this.description = description;
        this.gravite = gravite;
        this.active = active;
    }

    public Long getId() { return id; }
    public Long getUtilisateurId() { return utilisateurId; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public Gravite getGravite() { return gravite; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreeLe() { return creeLe; }
}
