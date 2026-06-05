package com.yukai.team.statisticsservice.entity;

import com.yukai.team.statisticsservice.entity.enums.HomeAwayType;
import com.yukai.team.statisticsservice.entity.enums.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "match_summary_projection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_summary_match", columnNames = "match_id")
        },
        indexes = {
                @Index(name = "idx_match_summary_match_time", columnList = "match_time"),
                @Index(name = "idx_match_summary_tournament_time", columnList = "tournament_id, match_time")
        }
)
public class MatchSummaryProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "tournament_name_snapshot", nullable = false, length = 100)
    private String tournamentNameSnapshot;

    @Column(name = "season_snapshot", nullable = false, length = 30)
    private String seasonSnapshot;

    @Column(name = "match_time", nullable = false)
    private OffsetDateTime matchTime;

    @Column(name = "opponent_team_name_snapshot", nullable = false, length = 100)
    private String opponentTeamNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "home_away", nullable = false, length = 10)
    private HomeAwayType homeAway;

    @Column(name = "our_score", nullable = false)
    private Integer ourScore;

    @Column(name = "opponent_score", nullable = false)
    private Integer opponentScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    private MatchStatus matchStatus;

    @Column(name = "finished", nullable = false)
    private Boolean finished;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = OffsetDateTime.now();
    }
}
