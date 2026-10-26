-- Dernière connexion des comptes et prompts partagés (créés par un administrateur, visibles de tous les développeurs)

ALTER TABLE utilisateur ADD COLUMN derniere_connexion TIMESTAMP;

ALTER TABLE prompt_utilisateur ADD COLUMN partage BOOLEAN NOT NULL DEFAULT FALSE;
