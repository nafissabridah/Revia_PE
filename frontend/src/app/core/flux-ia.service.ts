import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { AssistantReponse, MessageChat, OutilIA } from './models';

export type EvenementFlux =
  | { type: 'morceau'; texte: string }
  | { type: 'fin'; info: Omit<AssistantReponse, 'contenu'> }
  | { type: 'erreur'; message: string };

const HORS_LIGNE = 'Impossible de joindre le serveur RevIA. Vérifiez votre connexion.';
const INATTENDU = "Une erreur inattendue s'est produite. Réessayez plus tard.";

/** Lit un bloc Server-Sent Events (« event:… » + « data:… »). */
export function lireEvenement(bloc: string): EvenementFlux | null {
  let nom = 'message';
  const donnees: string[] = [];
  for (const ligne of bloc.split('\n')) {
    if (ligne.startsWith('event:')) {
      nom = ligne.slice(6).trim();
    } else if (ligne.startsWith('data:')) {
      donnees.push(ligne.slice(5).replace(/^ /, ''));
    }
  }
  if (!donnees.length) {
    return null;
  }
  try {
    const d = JSON.parse(donnees.join('\n'));
    if (nom === 'morceau') return { type: 'morceau', texte: String(d.t ?? '') };
    if (nom === 'fin') return { type: 'fin', info: d };
    if (nom === 'erreur') return { type: 'erreur', message: d.message || INATTENDU };
  } catch {
    return null;
  }
  return null;
}

/**
 * Réponses de l'assistant diffusées en direct : le texte s'affiche dès que Gemini l'écrit.
 * (fetch + ReadableStream, car HttpClient ne lit pas un flux SSE morceau par morceau.)
 */
@Injectable({ providedIn: 'root' })
export class FluxIaService {
  private readonly auth = inject(AuthService);

  discuter(messages: MessageChat[], code?: string, langage?: string): Observable<EvenementFlux> {
    return this.flux('/api/assistant/chat/flux', { messages, code: code || null, langage: langage || null });
  }

  outil(outil: OutilIA, req: { code: string; langage: string; cible?: string }): Observable<EvenementFlux> {
    return this.flux(`/api/assistant/outils/${outil}/flux`, req);
  }

  private flux(url: string, corps: unknown): Observable<EvenementFlux> {
    return new Observable<EvenementFlux>((obs) => {
      const arret = new AbortController();
      const jeton = this.auth.jeton;
      fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...(jeton ? { Authorization: `Bearer ${jeton}` } : {}) },
        body: JSON.stringify(corps),
        signal: arret.signal,
      })
        .then(async (rep) => {
          if (!rep.ok || !rep.body) {
            if (rep.status === 401) {
              this.auth.deconnexion();
            }
            let message = rep.status === 429 ? 'Trop de demandes. Réessayez dans un instant.' : INATTENDU;
            try {
              message = (await rep.json()).message || message;
            } catch {
              /* corps non JSON */
            }
            obs.next({ type: 'erreur', message });
            obs.complete();
            return;
          }
          const lecteur = rep.body.getReader();
          const decodeur = new TextDecoder();
          let tampon = '';
          for (;;) {
            const { value, done } = await lecteur.read();
            if (done) {
              break;
            }
            tampon += decodeur.decode(value, { stream: true }).replace(/\r\n/g, '\n');
            let fin = tampon.indexOf('\n\n');
            while (fin >= 0) {
              const ev = lireEvenement(tampon.slice(0, fin));
              tampon = tampon.slice(fin + 2);
              if (ev) {
                obs.next(ev);
              }
              fin = tampon.indexOf('\n\n');
            }
          }
          const reste = lireEvenement(tampon);
          if (reste) {
            obs.next(reste);
          }
          obs.complete();
        })
        .catch((e: unknown) => {
          if ((e as Error)?.name !== 'AbortError') {
            obs.next({ type: 'erreur', message: HORS_LIGNE });
            obs.complete();
          }
        });
      return () => arret.abort();
    });
  }
}
