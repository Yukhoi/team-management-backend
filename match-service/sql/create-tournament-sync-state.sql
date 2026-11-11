-- Apply manually before deploying Match Service. Safe to repeat; preserves applied versions.
CREATE TABLE IF NOT EXISTS match.tournament_sync_state (
    tournament_id BIGINT PRIMARY KEY,
    last_applied_version BIGINT NOT NULL DEFAULT -1,
    name VARCHAR(100),
    season VARCHAR(30),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_match_info_tournament_sync ON match.match_info (tournament_id);
