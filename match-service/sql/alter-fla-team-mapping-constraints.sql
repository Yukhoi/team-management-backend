DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_fla_team_mapping_external_team_per_tournament'
          AND conrelid = 'match.fla_team_mapping'::regclass
    ) THEN
        ALTER TABLE match.fla_team_mapping
            ADD CONSTRAINT uk_fla_team_mapping_external_team_per_tournament
            UNIQUE (internal_tournament_id, fla_championnat_id, fla_saison_id, fla_team_id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_fla_team_mapping_internal_tournament
    ON match.fla_team_mapping (internal_tournament_id);

CREATE INDEX IF NOT EXISTS idx_fla_team_mapping_external_team
    ON match.fla_team_mapping (fla_championnat_id, fla_saison_id, fla_team_id);
