package com.revia.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.revia.ai.PromptService;
import com.revia.security.UtilisateurConnecte;
import com.revia.service.AdminIAService;
import com.revia.service.AdminService;
import com.revia.service.Mapper;
import com.revia.service.PromptUtilisateurService;
import com.revia.web.dto.Dtos;

import jakarta.validation.Valid;

/** Réservé au rôle ADMIN (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final PromptService prompts;
    private final PromptUtilisateurService promptsUtilisateur;
    private final AdminIAService adminIA;

    public AdminController(AdminService admin, PromptService prompts, PromptUtilisateurService promptsUtilisateur, AdminIAService adminIA) {
        this.adminIA = adminIA;
        this.admin = admin;
        this.prompts = prompts;
        this.promptsUtilisateur = promptsUtilisateur;
    }

    @GetMapping("/utilisateurs")
    public List<Dtos.UtilisateurDto> utilisateurs() {
        return admin.gererUtilisateurs();
    }

    @PostMapping("/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.UtilisateurDto creer(@AuthenticationPrincipal UtilisateurConnecte u,
                                     @Valid @RequestBody Dtos.UtilisateurCreationRequest req) {
        return admin.creerUtilisateur(u.id(), req);
    }

    @PutMapping("/utilisateurs/{id}")
    public Dtos.UtilisateurDto modifier(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                                        @Valid @RequestBody Dtos.UtilisateurModificationRequest req) {
        return admin.modifierUtilisateur(u.id(), id, req);
    }

    @DeleteMapping("/utilisateurs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        admin.supprimerUtilisateur(u.id(), id);
    }

    @GetMapping("/utilisateurs/{id}/profil")
    public Dtos.ProfilCompetences profil(@PathVariable Long id) {
        return adminIA.profil(id);
    }

    @GetMapping("/recommandations-quotas")
    public Dtos.RecommandationsQuotas recommandationsQuotas() {
        return adminIA.recommandations();
    }

    @GetMapping("/alertes")
    public Dtos.Alertes alertes(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean resume) {
        return adminIA.alertes(resume);
    }

    @PostMapping("/assistant")
    public Dtos.PlanAdmin assistant(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody Dtos.CommandeRequest req) {
        return adminIA.interpreter(u.id(), req.commande());
    }

    @PostMapping("/assistant/executer")
    public Dtos.ResultatExecution executer(@AuthenticationPrincipal UtilisateurConnecte u, @Valid @RequestBody Dtos.ExecutionRequest req) {
        return adminIA.executer(u.id(), req);
    }

    @PutMapping("/utilisateurs/{id}/quota")
    public Dtos.UtilisateurDto quota(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                                     @Valid @RequestBody Dtos.QuotaRequest req) {
        return admin.definirQuota(u.id(), id, req.quota(), req.quotaAssistant());
    }

    @PutMapping("/utilisateurs/{id}/role")
    public Dtos.UtilisateurDto role(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                                    @Valid @RequestBody Dtos.RoleRequest req) {
        return admin.changerRole(u.id(), id, req.role());
    }

    @PostMapping("/utilisateurs/{id}/reinitialisation-mot-de-passe")
    public Dtos.MotDePasseTemporaire reinitialiserMotDePasse(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id) {
        return admin.reinitialiserMotDePasse(u.id(), id);
    }

    @GetMapping("/parametres")
    public Dtos.ParametresQuotas parametres() {
        return admin.parametresQuotas();
    }

    @PutMapping("/parametres/quotas")
    public Dtos.ParametresQuotas definirParametres(@AuthenticationPrincipal UtilisateurConnecte u,
                                                   @Valid @RequestBody Dtos.ParametresQuotasRequest req) {
        return admin.definirParametresQuotas(u.id(), req);
    }

    @PutMapping("/utilisateurs/{id}/blocage")
    public Dtos.UtilisateurDto blocage(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                                       @Valid @RequestBody Dtos.BooleenRequest req) {
        return admin.bloquerCompte(u.id(), id, req.valeur());
    }

    @GetMapping("/prompts-partages")
    public List<Dtos.PromptUtilisateurDto> promptsPartages(@AuthenticationPrincipal UtilisateurConnecte u) {
        return promptsUtilisateur.listerPartages(u.id());
    }

    @PostMapping("/prompts-partages")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.PromptUtilisateurDto creerPromptPartage(@AuthenticationPrincipal UtilisateurConnecte u,
                                                        @Valid @RequestBody Dtos.PromptUtilisateurRequest req) {
        return promptsUtilisateur.creerPartage(u.id(), req);
    }

    @PutMapping("/prompts-partages/{id}")
    public Dtos.PromptUtilisateurDto modifierPromptPartage(@AuthenticationPrincipal UtilisateurConnecte u, @PathVariable Long id,
                                                           @Valid @RequestBody Dtos.PromptUtilisateurRequest req) {
        return promptsUtilisateur.modifierPartage(u.id(), id, req);
    }

    @DeleteMapping("/prompts-partages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerPromptPartage(@PathVariable Long id) {
        promptsUtilisateur.supprimerPartage(id);
    }

    @GetMapping("/prompts")
    public List<Dtos.PromptDto> prompts() {
        prompts.actif();
        return prompts.tous().stream().map(Mapper::prompt).toList();
    }

    @PostMapping("/prompts")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.PromptDto publier(@Valid @RequestBody Dtos.PromptRequest req) {
        return Mapper.prompt(prompts.publier(req.version(), req.contenu()));
    }

    @PutMapping("/prompts/{id}/activer")
    public Dtos.PromptDto activer(@PathVariable Long id) {
        return Mapper.prompt(prompts.activer(id));
    }
}
