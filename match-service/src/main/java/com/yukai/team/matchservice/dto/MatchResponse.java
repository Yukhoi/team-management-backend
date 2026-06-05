package com.yukai.team.matchservice.dto;

import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Match response")
public class MatchResponse {

    @Schema(description = "Match ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(description = "Tournament ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tournamentId;
    @Schema(description = "Tournament name snapshot", example = "Summer League", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tournamentNameSnapshot;
    @Schema(description = "Season snapshot", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private String seasonSnapshot;
    @Schema(description = "Our team ID", example = "1")
    private Long ourTeamId;
    @Schema(description = "Our team name snapshot", example = "Team A")
    private String ourTeamNameSnapshot;
    @Schema(description = "Opponent team ID", example = "2")
    private Long opponentTeamId;
    @Schema(description = "Opponent team name snapshot", example = "Team B")
    private String opponentTeamNameSnapshot;
    @Schema(description = "Scheduled match time", example = "2026-06-01T20:00:00Z")
    private OffsetDateTime matchTime;
    @Schema(description = "Home or away designation", example = "HOME")
    private HomeAway homeAway;
    @Schema(description = "Venue", example = "Main Stadium")
    private String venue;
    @Schema(description = "Round or stage label", example = "Round 1")
    private String roundStage;
    @Schema(description = "Match lifecycle status", example = "FINISHED")
    private MatchStatus matchStatus;
    @Schema(description = "Our team score", example = "2")
    private Integer ourScore;
    @Schema(description = "Opponent score", example = "1")
    private Integer opponentScore;
    @Schema(description = "Whether the match is finished", example = "true")
    private Boolean finished;
    @Schema(description = "Creation time", example = "2026-06-01T18:00:00Z")
    private OffsetDateTime createdAt;
    @Schema(description = "Last update time", example = "2026-06-01T22:00:00Z")
    private OffsetDateTime updatedAt;
    @Schema(description = "Optimistic lock version", example = "0")
    private Long version;
    @Schema(description = "Player appearances recorded for the match")
    private List<MatchAppearanceResponse> appearances;

    @Schema(description = "Frontend-compatible match ID alias", example = "1")
    public Long getMatchId() {
        return id;
    }

    @Schema(description = "Opponent display name", example = "Team B")
    public String getOpponentName() {
        return opponentTeamNameSnapshot;
    }

    @Schema(description = "Frontend-compatible match date alias", example = "2026-06-01T20:00:00Z")
    public OffsetDateTime getMatchDate() {
        return matchTime;
    }

    @Schema(description = "Frontend-compatible status alias", example = "FINISHED")
    public MatchStatus getStatus() {
        return matchStatus;
    }

    @Schema(description = "Home team score", example = "2")
    public Integer getHomeScore() {
        return homeAway == HomeAway.HOME ? ourScore : opponentScore;
    }

    @Schema(description = "Away team score", example = "1")
    public Integer getAwayScore() {
        return homeAway == HomeAway.HOME ? opponentScore : ourScore;
    }

    @Schema(description = "Result from our team perspective", example = "WIN")
    public String getResult() {
        if (!Boolean.TRUE.equals(finished) || ourScore == null || opponentScore == null) {
            return null;
        }
        if (ourScore.equals(opponentScore)) {
            return "DRAW";
        }
        return ourScore > opponentScore ? "WIN" : "LOSS";
    }
}
