package com.yukai.team.statisticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        schema = "statistics",
        name = "match_player_appearance_projection",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_player_appearance",
                        columnNames = {"match_id", "player_id"}
                )
        },
        indexes = {
                @Index(name = "idx_match_player_appearance_match", columnList = "match_id"),
                @Index(name = "idx_match_player_appearance_player", columnList = "player_id")
        }
)
public class MatchPlayerAppearanceProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "season", nullable = false, length = 30)
    private String season;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Builder.Default
    @Column(name = "appearance_count", nullable = false)
    private Integer appearanceCount = 0;

    @Builder.Default
    @Column(name = "starter_count", nullable = false)
    private Integer starterCount = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now();
    }
}
