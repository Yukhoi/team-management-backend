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
        name = "team_stats_projection",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_stats_team_season_tournament",
                        columnNames = {"team_id", "season", "tournament_id"}
                )
        },
        indexes = {
                @Index(name = "idx_team_stats_season_tournament", columnList = "season, tournament_id")
        }
)
public class TeamStatsProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "team_name_snapshot", nullable = false, length = 100)
    private String teamNameSnapshot;

    @Column(name = "season", nullable = false, length = 30)
    private String season;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "tournament_name_snapshot", nullable = false, length = 100)
    private String tournamentNameSnapshot;

    @Builder.Default
    @Column(name = "played", nullable = false)
    private Integer played = 0;

    @Builder.Default
    @Column(name = "win", nullable = false)
    private Integer win = 0;

    @Builder.Default
    @Column(name = "draw", nullable = false)
    private Integer draw = 0;

    @Builder.Default
    @Column(name = "lose", nullable = false)
    private Integer lose = 0;

    @Builder.Default
    @Column(name = "goals_for", nullable = false)
    private Integer goalsFor = 0;

    @Builder.Default
    @Column(name = "goals_against", nullable = false)
    private Integer goalsAgainst = 0;

    @Builder.Default
    @Column(name = "goal_diff", nullable = false)
    private Integer goalDiff = 0;

    @Builder.Default
    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public void applyResult(int ourScore, int opponentScore) {
        played = safe(played) + 1;
        goalsFor = safe(goalsFor) + ourScore;
        goalsAgainst = safe(goalsAgainst) + opponentScore;
        points = safe(points) + calculatePoints(ourScore, opponentScore);

        if (ourScore > opponentScore) {
            win = safe(win) + 1;
        } else if (ourScore == opponentScore) {
            draw = safe(draw) + 1;
        } else {
            lose = safe(lose) + 1;
        }
        recalculateGoalDiff();
    }

    public void rollbackResult(int ourScore, int opponentScore) {
        played = nonNegativeDelta(played, -1);
        goalsFor = nonNegativeDelta(goalsFor, -ourScore);
        goalsAgainst = nonNegativeDelta(goalsAgainst, -opponentScore);
        points = nonNegativeDelta(points, -calculatePoints(ourScore, opponentScore));

        if (ourScore > opponentScore) {
            win = nonNegativeDelta(win, -1);
        } else if (ourScore == opponentScore) {
            draw = nonNegativeDelta(draw, -1);
        } else {
            lose = nonNegativeDelta(lose, -1);
        }
        recalculateGoalDiff();
    }

    public void recalculateGoalDiff() {
        goalDiff = safe(goalsFor) - safe(goalsAgainst);
    }

    private int calculatePoints(int ourScore, int opponentScore) {
        if (ourScore > opponentScore) {
            return 3;
        }
        if (ourScore == opponentScore) {
            return 1;
        }
        return 0;
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now();
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private int nonNegativeDelta(Integer value, int delta) {
        return Math.max(0, safe(value) + delta);
    }
}
