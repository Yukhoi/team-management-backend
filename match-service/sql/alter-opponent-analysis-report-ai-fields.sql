ALTER TABLE match.opponent_analysis_report
    ADD COLUMN IF NOT EXISTS error_code VARCHAR(100);

ALTER TABLE match.opponent_analysis_report
    ADD COLUMN IF NOT EXISTS prompt_tokens INTEGER;

ALTER TABLE match.opponent_analysis_report
    ADD COLUMN IF NOT EXISTS completion_tokens INTEGER;

ALTER TABLE match.opponent_analysis_report
    ADD COLUMN IF NOT EXISTS total_tokens INTEGER;

CREATE INDEX IF NOT EXISTS idx_opponent_analysis_report_reuse
    ON match.opponent_analysis_report (
        match_id,
        snapshot_id,
        provider,
        model,
        prompt_version,
        language,
        status,
        created_at DESC
    );
