package com.revia.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Tests d'API (MockMvc) : authentification, création de revue, erreurs 400 / 401 / 403 / 404 / 429. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    static final String CODE_JAVA = """
            public class UserService {
                public User find(String id) throws SQLException {
                    Statement st = conn.createStatement();
                    String sql = "SELECT * FROM users WHERE id=" + id;
                    ResultSet rs = st.executeQuery(sql);
                    return map(rs);
                }
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String inscrire() throws Exception {
        String email = "dev-" + UUID.randomUUID() + "@revia.test";
        String rep = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Dev Test", "email", email, "motDePasse", "Motdepasse1"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(rep).get("token").asText();
    }

    String admin() throws Exception {
        String rep = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@revia.local\",\"motDePasse\":\"Admin2026!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(rep).get("token").asText();
    }

    JsonNode soumettre(String token, String code) throws Exception {
        String rep = mvc.perform(post("/api/revues").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("titre", "UserService.java", "langage", "auto", "code", code))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(rep);
    }

    @Test
    @DisplayName("401 sans jeton, 401 avec un jeton falsifié")
    void nonAuthentifie() throws Exception {
        mvc.perform(get("/api/revues")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
        mvc.perform(get("/api/me").header("Authorization", "Bearer abc.def.ghi")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Inscription, connexion, mauvais mot de passe, e-mail déjà utilisé, validation")
    void authentification() throws Exception {
        String email = "sarra-" + UUID.randomUUID() + "@revia.test";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Sarra A.", "email", email, "motDePasse", "Sarra2026!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.utilisateur.role").value("DEVELOPPEUR"))
                .andExpect(jsonPath("$.utilisateur.quotaJournalier").value(50))
                .andExpect(jsonPath("$.utilisateur.motDePasseHash").doesNotExist());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Sarra A.", "email", email, "motDePasse", "Sarra2026!"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "faux-mdp-1"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("E-mail ou mot de passe incorrect."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Sarra2026!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"X\",\"email\":\"pas-un-email\",\"motDePasse\":\"court\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.champs.email").exists());
    }

    @Test
    @DisplayName("Création d'une revue : injection SQL détectée ligne 4, score, correction, historique, CSV")
    void creationRevue() throws Exception {
        String token = inscrire();
        JsonNode r = soumettre(token, CODE_JAVA);
        assertThat(r.get("langage").asText()).isEqualTo("java");
        assertThat(r.get("score").asInt()).isLessThan(100);
        JsonNode injection = null;
        for (JsonNode p : r.get("problemes")) {
            if (p.get("titre").asText().equals("Injection SQL")) {
                injection = p;
            }
        }
        assertThat(injection).isNotNull();
        assertThat(injection.get("ligne").asInt()).isEqualTo(4);
        assertThat(injection.get("gravite").asText()).isEqualTo("critique");
        assertThat(injection.get("correction").get("codeApres").asText()).contains("prepareStatement");

        long id = r.get("id").asLong();
        mvc.perform(get("/api/revues").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/revues").param("langage", "python").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/revues/export.csv").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("revia-historique")))
                .andExpect(content().string(containsString("UserService.java")));
        mvc.perform(get("/api/tableau-de-bord").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nbRevues").value(1))
                .andExpect(jsonPath("$.nbProblemes", greaterThan(0)));

        // Faux positif : le score remonte
        int avant = r.get("score").asInt();
        String fp = mvc.perform(post("/api/problemes/" + injection.get("id").asLong() + "/faux-positif")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"valeur\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fauxPositif").value(true)).andReturn().getResponse().getContentAsString();
        assertThat(fp).isNotEmpty();
        mvc.perform(get("/api/revues/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.score").value(avant + 20));
        // Copier la correction
        mvc.perform(post("/api/problemes/" + injection.get("id").asLong() + "/correction/copie")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code", containsString("prepareStatement")));
        mvc.perform(delete("/api/revues/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/revues/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un développeur ne voit pas les revues d'un autre (404) et n'accède pas à l'admin (403)")
    void isolation() throws Exception {
        String a = inscrire();
        String b = inscrire();
        long id = soumettre(a, CODE_JAVA).get("id").asLong();
        mvc.perform(get("/api/revues/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", "Bearer " + b)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Code vide ou trop long : 400 « Collez entre 1 et 2 000 lignes de code. »")
    void longueurCode() throws Exception {
        String token = inscrire();
        mvc.perform(post("/api/revues").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Collez entre 1 et 2 000 lignes de code."));
        String trop = "x\n".repeat(2001);
        mvc.perform(post("/api/revues").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", trop))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_LONGUEUR"));
    }

    @Test
    @DisplayName("Quota utilisateur atteint : 429 avant tout appel au LLM ; admin : quota et blocage")
    void quotaEtAdmin() throws Exception {
        String dev = inscrire();
        String adm = admin();
        long devId = json.readTree(mvc.perform(get("/api/me").header("Authorization", "Bearer " + dev))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/admin/utilisateurs/" + devId + "/quota").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quota\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quotaJournalier").value(1));
        soumettre(dev, "int a = 1;");
        mvc.perform(post("/api/revues").header("Authorization", "Bearer " + dev).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"int b = 2;\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Vous avez atteint vos 1 revues du jour."));
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(greaterThan(1))));
        // Blocage : le jeton existant est immédiatement refusé
        mvc.perform(put("/api/admin/utilisateurs/" + devId + "/blocage").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeur\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bloque").value(true));
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + dev)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin : créer, modifier et supprimer un utilisateur (avec ses revues) ; garde-fous")
    void crudUtilisateurs() throws Exception {
        String adm = admin();
        String email = "crud-" + UUID.randomUUID() + "@revia.test";
        String creation = json.writeValueAsString(Map.of("nom", "Compte Crud", "email", email, "motDePasse", "Motdepasse1",
                "role", "DEVELOPPEUR", "quota", 7));

        // Création : 201, rôle et quota respectés
        String rep = mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content(creation))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("DEVELOPPEUR"))
                .andExpect(jsonPath("$.quotaJournalier").value(7)).andReturn().getResponse().getContentAsString();
        long id = json.readTree(rep).get("id").asLong();

        // E-mail déjà utilisé : 409 ; mot de passe trop faible : 400 ; non-admin : 403
        mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content(creation))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Faible", "email", "faible-" + UUID.randomUUID() + "@revia.test",
                                "motDePasse", "abc", "role", "DEVELOPPEUR"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + inscrire())
                        .contentType(MediaType.APPLICATION_JSON).content(creation))
                .andExpect(status().isForbidden());

        // Le compte créé peut se connecter
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Motdepasse1"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).get("token").asText();
        soumettre(token, "int a = 1;");

        // Modification : nom, e-mail, mot de passe et quota
        String nouvelEmail = "modifie-" + UUID.randomUUID() + "@revia.test";
        mvc.perform(put("/api/admin/utilisateurs/" + id).header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Nom Modifié", "email", nouvelEmail,
                                "motDePasse", "Nouveau2026", "quota", 12))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nom").value("Nom Modifié"))
                .andExpect(jsonPath("$.email").value(nouvelEmail)).andExpect(jsonPath("$.quotaJournalier").value(12));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Motdepasse1"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", nouvelEmail, "motDePasse", "Nouveau2026"))))
                .andExpect(status().isOk());

        // Suppression : 204, jeton refusé ensuite, compte introuvable, auto-suppression interdite
        mvc.perform(delete("/api/admin/utilisateurs/" + id).header("Authorization", "Bearer " + adm))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/utilisateurs/" + id).header("Authorization", "Bearer " + adm))
                .andExpect(status().isNotFound());
        long adminId = json.readTree(mvc.perform(get("/api/me").header("Authorization", "Bearer " + adm))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/admin/utilisateurs/" + adminId).header("Authorization", "Bearer " + adm))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez pas supprimer votre propre compte."));
    }

    @Test
    @DisplayName("Quota assistant (429), quotas par défaut, changement de rôle et réinitialisation du mot de passe")
    void quotasAssistantRolesEtReinitialisation() throws Exception {
        String adm = admin();
        String dev = inscrire();
        long devId = json.readTree(mvc.perform(get("/api/me").header("Authorization", "Bearer " + dev))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        String chat = "{\"messages\":[{\"role\":\"utilisateur\",\"contenu\":\"Bonjour\"}]}";

        // Quota assistant de 1 : la 2e requête est refusée avant tout appel au LLM ; l'administrateur n'est pas limité
        mvc.perform(put("/api/admin/utilisateurs/" + devId + "/quota").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quota\":50,\"quotaAssistant\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quotaAssistant").value(1));
        mvc.perform(post("/api/assistant/chat").header("Authorization", "Bearer " + dev)
                        .contentType(MediaType.APPLICATION_JSON).content(chat))
                .andExpect(status().isOk());
        mvc.perform(post("/api/assistant/chat").header("Authorization", "Bearer " + dev)
                        .contentType(MediaType.APPLICATION_JSON).content(chat))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Vous avez atteint vos 1 requêtes assistant du jour."));
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + dev))
                .andExpect(jsonPath("$.assistantDuJour").value(1));
        mvc.perform(post("/api/assistant/chat").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content(chat))
                .andExpect(status().isOk());

        // Quotas par défaut appliqués à tous les développeurs, puis rétablis
        mvc.perform(put("/api/admin/parametres/quotas").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotaRevuesDefaut\":20,\"quotaAssistantDefaut\":30,\"appliquerATous\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quotaRevuesDefaut").value(20));
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + dev))
                .andExpect(jsonPath("$.quotaJournalier").value(20)).andExpect(jsonPath("$.quotaAssistant").value(30));
        mvc.perform(get("/api/systeme/info")).andExpect(jsonPath("$.quotaParDefaut").value(20))
                .andExpect(jsonPath("$.quotaAssistantParDefaut").value(30));
        mvc.perform(put("/api/admin/parametres/quotas").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotaRevuesDefaut\":50,\"quotaAssistantDefaut\":100,\"appliquerATous\":true}"))
                .andExpect(status().isOk());

        // Changement de rôle : promotion, accès admin immédiat, rétrogradation ; pas sur son propre compte
        String email = "role-" + UUID.randomUUID() + "@revia.test";
        long id = json.readTree(mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Compte Rôle", "email", email, "motDePasse", "Motdepasse1",
                                "role", "DEVELOPPEUR"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String token = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Motdepasse1"))))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/utilisateurs/" + id + "/role").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/utilisateurs/" + id + "/role").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DEVELOPPEUR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("DEVELOPPEUR"))
                .andExpect(jsonPath("$.quotaJournalier").value(50));
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        long adminId = json.readTree(mvc.perform(get("/api/me").header("Authorization", "Bearer " + adm))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/admin/utilisateurs/" + adminId + "/role").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DEVELOPPEUR\"}"))
                .andExpect(status().isBadRequest());

        // Réinitialisation : mot de passe temporaire valide, ancien mot de passe refusé ; réservé à l'admin
        mvc.perform(post("/api/admin/utilisateurs/" + id + "/reinitialisation-mot-de-passe").header("Authorization", "Bearer " + dev))
                .andExpect(status().isForbidden());
        String temporaire = json.readTree(mvc.perform(post("/api/admin/utilisateurs/" + id + "/reinitialisation-mot-de-passe")
                        .header("Authorization", "Bearer " + adm))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("motDePasse").asText();
        assertThat(temporaire).hasSize(12).matches("^(?=.*[A-Za-z])(?=.*\\d).+$");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Motdepasse1"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", temporaire))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mes prompts : créer, lister, modifier, supprimer ; privés à leur auteur")
    void mesPrompts() throws Exception {
        String a = inscrire();
        String b = inscrire();
        String corps = json.writeValueAsString(Map.of("titre", "Explique en simple", "contenu", "Explique ce code à un débutant."));
        long id = json.readTree(mvc.perform(post("/api/mes-prompts").header("Authorization", "Bearer " + a)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.titre").value("Explique en simple"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/mes-prompts").header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("titre", "Avantages", "contenu", "Liste les points forts et faibles."))))
                .andExpect(status().isCreated());

        // Liste triée par titre, limitée à l'auteur
        mvc.perform(get("/api/mes-prompts").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].titre").value("Avantages"));
        mvc.perform(get("/api/mes-prompts").header("Authorization", "Bearer " + b))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        // Un autre utilisateur ne peut ni modifier ni supprimer (404) ; titre vide : 400 ; sans jeton : 401
        mvc.perform(put("/api/mes-prompts/" + id).header("Authorization", "Bearer " + b)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/mes-prompts/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
        mvc.perform(post("/api/mes-prompts").header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\" \",\"contenu\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/mes-prompts")).andExpect(status().isUnauthorized());

        // Modification puis suppression par l'auteur
        mvc.perform(put("/api/mes-prompts/" + id).header("Authorization", "Bearer " + a).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("titre", "Explique", "contenu", "Explique ligne par ligne."))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenu").value("Explique ligne par ligne."));
        mvc.perform(delete("/api/mes-prompts/" + id).header("Authorization", "Bearer " + a)).andExpect(status().isNoContent());
        mvc.perform(get("/api/mes-prompts").header("Authorization", "Bearer " + a)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Prompts partagés par l'admin, dernière connexion et en-têtes Bucket4j")
    void promptsPartagesConnexionEtDebit() throws Exception {
        String adm = admin();
        String dev = inscrire();
        String corps = json.writeValueAsString(Map.of("titre", "Revue sécurité OWASP", "contenu", "Passe ce code en revue selon l'OWASP Top 10."));

        // Création par l'admin ; visible de tout développeur mais non modifiable par lui ; refusée à un développeur
        long id = json.readTree(mvc.perform(post("/api/admin/prompts-partages").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.partage").value(true))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/admin/prompts-partages").header("Authorization", "Bearer " + dev)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/mes-prompts").header("Authorization", "Bearer " + dev))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].titre").value("Revue sécurité OWASP"))
                .andExpect(jsonPath("$[0].partage").value(true)).andExpect(jsonPath("$[0].modifiable").value(false));
        mvc.perform(put("/api/mes-prompts/" + id).header("Authorization", "Bearer " + dev)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/mes-prompts/" + id).header("Authorization", "Bearer " + dev)).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/prompts-partages/" + id).header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("titre", "Revue OWASP", "contenu", "Version 2."))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titre").value("Revue OWASP"));
        mvc.perform(delete("/api/admin/prompts-partages/" + id).header("Authorization", "Bearer " + adm)).andExpect(status().isNoContent());
        mvc.perform(get("/api/mes-prompts").header("Authorization", "Bearer " + dev)).andExpect(jsonPath("$", hasSize(0)));

        // Dernière connexion renseignée à la connexion ; en-têtes de limitation de débit et limites publiées
        String email = "connexion-" + UUID.randomUUID() + "@revia.test";
        mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Compte Connexion", "email", email, "motDePasse", "Motdepasse1", "role", "DEVELOPPEUR"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.derniereConnexion").doesNotExist());
        String rep = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Motdepasse1"))))
                .andExpect(status().isOk()).andExpect(header().exists("X-RateLimit-Limit")).andExpect(header().exists("X-RateLimit-Remaining"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(rep).get("token").asText();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.derniereConnexion").isNotEmpty());
        mvc.perform(get("/api/systeme/info")).andExpect(jsonPath("$.analysesParMinute").isNumber())
                .andExpect(jsonPath("$.connexionsParMinute").isNumber());
    }

    @Test
    @DisplayName("Compactage de la conversation : résumé, minimum deux messages, budget de contexte publié")
    void compactage() throws Exception {
        String dev = inscrire();
        String conversation = json.writeValueAsString(Map.of("messages", java.util.List.of(
                Map.of("role", "utilisateur", "contenu", "Comment éviter une injection SQL en Java ?"),
                Map.of("role", "assistant", "contenu", "Utilisez une requête paramétrée avec PreparedStatement."),
                Map.of("role", "utilisateur", "contenu", "Et pour un mot de passe = \"admin123\" dans le code ?"))));
        mvc.perform(post("/api/assistant/compacter").header("Authorization", "Bearer " + dev)
                        .contentType(MediaType.APPLICATION_JSON).content(conversation))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outil").value("compactage"))
                .andExpect(jsonPath("$.contenu").value(containsString("injection SQL")));
        mvc.perform(post("/api/assistant/compacter").header("Authorization", "Bearer " + dev)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[{\"role\":\"utilisateur\",\"contenu\":\"Bonjour\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/assistant/compacter").contentType(MediaType.APPLICATION_JSON).content(conversation))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/systeme/info")).andExpect(jsonPath("$.contexteMaxTokens").value(12000));
    }

    long creerCompte(String adm, String nom, String email) throws Exception {
        return json.readTree(mvc.perform(post("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", nom, "email", email, "motDePasse", "Motdepasse1", "role", "DEVELOPPEUR"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("Admin IA : profil de compétences, quotas conseillés, alertes d'usage anormal")
    void adminProfilQuotasEtAlertes() throws Exception {
        String adm = admin();
        String email = "profil-" + UUID.randomUUID() + "@revia.test";
        long id = creerCompte(adm, "Ariane Moreau", email);
        String token = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "Motdepasse1"))))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        soumettre(token, "String sql = \"SELECT * FROM users WHERE id=\" + id;");

        // Profil : statistiques calculées par le serveur + synthèse (règles de secours en mode demo)
        mvc.perform(get("/api/admin/utilisateurs/" + id + "/profil").header("Authorization", "Bearer " + adm))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nbRevues").value(1)).andExpect(jsonPath("$.source").value("regles"))
                .andExpect(jsonPath("$.synthese").value(containsString("Axes de progression")));
        mvc.perform(get("/api/admin/utilisateurs/" + id + "/profil").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());

        // Quotas conseillés : une ligne par développeur, pic + 25 % arrondi aux 5 supérieurs, minimum 10 revues
        mvc.perform(get("/api/admin/recommandations-quotas").header("Authorization", "Bearer " + adm))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recommandations[?(@.utilisateurId==" + id + ")].picRevues").value(1))
                .andExpect(jsonPath("$.recommandations[?(@.utilisateurId==" + id + ")].recommandeRevues").value(10))
                .andExpect(jsonPath("$.commentaire").isNotEmpty());

        // Alertes : 6 connexions échouées sur un compte existant => alerte critique
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "motDePasse", "mauvais-" + i))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/admin/alertes").header("Authorization", "Bearer " + adm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertes[?(@.type=='connexions_echouees' && @.utilisateurId==" + id + ")].niveau").value("critique"))
                .andExpect(jsonPath("$.resume").isNotEmpty());
        mvc.perform(get("/api/admin/alertes").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Assistant d'administration : commande en français -> plan à confirmer -> exécution validée par le serveur")
    void assistantAdministration() throws Exception {
        String adm = admin();
        long zoelia = creerCompte(adm, "Zoelia Quentin", "zoelia-" + UUID.randomUUID() + "@revia.test");
        long yanis = creerCompte(adm, "Yanis Bertrand", "yanis-" + UUID.randomUUID() + "@revia.test");

        // Interprétation : rien n'est exécuté
        mvc.perform(post("/api/admin/assistant").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commande\":\"bloque Zoelia\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.actions", hasSize(1)))
                .andExpect(jsonPath("$.actions[0].type").value("BLOQUER")).andExpect(jsonPath("$.actions[0].utilisateurId").value(zoelia));
        mvc.perform(post("/api/admin/assistant").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commande\":\"passe Yanis en administrateur\"}"))
                .andExpect(jsonPath("$.actions", hasSize(1))).andExpect(jsonPath("$.actions[0].type").value("CHANGER_ROLE"))
                .andExpect(jsonPath("$.actions[0].role").value("ADMIN"));
        mvc.perform(post("/api/admin/assistant").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commande\":\"mets le quota assistant de Yanis a 33\"}"))
                .andExpect(jsonPath("$.actions", hasSize(1))).andExpect(jsonPath("$.actions[0].type").value("DEFINIR_QUOTA"))
                .andExpect(jsonPath("$.actions[0].quotaAssistant").value(33));
        // Garde-fous : un administrateur n'est jamais bloqué ; commande incomprise ; réservé à l'admin
        mvc.perform(post("/api/admin/assistant").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commande\":\"bloque Administrateur RevIA\"}"))
                .andExpect(jsonPath("$.actions", hasSize(0))).andExpect(jsonPath("$.avertissements", hasSize(greaterThan(0))));
        mvc.perform(post("/api/admin/assistant").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commande\":\"fais-moi un cafe\"}"))
                .andExpect(jsonPath("$.actions", hasSize(0)));
        mvc.perform(post("/api/admin/assistant").header("Authorization", "Bearer " + inscrire()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commande\":\"bloque Zoelia\"}"))
                .andExpect(status().isForbidden());

        // Exécution confirmée : le serveur revalide chaque action
        String corps = json.writeValueAsString(Map.of("actions", java.util.List.of(
                Map.of("type", "BLOQUER", "utilisateurId", zoelia),
                Map.of("type", "DEFINIR_QUOTA", "utilisateurId", yanis, "quota", 44, "quotaAssistant", 33),
                Map.of("type", "BLOQUER", "utilisateurId", 999999))));
        mvc.perform(post("/api/admin/assistant/executer").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultats[0].ok").value(true))
                .andExpect(jsonPath("$.resultats[1].ok").value(true)).andExpect(jsonPath("$.resultats[2].ok").value(false));
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", "Bearer " + adm))
                .andExpect(jsonPath("$[?(@.id==" + zoelia + ")].bloque").value(true))
                .andExpect(jsonPath("$[?(@.id==" + yanis + ")].quotaJournalier").value(44))
                .andExpect(jsonPath("$[?(@.id==" + yanis + ")].quotaAssistant").value(33));
        // Un administrateur ne peut pas être bloqué même par une action forgée
        long adminId = json.readTree(mvc.perform(get("/api/me").header("Authorization", "Bearer " + adm))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/admin/assistant/executer").header("Authorization", "Bearer " + adm).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("actions", java.util.List.of(Map.of("type", "BLOQUER", "utilisateurId", adminId))))))
                .andExpect(jsonPath("$.resultats[0].ok").value(false));
    }

    @Test
    @DisplayName("Informations système publiques sans la clé d'API ; santé de l'application")
    void systeme() throws Exception {
        mvc.perform(get("/api/systeme/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.fournisseur").value("demo"))
                .andExpect(jsonPath("$.temperature").value(0.2))
                .andExpect(jsonPath("$.apiKey").doesNotExist());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
