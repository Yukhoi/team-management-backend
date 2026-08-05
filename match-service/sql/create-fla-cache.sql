CREATE TABLE IF NOT EXISTS match.fla_championnat (
    id BIGSERIAL PRIMARY KEY,
    championnat_id BIGINT NOT NULL,
    saison_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fla_championnat_championnat_saison
        UNIQUE (championnat_id, saison_id)
);

CREATE INDEX IF NOT EXISTS idx_fla_championnat_name
    ON match.fla_championnat (name ASC);

CREATE INDEX IF NOT EXISTS idx_fla_championnat_championnat
    ON match.fla_championnat (championnat_id);

CREATE TABLE IF NOT EXISTS match.fla_team (
    id BIGSERIAL PRIMARY KEY,
    championnat_id BIGINT NOT NULL,
    saison_id BIGINT NOT NULL,
    fla_team_id BIGINT NOT NULL,
    team_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_fla_team_championnat_saison_team
        UNIQUE (championnat_id, saison_id, fla_team_id)
);

CREATE INDEX IF NOT EXISTS idx_fla_team_championnat_name
    ON match.fla_team (championnat_id, team_name ASC);

CREATE INDEX IF NOT EXISTS idx_fla_team_championnat_saison_name
    ON match.fla_team (championnat_id, saison_id, team_name ASC);

CREATE INDEX IF NOT EXISTS idx_fla_team_fla_team_id
    ON match.fla_team (fla_team_id);
