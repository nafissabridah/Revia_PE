package com.revia.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revia.ai.ConseilIA;
import com.revia.domain.Developpeur;
import com.revia.domain.Probleme;
import com.revia.domain.Revue;
import com.revia.domain.UsageAssistant;
import com.revia.domain.Utilisateur;
import com.revia.error.ApiException;
import com.revia.error.ErrorCode;
import com.revia.repository.RevueRepository;
import com.revia.repository.UsageAssistantRepository;
import com.revia.repository.UtilisateurRepository;
import com.revia.security.JournalConnexions;
import com.revia.web.dto.Dtos;

/**
 * Administration assistée par IA. Les chiffres sont toujours calculés par le serveur ; l'IA ne fait que rédiger
 * (profil, commentaires) ou traduire une commande en actions, que le serveur valide, que l'administrateur confirme
 * et que {@link AdminService} exécute. Sans IA disponible, des règles de secours prennent le relais.
 */
@Service
public class AdminIAService {

    private static final Logger log = LoggerFactory.getLogger(AdminIAService.class);
    private static final int JOURS_PROFIL = 180;
    private static final int JOURS_QUOTAS = 30;
    private static final Pattern INACTIFS = Pattern.compile("inactifs?\\s+depuis\\s+(\\d+)\\s*(?:jours?|j)\\b");
    private static final Pattern QUOTA_REVUES = Pattern.compile("quota\\s+(?:de\\s+|a\\s+|=\\s*|:\\s*)?(\\d+)");
    /** Mots du vocabulaire de commande ou de l'application : jamais pris pour le nom d'un compte. */
    private static final java.util.Set<String> MOTS_COMMUNS = java.util.Set.of("administrateur", "admin", "developpeur", "compte",
            "comptes", "quota", "revia", "assistant", "tous", "les", "des", "test", "dev", "user", "utilisateur");
    private static final Pattern QUOTA_ASSISTANT = Pattern.compile("assistant\\D{0,20}?(\\d+)");

    private final UtilisateurRepository utilisateurs;
    private final RevueRepository revues;
    private final UsageAssistantRepository usages;
    private final AdminService admin;
    private final ConseilIA ia;
    private final JournalConnexions journal;
    private final ObjectMapper mapper;

    public AdminIAService(UtilisateurRepository utilisateurs, RevueRepository revues, UsageAssistantRepository usages,
                          AdminService admin, ConseilIA ia, JournalConnexions journal, ObjectMapper mapper) {
        this.utilisateurs = utilisateurs;
        this.revues = revues;
        this.usages = usages;
        this.admin = admin;
        this.ia = ia;
        this.journal = journal;
        this.mapper = mapper;
    }

    // =====================================================================================================
    // 1. Profil de compétences
    // =====================================================================================================

    @Transactional(readOnly = true)
    public Dtos.ProfilCompetences profil(Long userId) {
        Utilisateur u = trouver(userId);
        if (!(u instanceof Developpeur)) {
            throw new ApiException(ErrorCode.VALIDATION, "Le profil de compétences ne concerne que les comptes développeur.");
        }
        List<Revue> rs = revues.findByAuteurIdAndCreeLeGreaterThanEqualOrderByCreeLeAsc(userId, LocalDateTime.now().minusDays(JOURS_PROFIL));
        int n = rs.size();
        int moyenne = n == 0 ? 0 : (int) Math.round(rs.stream().mapToInt(Revue::getScore).average().orElse(0));
        int k = Math.min(3, Math.max(n, 1));
        int initial = n == 0 ? 0 : (int) Math.round(rs.subList(0, k).stream().mapToInt(Revue::getScore).average().orElse(0));
        int recent = n == 0 ? 0 : (int) Math.round(rs.subList(n - k, n).stream().mapToInt(Revue::getScore).average().orElse(0));
        String tendance = n < 4 ? "insuffisant" : recent - initial >= 5 ? "progression" : initial - recent >= 5 ? "baisse" : "stable";

        Map<String, Long> titres = new HashMap<>();
        Map<String, Long> categories = new LinkedHashMap<>();
        Map<String, Long> gravites = new LinkedHashMap<>();
        long masquages = 0;
        for (Revue r : rs) {
            masquages += r.getNbMasquages();
            for (Probleme p : r.getProblemes()) {
                if (!p.isFauxPositif()) {
                    titres.merge(p.getTitre().trim(), 1L, Long::sum);
                    categories.merge(p.getCategorie(), 1L, Long::sum);
                    gravites.merge(p.getGravite().name(), 1L, Long::sum);
                }
            }
        }
        List<Dtos.Recurrent> recurrents = titres.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5).map(e -> new Dtos.Recurrent(e.getKey(), e.getValue())).toList();

        String secours = syntheseRegles(u.getNom(), n, moyenne, tendance, recurrents, masquages);
        String donnees = "Développeur : " + u.getNom() + "\nRevues sur " + JOURS_PROFIL + " jours : " + n + "\nScore moyen : " + moyenne
                + "/100 (premières revues : " + initial + ", dernières : " + recent + ", tendance : " + tendance + ")\n"
                + "Problèmes récurrents : " + (recurrents.isEmpty() ? "aucun" : recurrents.stream()
                        .map(r -> r.titre() + " (" + r.occurrences() + ")").collect(Collectors.joining(", "))) + "\n"
                + "Catégories : " + categories + "\nGravités : " + gravites + "\nSecrets masqués avant envoi : " + masquages;
        String texte = n == 0 ? secours : ia.rediger("""
                Outil : synthèse de profil de compétences.
                Rédige en Markdown une synthèse pour l'administrateur : un court bilan (2 phrases), puis « Axes de progression »
                avec exactement 3 points numérotés, concrets, qui s'appuient sur les problèmes récurrents.
                Ton bienveillant, 150 mots maximum.
                """, donnees, () -> secours);
        return new Dtos.ProfilCompetences(userId, u.getNom(), n, moyenne, initial, recent, tendance, recurrents, categories, gravites,
                masquages, texte, texte.equals(secours) ? "regles" : "ia");
    }

    private static String syntheseRegles(String nom, int n, int moyenne, String tendance, List<Dtos.Recurrent> rec, long masquages) {
        if (n == 0) {
            return "Aucune revue sur les " + JOURS_PROFIL + " derniers jours : le profil de " + nom + " sera disponible après ses premières revues.";
        }
        String lib = switch (tendance) {
            case "progression" -> "en progression";
            case "baisse" -> "en baisse";
            case "stable" -> "stable";
            default -> "tendance non significative (moins de 4 revues)";
        };
        StringBuilder sb = new StringBuilder("### Bilan de " + nom + "\n\n");
        sb.append("- **").append(n).append(" revue(s)** sur ").append(JOURS_PROFIL).append(" jours, score moyen **").append(moyenne)
                .append("/100** (").append(lib).append(").\n");
        sb.append("- Failles les plus fréquentes : ").append(rec.isEmpty() ? "aucune faille récurrente."
                : rec.stream().map(r -> "« " + r.titre() + " » (" + r.occurrences() + ")").collect(Collectors.joining(", ")) + ".").append('\n');
        if (masquages > 0) {
            sb.append("- ").append(masquages).append(" secret(s) masqué(s) avant envoi à l'IA : des secrets sont écrits dans le code.\n");
        }
        sb.append("\n**Axes de progression**\n");
        List<String> axes = new ArrayList<>();
        for (Dtos.Recurrent r : rec) {
            if (axes.size() < 3) {
                axes.add(conseil(r.titre()));
            }
        }
        if (masquages > 0 && axes.size() < 3) {
            axes.add("Déplacer les mots de passe et clés dans des variables d'environnement, jamais dans le code.");
        }
        String[] generiques = {"Relancer une revue après chaque correction pour vérifier la progression du score.",
                "Utiliser l'outil « Génération de tests » sur le code corrigé.",
                "Lire l'explication de chaque correction avant de l'appliquer."};
        for (int i = 0; axes.size() < 3; i++) {
            axes.add(generiques[i]);
        }
        for (int i = 0; i < 3; i++) {
            sb.append(i + 1).append(". ").append(axes.get(i)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String conseil(String titre) {
        String t = norm(titre);
        if (t.contains("sql") || t.contains("injection")) {
            return "Éviter « " + titre + " » : utiliser des requêtes paramétrées (PreparedStatement) au lieu de concaténer des valeurs.";
        }
        if (t.contains("mot de passe") || t.contains("secret") || t.contains("cle")) {
            return "Éviter « " + titre + " » : placer les secrets dans des variables d'environnement.";
        }
        if (t.contains("ressource") || t.contains("ferm")) {
            return "Éviter « " + titre + " » : fermer les ressources avec try-with-resources.";
        }
        if (t.contains("xss")) {
            return "Éviter « " + titre + " » : échapper toute donnée affichée dans une page.";
        }
        return "Traiter le problème récurrent « " + titre + " » : relire sa correction proposée et l'appliquer d'emblée.";
    }

    // =====================================================================================================
    // 2. Quotas conseillés
    // =====================================================================================================

    @Transactional(readOnly = true)
    public Dtos.RecommandationsQuotas recommandations() {
        LocalDateTime depuis = LocalDate.now().minusDays(JOURS_QUOTAS).atStartOfDay();
        Map<Long, Map<LocalDate, Integer>> parRevues = new HashMap<>();
        for (Object[] l : revues.activiteDepuis(depuis)) {
            parRevues.computeIfAbsent((Long) l[0], x -> new HashMap<>()).merge(((LocalDateTime) l[1]).toLocalDate(), 1, Integer::sum);
        }
        Map<Long, Map<LocalDate, Integer>> parAssistant = new HashMap<>();
        for (UsageAssistant a : usages.findByCreeLeGreaterThanEqual(depuis)) {
            parAssistant.computeIfAbsent(a.getUtilisateurId(), x -> new HashMap<>()).merge(a.getCreeLe().toLocalDate(), 1, Integer::sum);
        }
        List<Dtos.RecommandationQuota> liste = new ArrayList<>();
        for (Utilisateur u : utilisateurs.findAll(Sort.by("id"))) {
            if (!(u instanceof Developpeur d)) {
                continue;
            }
            Map<LocalDate, Integer> r = parRevues.getOrDefault(d.getId(), Map.of());
            Map<LocalDate, Integer> a = parAssistant.getOrDefault(d.getId(), Map.of());
            long actifs = java.util.stream.Stream.concat(r.keySet().stream(), a.keySet().stream()).distinct().count();
            int picR = r.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            int picA = a.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            double moyR = actifs == 0 ? 0 : arrondi1(r.values().stream().mapToInt(Integer::intValue).sum() / (double) actifs);
            double moyA = actifs == 0 ? 0 : arrondi1(a.values().stream().mapToInt(Integer::intValue).sum() / (double) actifs);
            int recR = actifs == 0 ? d.getQuotaJournalier() : conseille(picR, 10);
            int recA = actifs == 0 ? d.getQuotaAssistant() : conseille(picA, 20);
            String explication = actifs == 0
                    ? "Aucune activité sur " + JOURS_QUOTAS + " jours : quotas conservés."
                    : actifs + " jour(s) actif(s) : " + moyR + " revue(s) et " + moyA + " requête(s) assistant par jour en moyenne, pic à "
                            + picR + " et " + picA + ". Quotas conseillés : pic + 25 %, arrondi aux 5 supérieurs.";
            liste.add(new Dtos.RecommandationQuota(d.getId(), d.getNom(), d.getQuotaJournalier(), d.getQuotaAssistant(), recR, recA, moyR,
                    picR, moyA, picA, (int) actifs, explication, recR != d.getQuotaJournalier() || recA != d.getQuotaAssistant()));
        }
        long aChanger = liste.stream().filter(Dtos.RecommandationQuota::modificationUtile).count();
        String secours = liste.isEmpty() ? "Aucun développeur à analyser."
                : aChanger == 0 ? "Les quotas actuels correspondent à l'usage des " + JOURS_QUOTAS + " derniers jours."
                : aChanger + " compte(s) sur " + liste.size() + " ont un quota éloigné de leur usage réel (pic + 25 %) : voir les propositions ci-dessous.";
        String donnees = liste.stream().map(x -> x.nom() + " : quota " + x.quotaRevues() + "/" + x.quotaAssistant() + ", pic " + x.picRevues()
                + "/" + x.picAssistant() + ", conseillé " + x.recommandeRevues() + "/" + x.recommandeAssistant()).collect(Collectors.joining("\n"));
        String commentaire = liste.isEmpty() ? secours : ia.rediger("""
                Outil : commentaire sur les quotas conseillés.
                En 3 phrases maximum, commente ces propositions de quotas (revues/assistant par jour) pour l'administrateur :
                quels comptes sont sous-dimensionnés ou sur-dimensionnés, et la prudence à avoir.
                """, donnees, () -> secours);
        return new Dtos.RecommandationsQuotas(liste, commentaire, commentaire.equals(secours) ? "regles" : "ia");
    }

    private static int conseille(int pic, int minimum) {
        int brut = Math.max(minimum, (int) Math.ceil(pic * 1.25));
        return Math.min(10_000, ((brut + 4) / 5) * 5);
    }

    private static double arrondi1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // =====================================================================================================
    // 3. Alertes d'usage anormal
    // =====================================================================================================

    @Transactional(readOnly = true)
    public Dtos.Alertes alertes(boolean avecResumeIA) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime debutJour = QuotaService.debutDuJour();
        LocalDateTime depuis = debutJour.minusDays(14);
        Map<Long, int[]> revuesJour = new HashMap<>();       // [aujourd'hui, avant, masquages aujourd'hui]
        for (Object[] l : revues.activiteDepuis(depuis)) {
            int[] c = revuesJour.computeIfAbsent((Long) l[0], x -> new int[3]);
            if (((LocalDateTime) l[1]).isBefore(debutJour)) {
                c[1]++;
            } else {
                c[0]++;
                c[2] += (Integer) l[2];
            }
        }
        Map<Long, int[]> assistantJour = new HashMap<>();    // [aujourd'hui, avant]
        for (UsageAssistant a : usages.findByCreeLeGreaterThanEqual(depuis)) {
            int[] c = assistantJour.computeIfAbsent(a.getUtilisateurId(), x -> new int[2]);
            c[a.getCreeLe().isBefore(debutJour) ? 1 : 0]++;
        }
        List<Dtos.Alerte> alertes = new ArrayList<>();
        Map<String, Utilisateur> parEmail = new HashMap<>();
        for (Utilisateur u : utilisateurs.findAll(Sort.by("id"))) {
            parEmail.put(u.getEmail().toLowerCase(Locale.ROOT), u);
            if (!(u instanceof Developpeur d)) {
                continue;
            }
            int[] r = revuesJour.getOrDefault(d.getId(), new int[3]);
            int[] a = assistantJour.getOrDefault(d.getId(), new int[2]);
            int aujourdhui = r[0] + a[0];
            double base = (r[1] + a[1]) / 14.0;
            if (aujourdhui >= 10 && aujourdhui > 3 * base) {
                String niveau = aujourdhui > 5 * Math.max(base, 1) ? "critique" : "attention";
                alertes.add(new Dtos.Alerte(niveau, "pic_usage", d.getId(), d.getNom(), "Pic d'activité",
                        aujourdhui + " requêtes aujourd'hui (" + r[0] + " revues, " + a[0] + " assistant) contre " + arrondi1(base)
                                + " par jour en moyenne sur les 14 jours précédents.", maintenant));
            }
            if (r[2] >= 10) {
                alertes.add(new Dtos.Alerte("attention", "secrets_en_dur", d.getId(), d.getNom(), "Beaucoup de secrets masqués",
                        r[2] + " secret(s) masqué(s) dans ses revues du jour : des mots de passe ou clés sont écrits dans le code.", maintenant));
            }
            if (d.getQuotaJournalier() > 0 && r[0] >= 0.9 * d.getQuotaJournalier()
                    || d.getQuotaAssistant() > 0 && a[0] >= 0.9 * d.getQuotaAssistant()) {
                alertes.add(new Dtos.Alerte("info", "quota_proche", d.getId(), d.getNom(), "Quota presque atteint",
                        "Revues " + r[0] + "/" + d.getQuotaJournalier() + ", assistant " + a[0] + "/" + d.getQuotaAssistant() + " aujourd'hui.", maintenant));
            }
        }
        List<JournalConnexions.Echec> echecs = journal.depuis(Duration.ofMinutes(15));
        Map<String, Long> parCompte = echecs.stream().collect(Collectors.groupingBy(JournalConnexions.Echec::email, Collectors.counting()));
        for (Map.Entry<String, Long> e : parCompte.entrySet()) {
            if (e.getValue() >= 5) {
                Utilisateur cible = parEmail.get(e.getKey());
                alertes.add(new Dtos.Alerte(cible != null ? "critique" : "attention", "connexions_echouees", cible != null ? cible.getId() : null,
                        cible != null ? cible.getNom() : e.getKey(), "Connexions échouées répétées",
                        e.getValue() + " échecs en 15 minutes sur " + (cible != null ? "ce compte" : "un compte inexistant")
                                + " : tentative de deviner le mot de passe possible.", maintenant));
            }
        }
        if (echecs.size() >= 15 && parCompte.size() >= 3) {
            alertes.add(new Dtos.Alerte("critique", "force_brute", null, "Plusieurs comptes", "Attaque possible par dictionnaire",
                    echecs.size() + " échecs de connexion en 15 minutes sur " + parCompte.size() + " identifiants différents.", maintenant));
        }
        alertes.sort(Comparator.comparingInt((Dtos.Alerte x) -> switch (x.niveau()) {
            case "critique" -> 0;
            case "attention" -> 1;
            default -> 2;
        }));
        long critiques = alertes.stream().filter(x -> "critique".equals(x.niveau())).count();
        String secours = alertes.isEmpty() ? "Aucune anomalie détectée : l'activité est dans la normale."
                : alertes.size() + " alerte(s) dont " + critiques + " critique(s) : traitez d'abord les alertes rouges.";
        String resume = alertes.isEmpty() || !avecResumeIA ? secours : ia.rediger("""
                Outil : résumé d'alertes de sécurité et d'usage.
                Résume ces alertes en 2 phrases pour l'administrateur et indique la première action à faire.
                """, alertes.stream().map(x -> "[" + x.niveau() + "] " + x.nom() + " : " + x.message() + " - " + x.detail())
                .collect(Collectors.joining("\n")), () -> secours);
        return new Dtos.Alertes(alertes, resume, resume.equals(secours) ? "regles" : "ia");
    }

    // =====================================================================================================
    // 4. Assistant d'administration en langage naturel
    // =====================================================================================================

    /** Ce que l'administrateur veut faire, avant résolution des comptes concernés. */
    private record Intention(String type, List<String> noms, Integer inactifsJours, String filtreRole, String role, Integer quota,
                             Integer quotaAssistant) {
    }

    @Transactional(readOnly = true)
    public Dtos.PlanAdmin interpreter(Long adminId, String commande) {
        List<Utilisateur> tous = utilisateurs.findAll(Sort.by("id"));
        List<Intention> intentions = null;
        String source = "regles";
        if (ia.modeleDisponible()) {
            intentions = interpreterParIA(commande, tous);
            if (intentions != null && !intentions.isEmpty()) {
                source = "ia";
            }
        }
        if (intentions == null || intentions.isEmpty()) {
            intentions = interpreterParRegles(commande, tous);
            source = "regles";
        }
        List<String> avertissements = new ArrayList<>();
        List<Dtos.ActionPlan> actions = new ArrayList<>();
        for (Intention i : intentions) {
            resoudre(adminId, i, tous, actions, avertissements);
        }
        if (intentions.isEmpty()) {
            avertissements.add("Commande non comprise. Exemples : « bloque les comptes inactifs depuis 30 jours », "
                    + "« passe Sarra en administrateur », « mets le quota de Sarra à 30 », « débloque Sarra ».");
        }
        String interpretation = intentions.isEmpty() ? "Aucune action reconnue."
                : intentions.stream().map(AdminIAService::decrire).collect(Collectors.joining(" ; ")) + " → " + actions.size() + " compte(s) concerné(s).";
        return new Dtos.PlanAdmin(interpretation, source, actions, avertissements);
    }

    private List<Intention> interpreterParIA(String commande, List<Utilisateur> tous) {
        String comptes = tous.stream().map(u -> "- " + u.getNom() + " (" + u.role() + (u.isBloque() ? ", bloqué" : "") + ")")
                .collect(Collectors.joining("\n"));
        String brut = ia.appeler("""
                Outil : interprétation d'une commande d'administration.
                Transforme la commande en actions. Réponds UNIQUEMENT par un tableau JSON, sans texte autour.
                Chaque élément : {"type":"BLOQUER|DEBLOQUER|CHANGER_ROLE|DEFINIR_QUOTA","noms":["nom d'un compte de la liste"],
                "inactifsDepuisJours":<entier ou null>,"filtreRole":"DEVELOPPEUR|ADMIN|null","role":"DEVELOPPEUR|ADMIN|null",
                "quota":<entier ou null>,"quotaAssistant":<entier ou null>}
                « quota » = revues par jour, « quotaAssistant » = requêtes assistant par jour. N'utilise que ces types.
                Si la commande est hors périmètre, renvoie [].
                """, "Commande : " + commande + "\nComptes :\n" + comptes);
        if (brut == null) {
            return null;
        }
        try {
            String json = brut.substring(brut.indexOf('['), brut.lastIndexOf(']') + 1);
            List<Intention> liste = new ArrayList<>();
            for (JsonNode n : mapper.readTree(json)) {
                String type = n.path("type").asText("");
                if (!List.of("BLOQUER", "DEBLOQUER", "CHANGER_ROLE", "DEFINIR_QUOTA").contains(type)) {
                    continue;
                }
                List<String> noms = new ArrayList<>();
                n.path("noms").forEach(x -> noms.add(x.asText()));
                liste.add(new Intention(type, noms, entier(n, "inactifsDepuisJours"), texte(n, "filtreRole"), texte(n, "role"),
                        entier(n, "quota"), entier(n, "quotaAssistant")));
            }
            return liste;
        } catch (Exception e) {
            log.warn("Réponse d'interprétation invalide, règles de secours utilisées : {}", e.getMessage());
            return null;
        }
    }

    private static Integer entier(JsonNode n, String champ) {
        JsonNode v = n.get(champ);
        return v == null || v.isNull() || !v.canConvertToInt() ? null : v.asInt();
    }

    private static String texte(JsonNode n, String champ) {
        JsonNode v = n.get(champ);
        String t = v == null || v.isNull() ? null : v.asText();
        return t == null || t.isBlank() || "null".equalsIgnoreCase(t) ? null : t.toUpperCase(Locale.ROOT);
    }

    /** Secours sans IA : reconnaît quelques formulations courantes (bloquer, débloquer, changer de rôle, quotas). */
    private List<Intention> interpreterParRegles(String commande, List<Utilisateur> tous) {
        String t = norm(commande);
        String type;
        String role = null;
        if (t.contains("debloqu")) {
            type = "DEBLOQUER";
        } else if (t.contains("bloqu")) {
            type = "BLOQUER";
        } else if (t.contains("quota")) {
            type = "DEFINIR_QUOTA";
        } else if (Pattern.compile("\\ben\\s+(administrateur|admin)\\b").matcher(t).find()) {
            type = "CHANGER_ROLE";
            role = "ADMIN";
        } else if (Pattern.compile("\\ben\\s+developpeur\\b").matcher(t).find()) {
            type = "CHANGER_ROLE";
            role = "DEVELOPPEUR";
        } else {
            return List.of();
        }
        Integer inactifs = null;
        Matcher mi = INACTIFS.matcher(t);
        if (mi.find()) {
            inactifs = Integer.parseInt(mi.group(1));
        }
        String filtre = t.contains("tous les developpeurs") || t.contains("tous les comptes developpeur") ? "DEVELOPPEUR" : null;
        List<String> noms = new ArrayList<>();
        for (Utilisateur u : tous) {
            if (mentionne(t, u)) {
                noms.add(u.getNom());
            }
        }
        Integer quota = null;
        Integer quotaAssistant = null;
        if ("DEFINIR_QUOTA".equals(type)) {
            Matcher ma = QUOTA_ASSISTANT.matcher(t);
            Matcher mr = QUOTA_REVUES.matcher(t);
            if (ma.find()) {
                quotaAssistant = Integer.parseInt(ma.group(1));
            }
            if (mr.find()) {
                quota = Integer.parseInt(mr.group(1));
            }
        }
        return List.of(new Intention(type, noms, inactifs, filtre, role, quota, quotaAssistant));
    }

    private static boolean mentionne(String texteNormalise, Utilisateur u) {
        String nom = norm(u.getNom());
        String local = norm(u.getEmail().split("@")[0]);
        if (nom.length() >= 3 && texteNormalise.contains(nom)) {
            return true;
        }
        if (local.length() >= 3 && !MOTS_COMMUNS.contains(local) && Pattern.compile("\\b" + Pattern.quote(local) + "\\b").matcher(texteNormalise).find()) {
            return true;
        }
        for (String mot : nom.split("[^a-z0-9]+")) {
            if (mot.length() >= 3 && !MOTS_COMMUNS.contains(mot) && Pattern.compile("\\b" + Pattern.quote(mot) + "\\b").matcher(texteNormalise).find()) {
                return true;
            }
        }
        return false;
    }

    private void resoudre(Long adminId, Intention i, List<Utilisateur> tous, List<Dtos.ActionPlan> actions, List<String> avertissements) {
        List<Utilisateur> cibles = new ArrayList<>();
        if (i.inactifsJours() != null) {
            LocalDateTime limite = LocalDateTime.now().minusDays(i.inactifsJours());
            for (Utilisateur u : tous) {
                LocalDateTime derniere = u.getDerniereConnexion() != null ? u.getDerniereConnexion() : u.getCreeLe();
                if (derniere.isBefore(limite)) {
                    cibles.add(u);
                }
            }
        } else if (i.noms() != null && !i.noms().isEmpty()) {
            for (String demande : i.noms()) {
                String d = norm(demande);
                List<Utilisateur> trouves = tous.stream().filter(u -> norm(u.getNom()).contains(d) || d.contains(norm(u.getNom()))
                        || norm(u.getEmail()).startsWith(d)).toList();
                if (trouves.isEmpty()) {
                    avertissements.add("Aucun compte ne correspond à « " + demande + " ».");
                }
                trouves.stream().filter(u -> !cibles.contains(u)).forEach(cibles::add);
            }
        } else if (i.filtreRole() != null) {
            tous.stream().filter(u -> u.role().equals(i.filtreRole())).forEach(cibles::add);
        } else {
            avertissements.add("Précisez les comptes concernés (un nom, « les comptes inactifs depuis 30 jours », « tous les développeurs »).");
            return;
        }
        if (cibles.isEmpty() && avertissements.isEmpty()) {
            avertissements.add("Aucun compte ne correspond à cette commande.");
        }
        for (Utilisateur u : cibles) {
            boolean moi = u.getId().equals(adminId);
            switch (i.type()) {
                case "BLOQUER" -> {
                    if (moi || !(u instanceof Developpeur)) {
                        avertissements.add(u.getNom() + " : l'assistant ne bloque que des comptes développeur (jamais un administrateur).");
                    } else if (u.isBloque()) {
                        avertissements.add(u.getNom() + " est déjà bloqué.");
                    } else {
                        actions.add(new Dtos.ActionPlan("BLOQUER", u.getId(), u.getNom(), "Bloquer le compte de " + u.getNom(), null, null, null));
                    }
                }
                case "DEBLOQUER" -> {
                    if (!u.isBloque()) {
                        avertissements.add(u.getNom() + " n'est pas bloqué.");
                    } else {
                        actions.add(new Dtos.ActionPlan("DEBLOQUER", u.getId(), u.getNom(), "Débloquer le compte de " + u.getNom(), null, null, null));
                    }
                }
                case "CHANGER_ROLE" -> {
                    if (i.role() == null || !List.of("ADMIN", "DEVELOPPEUR").contains(i.role())) {
                        avertissements.add("Rôle cible non reconnu (administrateur ou développeur).");
                    } else if (moi) {
                        avertissements.add("Vous ne pouvez pas modifier votre propre rôle.");
                    } else if (u.role().equals(i.role())) {
                        avertissements.add(u.getNom() + " a déjà ce rôle.");
                    } else {
                        actions.add(new Dtos.ActionPlan("CHANGER_ROLE", u.getId(), u.getNom(),
                                "Passer " + u.getNom() + " en " + ("ADMIN".equals(i.role()) ? "administrateur" : "développeur"), i.role(), null, null));
                    }
                }
                case "DEFINIR_QUOTA" -> {
                    if (!(u instanceof Developpeur d)) {
                        avertissements.add(u.getNom() + " : un administrateur n'a pas de quota.");
                    } else if (i.quota() == null && i.quotaAssistant() == null) {
                        avertissements.add("Précisez la valeur du quota (ex. « quota de Sarra à 30 »).");
                        return;
                    } else {
                        actions.add(new Dtos.ActionPlan("DEFINIR_QUOTA", u.getId(), u.getNom(), "Quotas de " + u.getNom() + " : "
                                + (i.quota() != null ? i.quota() + " revues" : d.getQuotaJournalier() + " revues") + ", "
                                + (i.quotaAssistant() != null ? i.quotaAssistant() : d.getQuotaAssistant()) + " requêtes assistant par jour",
                                null, i.quota(), i.quotaAssistant()));
                    }
                }
                default -> avertissements.add("Action non prise en charge : " + i.type());
            }
        }
    }

    private static String decrire(Intention i) {
        String cible = i.inactifsJours() != null ? "comptes inactifs depuis " + i.inactifsJours() + " jours"
                : i.noms() != null && !i.noms().isEmpty() ? String.join(", ", i.noms())
                : i.filtreRole() != null ? "tous les " + ("ADMIN".equals(i.filtreRole()) ? "administrateurs" : "développeurs") : "comptes non précisés";
        return switch (i.type()) {
            case "BLOQUER" -> "Bloquer " + cible;
            case "DEBLOQUER" -> "Débloquer " + cible;
            case "CHANGER_ROLE" -> "Passer " + cible + " en " + ("ADMIN".equals(i.role()) ? "administrateur" : "développeur");
            default -> "Définir les quotas de " + cible;
        };
    }

    /** Exécute les actions confirmées ; chacune est revalidée par le serveur (rien n'est exécuté sur la seule foi du plan). */
    public Dtos.ResultatExecution executer(Long adminId, Dtos.ExecutionRequest req) {
        List<Dtos.ResultatAction> resultats = new ArrayList<>();
        for (Dtos.ActionRequest a : req.actions()) {
            Utilisateur cible = utilisateurs.findById(a.utilisateurId()).orElse(null);
            String nom = cible == null ? "?" : cible.getNom();
            try {
                if (cible == null) {
                    throw new ApiException(ErrorCode.INTROUVABLE, "Utilisateur introuvable.");
                }
                switch (a.type()) {
                    case "BLOQUER" -> {
                        if (!(cible instanceof Developpeur)) {
                            throw new ApiException(ErrorCode.VALIDATION, "L'assistant ne bloque que des comptes développeur.");
                        }
                        admin.bloquerCompte(adminId, cible.getId(), true);
                    }
                    case "DEBLOQUER" -> admin.bloquerCompte(adminId, cible.getId(), false);
                    case "CHANGER_ROLE" -> {
                        if (a.role() == null) {
                            throw new ApiException(ErrorCode.VALIDATION, "Rôle manquant.");
                        }
                        admin.changerRole(adminId, cible.getId(), a.role());
                    }
                    case "DEFINIR_QUOTA" -> {
                        if (!(cible instanceof Developpeur d)) {
                            throw new ApiException(ErrorCode.VALIDATION, "Un administrateur n'a pas de quota.");
                        }
                        admin.definirQuota(adminId, d.getId(), a.quota() != null ? a.quota() : d.getQuotaJournalier(), a.quotaAssistant());
                    }
                    default -> throw new ApiException(ErrorCode.VALIDATION, "Action inconnue.");
                }
                resultats.add(new Dtos.ResultatAction(a.type(), a.utilisateurId(), nom, true, "Fait."));
                log.info("Admin {} : action assistée {} sur le compte {}", adminId, a.type(), a.utilisateurId());
            } catch (ApiException e) {
                resultats.add(new Dtos.ResultatAction(a.type(), a.utilisateurId(), nom, false, e.getMessageUtilisateur()));
            } catch (IllegalArgumentException e) {
                resultats.add(new Dtos.ResultatAction(a.type(), a.utilisateurId(), nom, false, e.getMessage()));
            }
        }
        return new Dtos.ResultatExecution(resultats);
    }

    // ---------- utilitaires

    private Utilisateur trouver(Long id) {
        return utilisateurs.findById(id).orElseThrow(() -> new ApiException(ErrorCode.INTROUVABLE, "Utilisateur introuvable."));
    }

    private static String norm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
