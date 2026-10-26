package com.revia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

/** Développeur : soumet du code, consulte ses revues, son historique et son tableau de bord. */
@Entity
@DiscriminatorValue("DEVELOPPEUR")
public class Developpeur extends Utilisateur {

    @Column(name = "quota_journalier", nullable = false)
    private int quotaJournalier = 50;

    @Column(name = "quota_assistant", nullable = false)
    private int quotaAssistant = 100;

    /** Calculé à la volée (nombre de revues créées aujourd'hui), jamais stocké. */
    @Transient
    private int revuesDuJour;

    /** Calculé à la volée (requêtes assistant / outils IA d'aujourd'hui), jamais stocké. */
    @Transient
    private int assistantDuJour;

    protected Developpeur() {
    }

    public Developpeur(String nom, String email, String motDePasseHash, int quotaJournalier) {
        super(nom, email, motDePasseHash);
        this.quotaJournalier = quotaJournalier;
    }

    @Override
    public String role() {
        return "DEVELOPPEUR";
    }

    /** Crée la revue d'un extrait de code (l'analyse IA est ensuite faite par AnalyseurIA). */
    public Revue soumettreCode(String titre, String langage, String code, String mode) {
        return new Revue(this, titre, langage, code, mode);
    }

    /** Vrai si le développeur a déjà consommé toutes ses revues du jour. */
    public boolean quotaAtteint() {
        return revuesDuJour >= quotaJournalier;
    }

    /** Vrai si le développeur a déjà consommé toutes ses requêtes assistant / outils du jour. */
    public boolean assistantAtteint() {
        return assistantDuJour >= quotaAssistant;
    }

    public int getQuotaAssistant() {
        return quotaAssistant;
    }

    public void setQuotaAssistant(int quotaAssistant) {
        this.quotaAssistant = quotaAssistant;
    }

    public int getAssistantDuJour() {
        return assistantDuJour;
    }

    public void setAssistantDuJour(int assistantDuJour) {
        this.assistantDuJour = assistantDuJour;
    }

    public int getQuotaJournalier() {
        return quotaJournalier;
    }

    public void setQuotaJournalier(int quotaJournalier) {
        this.quotaJournalier = quotaJournalier;
    }

    public int getRevuesDuJour() {
        return revuesDuJour;
    }

    public void setRevuesDuJour(int revuesDuJour) {
        this.revuesDuJour = revuesDuJour;
    }
}
