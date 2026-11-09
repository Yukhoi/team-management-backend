package com.yukai.team.matchservice.opponentanalysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "fla_team_matches_snapshot", schema = "match")
public class FlaTeamMatchesSnapshot {
    /** SHA-256 of the canonical source envelope, including team identity and schema version. */
    @Id
    @Column(name = "payload_hash", length = 64, nullable = false)
    private String payloadHash;

    @Column(name = "fla_team_id", nullable = false)
    private Long flaTeamId;

    @Column(name = "source_saison_id")
    private Long sourceSaisonId;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;
}
