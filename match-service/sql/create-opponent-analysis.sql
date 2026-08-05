CREATE TABLE IF NOT EXISTS match.fla_team_mapping (
    id BIGSERIAL PRIMARY KEY,
    internal_tournament_id BIGINT NOT NULL,
    internal_team_id BIGINT NOT NULL,
    fla_championnat_id BIGINT NOT NULL,
    fla_saison_id BIGINT NOT NULL,
    fla_team_id BIGINT NOT NULL,
    fla_team_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fla_team_mapping_internal_tournament_team
        UNIQUE (internal_tournament_id, internal_team_id)
);

CREATE INDEX IF NOT EXISTS idx_fla_team_mapping_fla_team
    ON match.fla_team_mapping (fla_championnat_id, fla_saison_id, fla_team_id);

CREATE TABLE IF NOT EXISTS match.external_data_snapshot (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    championnat_id BIGINT NOT NULL,
    saison_id BIGINT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    raw_payload TEXT NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_external_data_snapshot_payload_hash
    ON match.external_data_snapshot (payload_hash);

CREATE INDEX IF NOT EXISTS idx_external_data_snapshot_provider_competition
    ON match.external_data_snapshot (provider, championnat_id, saison_id, fetched_at DESC);

CREATE TABLE IF NOT EXISTS match.opponent_analysis_report (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL,
    opponent_team_id BIGINT NOT NULL,
    snapshot_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    prompt_version VARCHAR(50),
    language VARCHAR(20) NOT NULL DEFAULT 'zh-CN',
    status VARCHAR(20) NOT NULL,
    metrics_json TEXT,
    report_json TEXT,
    error_message TEXT,
    source_fetched_at TIMESTAMP WITH TIME ZONE,
    generated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_opponent_analysis_report_match_created
    ON match.opponent_analysis_report (match_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_opponent_analysis_report_opponent_team
    ON match.opponent_analysis_report (opponent_team_id);

CREATE INDEX IF NOT EXISTS idx_opponent_analysis_report_snapshot
    ON match.opponent_analysis_report (snapshot_id);

CREATE INDEX IF NOT EXISTS idx_opponent_analysis_report_status_created
    ON match.opponent_analysis_report (status, created_at ASC);
