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
        name = "player_stats_projection",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_player_stats_player_season_tournament",
                        columnNames = {"player_id", "season", "tournament_id"}
                )
        },
        indexes = {
                @Index(name = "idx_player_stats_season_tournament", columnList = "season, tournament_id")
        }
)
public class PlayerStatsProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "player_name_snapshot", nullable = false, length = 100)
    private String playerNameSnapshot;

    @Column(name = "season", nullable = false, length = 30)
    private String season;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "tournament_name_snapshot", nullable = false, length = 100)
    private String tournamentNameSnapshot;

    @Builder.Default
    @Column(name = "appearances", nullable = false)
    private Integer appearances = 0;

    @Builder.Default
    @Column(name = "starts", nullable = false)
    private Integer starts = 0;

    @Builder.Default
    @Column(name = "goals", nullable = false)
    private Integer goals = 0;

    @Builder.Default
    @Column(name = "assists", nullable = false)
    private Integer assists = 0;

    @Builder.Default
    @Column(name = "goal_involvements", nullable = false)
    private Integer goalInvolvements = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public void increaseGoals(int delta) {
        goals = safe(goals) + delta;
        recalculateGoalInvolvements();
    }

    public void increaseAssists(int delta) {
        assists = safe(assists) + delta;
        recalculateGoalInvolvements();
    }

    public void increaseAppearances(int delta) {
        appearances = safe(appearances) + delta;
    }

    public void increaseStarts(int delta) {
        starts = safe(starts) + delta;
    }

    public void recalculateGoalInvolvements() {
        goalInvolvements = safe(goals) + safe(assists);
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now();
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
