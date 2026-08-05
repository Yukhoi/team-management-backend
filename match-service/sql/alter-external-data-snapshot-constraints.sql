DROP INDEX IF EXISTS match.uk_external_data_snapshot_payload_hash;

CREATE UNIQUE INDEX IF NOT EXISTS uk_external_data_snapshot_provider_context_hash
    ON match.external_data_snapshot (provider, championnat_id, saison_id, payload_hash);

CREATE INDEX IF NOT EXISTS idx_external_data_snapshot_provider_context_fetched
    ON match.external_data_snapshot (provider, championnat_id, saison_id, fetched_at DESC);
