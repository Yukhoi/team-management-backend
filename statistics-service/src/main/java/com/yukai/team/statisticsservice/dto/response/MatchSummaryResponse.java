package com.yukai.team.statisticsservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@Schema(description = "Match statistics summary response")
public class MatchSummaryResponse {

    @Schema(description = "Match ID", example = "1")
    private Long matchId;
    @Schema(description = "Tournament ID", example = "1")
    private Long tournamentId;
    @Schema(description = "Tournament name", example = "Summer League")
    private String tournamentName;
    @Schema(description = "Season label", example = "2026")
    private String season;
    @Schema(description = "Match time", example = "2026-06-01T20:00:00Z")
    private OffsetDateTime matchTime;
    @Schema(description = "Opponent team name", example = "Team B")
    private String opponentTeamName;
    @Schema(description = "Home or away value", example = "HOME")
    private String homeAway;
    @Schema(description = "Our score", example = "2")
    private Integer ourScore;
    @Schema(description = "Opponent score", example = "1")
    private Integer opponentScore;
    @Schema(description = "Match status", example = "FINISHED")
    private String matchStatus;
    @Schema(description = "Whether the match is finished", example = "true")
    private Boolean finished;
}
