package com.yukai.team.statisticsservice.dto.response;

import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
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
@Schema(description = "Leaderboard entry response")
public class LeaderboardResponse {

    @Schema(description = "Rank number", example = "1")
    private Integer rankNo;
    @Schema(description = "Ranked entity ID", example = "10")
    private Long entityId;
    @Schema(description = "Ranked entity name", example = "Alice")
    private String entityName;
    @Schema(description = "Metric value", example = "8")
    private Integer metricValue;
    @Schema(description = "Leaderboard board type", example = "GOALS")
    private LeaderboardBoardType boardType;
    @Schema(description = "Season label", example = "2026")
    private String season;
    @Schema(description = "Tournament ID", example = "1")
    private Long tournamentId;
}
