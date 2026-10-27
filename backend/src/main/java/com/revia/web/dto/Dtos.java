package com.revia.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Objets échangés en JSON avec le front-end Angular (requêtes validées par Bean Validation). */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Authentification / profil
    public record InscriptionRequest(
            @NotBlank(message = "Le nom est obligatoire.") @Size(min = 2, max = 80, message = "Le nom doit faire entre 2 et 80 caractères.") String nom,
            @NotBlank(message = "L'e-mail est obligatoire.") @Email(message = "E-mail invalide.") @Size(max = 160) String email,
            @NotBlank(message = "Le mot de passe est obligatoire.")
            @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Le mot de passe doit contenir au moins une lettre et un chiffre.")
            String motDePasse) {
    }

    public record ConnexionRequest(
            @NotBlank(message = "L'e-mail est obligatoire.") @Email(message = "E-mail invalide.") String email,
            @NotBlank(message = "Le mot de passe est obligatoire.") @Size(max = 72) String motDePasse) {
    }

    public record AuthResponse(String token, long expireDansSecondes, UtilisateurDto utilisateur) {
    }

    public record UtilisateurDto(Long id, String nom, String email, String role, Integer quotaJournalier,
                                 Integer revuesDuJour, Integer quotaAssistant, Integer assistantDuJour, boolean bloque,
                                 LocalDateTime creeLe, Long nbRevues, LocalDateTime derniereConnexion) {
    }

    public record ProfilRequest(
            @NotBlank(message = "Le nom est obligatoire.") @Size(min = 2, max = 80, message = "Le nom doit faire entre 2 et 80 caractères.") String nom) {
    }

    public record MotDePasseRequest(
            @NotBlank(message = "Saisissez votre mot de passe actuel.") String ancien,
            @NotBlank @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Le mot de passe doit contenir au moins une lettre et un chiffre.")
            String nouveau) {
    }

    // ---------- Revues
    public record RevueRequest(
            @Size(max = 160, message = "Le titre est limité à 160 caractères.") String titre,
            @Size(max = 20) String langage,
            @NotBlank(message = "Collez entre 1 et 2 000 lignes de code.")
            @Size(max = 200_000, message = "Collez entre 1 et 2 000 lignes de code.") String code,
            @Pattern(regexp = "complet|securite|qualite", message = "Mode inconnu.") String mode) {
    }

    public record RevueResume(Long id, String titre, String langage, LocalDateTime creeLe, int score,
                              long nbProblemes, long nbCritiques, String statut) {
    }

    public record RevueDetail(Long id, String titre, String langage, String code, int score, String resume, String mode,
                              LocalDateTime creeLe, long dureeMs, String fournisseur, String modele, String promptVersion,
                              int nbLignes, int nbMasquages, int problemesRejetes, String statut,
                              Map<String, Long> parGravite, Map<String, Long> parCategorie, List<ProblemeDto> problemes) {
    }

    public record ProblemeDto(Long id, int ligne, String categorie, String gravite, String titre, String explication,
                              String owasp, boolean fauxPositif, boolean corrige, Boolean utile,
                              CorrectionDto correction, long occurrencesPrecedentes) {
    }

    public record CorrectionDto(String codeAvant, String codeApres, String justification, boolean copiee) {
    }

    public record BooleenRequest(@NotNull Boolean valeur) {
    }

    public record CopieResponse(String code) {
    }

    public record PageDto<T>(List<T> contenu, int page, int taille, long total, int pages) {
    }

    // ---------- Assistant IA et outils IA
    public record MessageChat(
            @NotBlank @Pattern(regexp = "utilisateur|assistant", message = "Rôle inconnu.") String role,
            @NotBlank(message = "Écrivez votre question.") @Size(max = 8_000, message = "Message limité à 8 000 caractères.") String contenu) {
    }

    public record ChatRequest(
            @NotEmpty(message = "Écrivez votre question.") @Size(max = 40, message = "Conversation trop longue : recommencez-en une.")
            List<@Valid MessageChat> messages,
            @Size(max = 200_000) String code,
            @Size(max = 20) String langage) {
    }

    public record CompactageRequest(
            @NotEmpty(message = "Aucun message à compacter.") @Size(max = 40, message = "Conversation trop longue.")
            List<@Valid MessageChat> messages) {
    }

    public record OutilRequest(
            @NotBlank(message = "Collez votre code.") @Size(max = 200_000, message = "Code trop long.") String code,
            @Size(max = 20) String langage,
            @Size(max = 30) String cible) {
    }

    public record PromptUtilisateurRequest(
            @NotBlank(message = "Le titre est obligatoire.") @Size(min = 2, max = 80, message = "Le titre doit faire entre 2 et 80 caractères.") String titre,
            @NotBlank(message = "Le prompt est obligatoire.") @Size(max = 4_000, message = "Le prompt est limité à 4 000 caractères.") String contenu) {
    }

    /** « partage » : prompt officiel créé par un administrateur ; « modifiable » : faux pour un prompt partagé vu par un développeur. */
    public record PromptUtilisateurDto(Long id, String titre, String contenu, LocalDateTime creeLe, LocalDateTime modifieLe,
                                       boolean partage, boolean modifiable) {
    }

    public record AssistantReponse(String contenu, String outil, String langage, int nbMasquages, long dureeMs,
                                   String fournisseur, String modele) {
    }

    // ---------- Administration
    public record QuotaRequest(
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") int quota,
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") Integer quotaAssistant) {
    }

    public record RoleRequest(@NotBlank(message = "Le rôle est obligatoire.") @Pattern(regexp = "DEVELOPPEUR|ADMIN", message = "Rôle inconnu.") String role) {
    }

    public record ParametresQuotas(int quotaRevuesDefaut, int quotaAssistantDefaut) {
    }

    public record ParametresQuotasRequest(
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") int quotaRevuesDefaut,
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") int quotaAssistantDefaut,
            boolean appliquerATous) {
    }

    /** Mot de passe temporaire généré par la réinitialisation, affiché une seule fois à l'administrateur. */
    public record MotDePasseTemporaire(String motDePasse) {
    }

    public record UtilisateurCreationRequest(
            @NotBlank(message = "Le nom est obligatoire.") @Size(min = 2, max = 80, message = "Le nom doit faire entre 2 et 80 caractères.") String nom,
            @NotBlank(message = "L'e-mail est obligatoire.") @Email(message = "E-mail invalide.") @Size(max = 160) String email,
            @NotBlank(message = "Le mot de passe est obligatoire.")
            @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Le mot de passe doit contenir au moins une lettre et un chiffre.")
            String motDePasse,
            @NotBlank(message = "Le rôle est obligatoire.") @Pattern(regexp = "DEVELOPPEUR|ADMIN", message = "Rôle inconnu.") String role,
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") Integer quota,
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") Integer quotaAssistant) {
    }

    /** Le rôle n'est pas modifiable (héritage JPA) ; le mot de passe et le quota sont facultatifs. */
    public record UtilisateurModificationRequest(
            @NotBlank(message = "Le nom est obligatoire.") @Size(min = 2, max = 80, message = "Le nom doit faire entre 2 et 80 caractères.") String nom,
            @NotBlank(message = "L'e-mail est obligatoire.") @Email(message = "E-mail invalide.") @Size(max = 160) String email,
            @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Le mot de passe doit contenir au moins une lettre et un chiffre.")
            String motDePasse,
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") Integer quota,
            @Min(value = 0, message = "Le quota doit être positif.") @Max(value = 10_000, message = "Quota maximum : 10 000.") Integer quotaAssistant) {
    }

    // ---------- Administration assistée par IA (profil, quotas conseillés, alertes, commandes)
    public record Recurrent(String titre, long occurrences) {
    }

    public record ProfilCompetences(Long utilisateurId, String nom, int nbRevues, int scoreMoyen, int scoreInitial, int scoreRecent,
                                    String tendance, List<Recurrent> recurrents, Map<String, Long> parCategorie,
                                    Map<String, Long> parGravite, long nbMasquages, String synthese, String source) {
    }

    public record RecommandationQuota(Long utilisateurId, String nom, int quotaRevues, int quotaAssistant, int recommandeRevues,
                                      int recommandeAssistant, double moyenneRevuesJour, int picRevues, double moyenneAssistantJour,
                                      int picAssistant, int joursActifs, String explication, boolean modificationUtile) {
    }

    public record RecommandationsQuotas(List<RecommandationQuota> recommandations, String commentaire, String source) {
    }

    public record Alerte(String niveau, String type, Long utilisateurId, String nom, String message, String detail, LocalDateTime date) {
    }

    public record Alertes(List<Alerte> alertes, String resume, String source) {
    }

    public record CommandeRequest(
            @NotBlank(message = "Écrivez une commande.") @Size(max = 500, message = "Commande limitée à 500 caractères.") String commande) {
    }

    public record ActionPlan(String type, Long utilisateurId, String nom, String libelle, String role, Integer quota, Integer quotaAssistant) {
    }

    public record PlanAdmin(String interpretation, String source, List<ActionPlan> actions, List<String> avertissements) {
    }

    public record ActionRequest(
            @NotBlank @Pattern(regexp = "BLOQUER|DEBLOQUER|CHANGER_ROLE|DEFINIR_QUOTA", message = "Action inconnue.") String type,
            @NotNull Long utilisateurId,
            @Pattern(regexp = "DEVELOPPEUR|ADMIN", message = "Rôle inconnu.") String role,
            @Min(0) @Max(10_000) Integer quota,
            @Min(0) @Max(10_000) Integer quotaAssistant) {
    }

    public record ExecutionRequest(@NotEmpty(message = "Aucune action à exécuter.") @Size(max = 50, message = "50 actions maximum.")
                                   List<@Valid ActionRequest> actions) {
    }

    public record ResultatAction(String type, Long utilisateurId, String nom, boolean ok, String message) {
    }

    public record ResultatExecution(List<ResultatAction> resultats) {
    }

    public record PromptRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Version : lettres, chiffres, . _ - uniquement.") String version,
            @NotBlank @Size(max = 20_000) String contenu) {
    }

    public record PromptDto(Long id, String version, String contenu, boolean actif, LocalDateTime creeLe) {
    }

    public record SystemeInfo(String application, String version, String fournisseur, String modele, boolean disponible,
                              double temperature, int timeoutSec, int quotaParDefaut, int quotaAssistantParDefaut, int maxLignes,
                              int analysesParMinute, int connexionsParMinute, int contexteMaxTokens) {
    }
}
