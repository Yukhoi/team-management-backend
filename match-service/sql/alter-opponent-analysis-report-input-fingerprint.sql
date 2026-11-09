-- Run against the existing match schema before deploying the v2 application.
-- Idempotent; historical rows intentionally remain NULL and are never backfilled.
ALTER TABLE match.opponent_analysis_report
    ADD COLUMN IF NOT EXISTS metrics_input_fingerprint VARCHAR(64);
ALTER TABLE match.opponent_analysis_report
    ADD COLUMN IF NOT EXISTS ai_input_json TEXT;
CREATE INDEX IF NOT EXISTS idx_opponent_analysis_report_input_reuse
    ON match.opponent_analysis_report
    (match_id, metrics_input_fingerprint, provider, model, prompt_version, language, status, created_at DESC, id DESC);
