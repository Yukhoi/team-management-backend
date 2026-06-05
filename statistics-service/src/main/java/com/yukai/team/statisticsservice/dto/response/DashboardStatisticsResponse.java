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
@Schema(description = "Dashboard statistics response")
public class DashboardStatisticsResponse {

    @Schema(description = "Total matches", example = "12")
    private Integer totalMatches;
    @Schema(description = "Wins", example = "8")
    private Integer wins;
    @Schema(description = "Draws", example = "2")
    private Integer draws;
    @Schema(description = "Losses", example = "2")
    private Integer losses;
    @Schema(description = "Goals scored", example = "25")
    private Integer goals;
    @Schema(description = "Goals conceded", example = "11")
    private Integer goalsAgainst;
    @Schema(description = "Top scorer name", example = "Alice")
    private String topScorer;
    @Schema(description = "Top scorer goal count", example = "9")
    private Integer topScorerGoals;
    @Schema(description = "Top assist provider name", example = "Bob")
    private String topAssist;
    @Schema(description = "Top assist count", example = "6")
    private Integer topAssistCount;
}
