package com.revia.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Extrait de code sauvegardé par un développeur dans sa bibliothèque personnelle. */
@Entity
@Table(name = "snippet")
public class Snippet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Column(nullable = false, length = 120)
    private String titre;

    @Column(nullable = false, length = 30)
    private String langage;

    @Column(length = 200)
    private String tags;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe = LocalDateTime.now();

    protected Snippet() {
    }

    public Snippet(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    /** Applique les champs modifiables et met à jour la date de modification. */
    public void modifier(String titre, String langage, String tags, String code) {
        this.titre = titre;
        this.langage = langage;
        this.tags = tags;
        this.code = code;
        this.modifieLe = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUtilisateurId() { return utilisateurId; }
    public String getTitre() { return titre; }
    public String getLangage() { return langage; }
    public String getTags() { return tags; }
    public String getCode() { return code; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public LocalDateTime getModifieLe() { return modifieLe; }
}
