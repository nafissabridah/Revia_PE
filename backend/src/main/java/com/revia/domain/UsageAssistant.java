package com.revia.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Une requête à l'assistant IA ou à un outil IA ; sert à compter le quota journalier de l'assistant. */
@Entity
@Table(name = "usage_assistant")
public class UsageAssistant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Column(nullable = false, length = 20)
    private String outil;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    protected UsageAssistant() {
    }

    public UsageAssistant(Long utilisateurId, String outil) {
        this.utilisateurId = utilisateurId;
        this.outil = outil;
    }

    public Long getId() {
        return id;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public String getOutil() {
        return outil;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }
}
