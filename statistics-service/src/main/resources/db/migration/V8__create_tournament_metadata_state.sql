-- Manual migration; run before deploying Statistics Service. Safe to repeat.
CREATE TABLE IF NOT EXISTS statistics.tournament_metadata_state (
    tournament_id BIGINT PRIMARY KEY,
    last_applied_version BIGINT NOT NULL DEFAULT -1,
    name VARCHAR(100),
    season VARCHAR(30),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
