package com.yukai.team.matchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "match_info", schema = "match")
public class MatchInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "tournament_name_snapshot", nullable = false, length = 100)
    private String tournamentNameSnapshot;

    @Column(name = "season_snapshot", nullable = false, length = 30)
    private String seasonSnapshot;

    @Column(name = "our_team_id", nullable = false)
    private Long ourTeamId;

    @Column(name = "our_team_name_snapshot", nullable = false, length = 100)
    private String ourTeamNameSnapshot;

    @Column(name = "opponent_team_id", nullable = false)
    private Long opponentTeamId;

    @Column(name = "opponent_team_name_snapshot", nullable = false, length = 100)
    private String opponentTeamNameSnapshot;

    @Column(name = "match_time", nullable = false)
    private OffsetDateTime matchTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "home_away", nullable = false, length = 10)
    private HomeAway homeAway;

    @Column(name = "venue")
    private String venue;

    @Column(name = "round_stage", length = 100)
    private String roundStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    private MatchStatus matchStatus;

    @Column(name = "our_score", nullable = false)
    private Integer ourScore;

    @Column(name = "opponent_score", nullable = false)
    private Integer opponentScore;

    @Column(name = "finished", nullable = false)
    private Boolean finished;

    @Column(name = "created_by")
    private Long createdBy;

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
        if (matchStatus == null) {
            matchStatus = MatchStatus.SCHEDULED;
        }
        if (ourScore == null) {
            ourScore = 0;
        }
        if (opponentScore == null) {
            opponentScore = 0;
        }
        if (finished == null) {
            finished = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
