package com.revia.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Tests d'API (MockMvc) de l'assistant IA et des outils IA, avec le fournisseur « demo ». */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String jeton() throws Exception {
        String rep = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nom", "Dev Assistant", "email",
                                "assistant-" + UUID.randomUUID() + "@revia.test", "motDePasse", "Motdepasse1"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(rep).get("token").asText();
    }

    @Test
    @DisplayName("Chat : 401 sans jeton, 200 avec une question, 400 sans message")
    void chat() throws Exception {
        String corps = json.writeValueAsString(Map.of("messages", List.of(Map.of("role", "utilisateur", "contenu", "Bonjour"))));
        mvc.perform(post("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isUnauthorized());
        String jeton = jeton();
        mvc.perform(post("/api/assistant/chat").header("Authorization", jeton).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outil").value("chat"))
                .andExpect(jsonPath("$.fournisseur").value("demo"))
                .andExpect(jsonPath("$.contenu", containsString("assistant RevIA")));
        mvc.perform(post("/api/assistant/chat").header("Authorization", jeton).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("Diffusion en direct (SSE) : morceaux de texte puis événement « fin »")
    void fluxDirect() throws Exception {
        MvcResult debut = mvc.perform(post("/api/assistant/outils/explication/flux").header("Authorization", jeton())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", "int a = 1;\nint b = a + 1;", "langage", "c"))))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(debut))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:morceau")))
                .andExpect(content().string(containsString("En bref")))
                .andExpect(content().string(containsString("event:fin")));
    }

    @Test
    @DisplayName("Outils : génération de tests, outil inconnu (404), conversion sans cible (400)")
    void outils() throws Exception {
        String jeton = jeton();
        mvc.perform(post("/api/assistant/outils/tests").header("Authorization", jeton).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", "def total(prix, qte):\n    return prix * qte\n", "langage", "auto"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outil").value("tests"))
                .andExpect(jsonPath("$.langage").value("python"))
                .andExpect(jsonPath("$.contenu", containsString("def test_total")));
        mvc.perform(post("/api/assistant/outils/magie").header("Authorization", jeton).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"x = 1\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/assistant/outils/conversion").header("Authorization", jeton).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"x = 1\", \"langage\": \"python\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choisissez le langage cible de la conversion."));
    }
}
