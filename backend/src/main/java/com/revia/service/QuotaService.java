package com.revia.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.revia.domain.Developpeur;
import com.revia.domain.UsageAssistant;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.repository.RevueRepository;
import com.revia.repository.UsageAssistantRepository;
import com.revia.repository.UtilisateurRepository;

/**
 * Quotas journaliers réinitialisés à minuit, vérifiés AVANT tout appel au LLM :
 * revues (50 par défaut) et requêtes assistant / outils IA (100 par défaut).
 */
@Service
public class QuotaService {

    private final RevueRepository revues;
    private final UsageAssistantRepository usages;
    private final UtilisateurRepository utilisateurs;

    public QuotaService(RevueRepository revues, UsageAssistantRepository usages, UtilisateurRepository utilisateurs) {
        this.revues = revues;
        this.usages = usages;
        this.utilisateurs = utilisateurs;
    }

    public static LocalDateTime debutDuJour() {
        return LocalDate.now().atStartOfDay();
    }

    /** Renseigne les champs calculés revuesDuJour et assistantDuJour du développeur. */
    public Developpeur charger(Developpeur dev) {
        LocalDateTime debut = debutDuJour();
        dev.setRevuesDuJour((int) revues.countByAuteurIdAndCreeLeGreaterThanEqual(dev.getId(), debut));
        dev.setAssistantDuJour((int) usages.countByUtilisateurIdAndCreeLeGreaterThanEqual(dev.getId(), debut));
        return dev;
    }

    public void verifier(Developpeur dev) {
        charger(dev);
        if (dev.quotaAtteint()) {
            throw new ApiException(ErrorCode.QUOTA_UTILISATEUR,
                    "Vous avez atteint vos " + dev.getQuotaJournalier() + " revues du jour.");
        }
    }

    /** Vérifie puis enregistre une requête assistant / outil ; sans effet pour un administrateur. */
    @Transactional
    public void consommerAssistant(Long utilisateurId, String outil) {
        utilisateurs.findById(utilisateurId).filter(Developpeur.class::isInstance).map(Developpeur.class::cast).ifPresent(dev -> {
            charger(dev);
            if (dev.assistantAtteint()) {
                throw new ApiException(ErrorCode.QUOTA_UTILISATEUR,
                        "Vous avez atteint vos " + dev.getQuotaAssistant() + " requêtes assistant du jour.");
            }
            usages.save(new UsageAssistant(utilisateurId, outil));
        });
    }
}
