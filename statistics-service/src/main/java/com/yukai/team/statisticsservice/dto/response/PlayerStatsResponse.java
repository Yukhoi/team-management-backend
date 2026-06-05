package com.yukai.team.statisticsservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Player statistics response")
public class PlayerStatsResponse {

    @Schema(description = "Player ID", example = "10")
    private Long playerId;
    @Schema(description = "Player name", example = "Alice")
    private String playerName;
    @Schema(description = "Season label", example = "2026")
    private String season;
    @Schema(description = "Tournament ID", example = "1")
    private Long tournamentId;
    @Schema(description = "Tournament name", example = "Summer League")
    private String tournamentName;
    @Schema(description = "Appearance count", example = "12")
    private Integer appearances;
    @Schema(description = "Start count", example = "10")
    private Integer starts;
    @Schema(description = "Goal count", example = "8")
    private Integer goals;
    @Schema(description = "Assist count", example = "5")
    private Integer assists;
    @Schema(description = "Goals plus assists", example = "13")
    private Integer goalInvolvements;
}
