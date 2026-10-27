package com.revia.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.revia.config.ReviaProperties;
import com.revia.domain.Parametre;
import com.revia.repository.ParametreRepository;

/** Quotas par défaut des nouveaux comptes : valeur en base si l'administrateur l'a modifiée, sinon la configuration. */
@Service
public class ParametreService {

    static final String QUOTA_REVUES = "quota_revues_defaut";
    static final String QUOTA_ASSISTANT = "quota_assistant_defaut";

    private final ParametreRepository parametres;
    private final ReviaProperties props;

    public ParametreService(ParametreRepository parametres, ReviaProperties props) {
        this.parametres = parametres;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public int quotaRevuesDefaut() {
        return lire(QUOTA_REVUES, props.quotas().revuesParJour());
    }

    @Transactional(readOnly = true)
    public int quotaAssistantDefaut() {
        return lire(QUOTA_ASSISTANT, props.quotas().assistantParJour());
    }

    @Transactional
    public void definirQuotasDefaut(int revues, int assistant) {
        ecrire(QUOTA_REVUES, revues);
        ecrire(QUOTA_ASSISTANT, assistant);
    }

    private int lire(String cle, int defaut) {
        return parametres.findById(cle).map(p -> {
            try {
                return Integer.parseInt(p.getValeur());
            } catch (NumberFormatException e) {
                return defaut;
            }
        }).orElse(defaut);
    }

    private void ecrire(String cle, int valeur) {
        Parametre p = parametres.findById(cle).orElseGet(() -> new Parametre(cle, ""));
        p.setValeur(String.valueOf(valeur));
        parametres.save(p);
    }
}
