-- Historique des checks : une revue peut être rattachée au snippet analysé
ALTER TABLE revue ADD COLUMN snippet_id BIGINT;
ALTER TABLE revue ADD CONSTRAINT fk_revue_snippet FOREIGN KEY (snippet_id) REFERENCES snippet (id) ON DELETE SET NULL;
CREATE INDEX idx_revue_snippet ON revue (snippet_id);
