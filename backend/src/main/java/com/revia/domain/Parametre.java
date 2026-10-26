package com.revia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Paramètre d'exploitation modifiable depuis la page Admin (clé / valeur). */
@Entity
@Table(name = "parametre")
public class Parametre {

    @Id
    @Column(length = 40)
    private String cle;

    @Column(nullable = false, length = 100)
    private String valeur;

    protected Parametre() {
    }

    public Parametre(String cle, String valeur) {
        this.cle = cle;
        this.valeur = valeur;
    }

    public String getCle() {
        return cle;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }
}
