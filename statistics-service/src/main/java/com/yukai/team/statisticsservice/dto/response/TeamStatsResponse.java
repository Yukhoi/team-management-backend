package com.yukai.team.statisticsservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Team statistics response")
public class TeamStatsResponse {

    @Schema(description = "Rank number", example = "1")
    private Integer rankNo;
    @Schema(description = "Team ID", example = "1")
    private Long teamId;
    @Schema(description = "Team name", example = "Team A")
    private String teamName;
    @Schema(description = "Played matches", example = "12")
    private Integer played;
    @Schema(description = "Wins", example = "8")
    private Integer win;
    @Schema(description = "Draws", example = "2")
    private Integer draw;
    @Schema(description = "Losses", example = "2")
    private Integer lose;
    @Schema(description = "Goals for", example = "25")
    private Integer goalsFor;
    @Schema(description = "Goals against", example = "11")
    private Integer goalsAgainst;
    @Schema(description = "Goal difference", example = "14")
    private Integer goalDiff;
    @Schema(description = "Points", example = "26")
    private Integer points;
    @Schema(description = "Season label", example = "2026")
    private String season;
    @Schema(description = "Tournament ID", example = "1")
    private Long tournamentId;
    @Schema(description = "Tournament name", example = "Summer League")
    private String tournamentName;
}
