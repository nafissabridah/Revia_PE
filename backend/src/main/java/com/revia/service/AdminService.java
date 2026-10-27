package com.revia.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.revia.domain.Administrateur;
import com.revia.domain.Developpeur;
import com.revia.domain.Utilisateur;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.repository.RevueRepository;
import com.revia.repository.UtilisateurRepository;
import com.revia.web.dto.Dtos;

/** Gérer utilisateurs, rôles, mots de passe et quotas (cas d'utilisation de l'administrateur). */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final String LETTRES = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHIFFRES = "23456789";
    private static final SecureRandom ALEA = new SecureRandom();

    private final UtilisateurRepository utilisateurs;
    private final RevueRepository revues;
    private final QuotaService quotas;
    private final PasswordEncoder encoder;
    private final ParametreService parametres;

    public AdminService(UtilisateurRepository utilisateurs, RevueRepository revues, QuotaService quotas,
                        PasswordEncoder encoder, ParametreService parametres) {
        this.utilisateurs = utilisateurs;
        this.revues = revues;
        this.quotas = quotas;
        this.encoder = encoder;
        this.parametres = parametres;
    }

    /** Administrateur.gererUtilisateurs() : liste de tous les comptes avec leur consommation du jour. */
    @Transactional(readOnly = true)
    public List<Dtos.UtilisateurDto> gererUtilisateurs() {
        return utilisateurs.findAll(Sort.by("id")).stream().map(this::vue).toList();
    }

    @Transactional
    public Dtos.UtilisateurDto creerUtilisateur(Long adminId, Dtos.UtilisateurCreationRequest req) {
        admin(adminId);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (utilisateurs.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_DEJA_UTILISE);
        }
        String nom = req.nom().trim();
        String hash = encoder.encode(req.motDePasse());
        Utilisateur u;
        if ("ADMIN".equals(req.role())) {
            u = new Administrateur(nom, email, hash);
        } else {
            Developpeur d = new Developpeur(nom, email, hash, req.quota() != null ? req.quota() : parametres.quotaRevuesDefaut());
            d.setQuotaAssistant(req.quotaAssistant() != null ? req.quotaAssistant() : parametres.quotaAssistantDefaut());
            u = d;
        }
        u = utilisateurs.save(u);
        log.info("Admin {} : compte {} créé ({})", adminId, u.getId(), u.role());
        return vue(u);
    }

    @Transactional
    public Dtos.UtilisateurDto modifierUtilisateur(Long adminId, Long userId, Dtos.UtilisateurModificationRequest req) {
        Administrateur admin = admin(adminId);
        Utilisateur u = trouver(userId);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equalsIgnoreCase(u.getEmail()) && utilisateurs.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_DEJA_UTILISE);
        }
        u.modifierProfil(req.nom().trim());
        u.modifierEmail(email);
        if (req.motDePasse() != null && !req.motDePasse().isBlank()) {
            u.setMotDePasseHash(encoder.encode(req.motDePasse()));
        }
        if (u instanceof Developpeur d) {
            if (req.quota() != null) {
                admin.definirQuota(d, req.quota());
            }
            if (req.quotaAssistant() != null) {
                admin.definirQuotaAssistant(d, req.quotaAssistant());
            }
        }
        log.info("Admin {} : compte {} modifié", adminId, userId);
        return vue(u);
    }

    @Transactional
    public void supprimerUtilisateur(Long adminId, Long userId) {
        admin(adminId);
        if (adminId.equals(userId)) {
            throw new ApiException(ErrorCode.VALIDATION, "Vous ne pouvez pas supprimer votre propre compte.");
        }
        Utilisateur u = trouver(userId);
        utilisateurs.delete(u);
        utilisateurs.flush();
        log.info("Admin {} : compte {} supprimé avec ses revues", adminId, userId);
    }

    /** Change le rôle d'un compte (le rôle de l'administrateur connecté n'est pas modifiable). */
    @Transactional
    public Dtos.UtilisateurDto changerRole(Long adminId, Long userId, String role) {
        admin(adminId);
        if (adminId.equals(userId)) {
            throw new ApiException(ErrorCode.VALIDATION, "Vous ne pouvez pas modifier votre propre rôle.");
        }
        Utilisateur u = trouver(userId);
        if (u.role().equals(role)) {
            return vue(u);
        }
        if ("ADMIN".equals(role)) {
            utilisateurs.passerAdministrateur(userId);
        } else {
            utilisateurs.passerDeveloppeur(userId, parametres.quotaRevuesDefaut(), parametres.quotaAssistantDefaut());
        }
        log.info("Admin {} : compte {} passé au rôle {}", adminId, userId, role);
        return vue(trouver(userId));
    }

    /** Génère un mot de passe temporaire (affiché une seule fois) ; l'utilisateur le change ensuite dans Paramètres. */
    @Transactional
    public Dtos.MotDePasseTemporaire reinitialiserMotDePasse(Long adminId, Long userId) {
        admin(adminId);
        Utilisateur u = trouver(userId);
        String motDePasse = genererMotDePasse();
        u.setMotDePasseHash(encoder.encode(motDePasse));
        log.info("Admin {} : mot de passe du compte {} réinitialisé", adminId, userId);
        return new Dtos.MotDePasseTemporaire(motDePasse);
    }

    @Transactional
    public Dtos.UtilisateurDto definirQuota(Long adminId, Long devId, int quota, Integer quotaAssistant) {
        Administrateur admin = admin(adminId);
        Utilisateur u = trouver(devId);
        if (!(u instanceof Developpeur d)) {
            throw new ApiException(ErrorCode.VALIDATION, "Le quota ne s'applique qu'aux comptes développeur.");
        }
        admin.definirQuota(d, quota);
        if (quotaAssistant != null) {
            admin.definirQuotaAssistant(d, quotaAssistant);
        }
        log.info("Admin {} : quotas de l'utilisateur {} fixés à {} revues / {} assistant", adminId, devId, quota, d.getQuotaAssistant());
        return vue(d);
    }

    @Transactional
    public Dtos.UtilisateurDto bloquerCompte(Long adminId, Long userId, boolean bloque) {
        Administrateur admin = admin(adminId);
        Utilisateur u = trouver(userId);
        admin.bloquerCompte(u, bloque);
        log.info("Admin {} : compte {} {}", adminId, userId, bloque ? "bloqué" : "débloqué");
        return vue(u);
    }

    @Transactional(readOnly = true)
    public Dtos.ParametresQuotas parametresQuotas() {
        return new Dtos.ParametresQuotas(parametres.quotaRevuesDefaut(), parametres.quotaAssistantDefaut());
    }

    /** Quotas par défaut des nouveaux comptes ; « appliquerATous » les impose aussi aux développeurs existants. */
    @Transactional
    public Dtos.ParametresQuotas definirParametresQuotas(Long adminId, Dtos.ParametresQuotasRequest req) {
        admin(adminId);
        parametres.definirQuotasDefaut(req.quotaRevuesDefaut(), req.quotaAssistantDefaut());
        if (req.appliquerATous()) {
            int n = utilisateurs.appliquerQuotas(req.quotaRevuesDefaut(), req.quotaAssistantDefaut());
            log.info("Admin {} : quotas {} / {} appliqués à {} développeur(s)", adminId, req.quotaRevuesDefaut(), req.quotaAssistantDefaut(), n);
        } else {
            log.info("Admin {} : quotas par défaut fixés à {} / {}", adminId, req.quotaRevuesDefaut(), req.quotaAssistantDefaut());
        }
        return parametresQuotas();
    }

    private Utilisateur trouver(Long id) {
        return utilisateurs.findById(id).orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE, "Utilisateur introuvable."));
    }

    private Dtos.UtilisateurDto vue(Utilisateur u) {
        if (u instanceof Developpeur d) {
            quotas.charger(d);
        }
        return Mapper.utilisateur(u, revues.countByAuteurId(u.getId()));
    }

    private Administrateur admin(Long id) {
        return utilisateurs.findById(id).filter(Administrateur.class::isInstance).map(Administrateur.class::cast)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERDIT));
    }

    /** 12 caractères sans ambiguïté visuelle, avec au moins une lettre et un chiffre (règle de l'inscription). */
    private static String genererMotDePasse() {
        String tout = LETTRES + CHIFFRES;
        List<Character> c = new ArrayList<>();
        c.add(LETTRES.charAt(ALEA.nextInt(LETTRES.length())));
        c.add(CHIFFRES.charAt(ALEA.nextInt(CHIFFRES.length())));
        for (int i = 0; i < 10; i++) {
            c.add(tout.charAt(ALEA.nextInt(tout.length())));
        }
        Collections.shuffle(c, ALEA);
        StringBuilder sb = new StringBuilder();
        c.forEach(sb::append);
        return sb.toString();
    }
}
