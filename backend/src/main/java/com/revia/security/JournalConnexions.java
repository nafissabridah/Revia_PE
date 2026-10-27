package com.revia.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

/**
 * Mémoire courte (en mémoire, vidée au redémarrage) des tentatives de connexion échouées.
 * Sert uniquement à la détection d'usage anormal côté administration ; aucun mot de passe n'est conservé.
 */
@Component
public class JournalConnexions {

    private static final int MAX = 1000;

    public record Echec(Instant date, String email) {
    }

    private final Deque<Echec> echecs = new ConcurrentLinkedDeque<>();

    public void echec(String email) {
        echecs.addLast(new Echec(Instant.now(), email == null ? "" : email.trim().toLowerCase(Locale.ROOT)));
        while (echecs.size() > MAX) {
            echecs.pollFirst();
        }
    }

    public List<Echec> depuis(Duration duree) {
        Instant limite = Instant.now().minus(duree);
        List<Echec> r = new ArrayList<>();
        for (Echec e : echecs) {
            if (e.date().isAfter(limite)) {
                r.add(e);
            }
        }
        return r;
    }
}
