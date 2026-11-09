-- Apply manually using the repository's existing match-service/sql deployment convention.
-- Separate from external_data_snapshot: no standings payloads or mappings are changed.
CREATE TABLE IF NOT EXISTS match.fla_team_matches_snapshot (
    payload_hash VARCHAR(64) PRIMARY KEY,
    fla_team_id BIGINT NOT NULL,
    source_saison_id BIGINT,
    raw_payload TEXT NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fla_team_matches_snapshot_team_fetched
    ON match.fla_team_matches_snapshot (fla_team_id, fetched_at DESC, payload_hash DESC);
