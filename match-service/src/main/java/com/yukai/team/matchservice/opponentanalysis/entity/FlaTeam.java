package com.yukai.team.matchservice.opponentanalysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "fla_team",
        schema = "match",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fla_team_championnat_saison_team",
                columnNames = {"championnat_id", "saison_id", "fla_team_id"}
        )
)
public class FlaTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "championnat_id", nullable = false)
    private Long championnatId;

    @Column(name = "saison_id", nullable = false)
    private Long saisonId;

    @Column(name = "fla_team_id", nullable = false)
    private Long flaTeamId;

    @Column(name = "team_name", nullable = false, length = 150)
    private String teamName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
