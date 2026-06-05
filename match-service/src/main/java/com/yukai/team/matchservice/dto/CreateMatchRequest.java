package com.yukai.team.matchservice.dto;

import com.yukai.team.matchservice.entity.HomeAway;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateMatchRequest {

    @NotNull
    @Schema(description = "Tournament ID", example = "1")
    private Long tournamentId;

    @NotNull
    @Schema(description = "Our team ID", example = "1")
    private Long ourTeamId;

    @Schema(description = "Our team name snapshot", example = "Team A")
    private String ourTeamNameSnapshot;

    @Schema(description = "Opponent team ID", example = "2")
    private Long opponentTeamId;

    @Schema(description = "Opponent team name when creating an opponent", example = "Team B")
    private String opponentTeamName;

    @NotNull
    @Schema(description = "Scheduled match time")
    private OffsetDateTime matchTime;

    @NotNull
    @Schema(description = "Home or away designation", example = "HOME")
    private HomeAway homeAway;

    @Schema(description = "Venue", example = "Main Stadium")
    private String venue;

    @Schema(description = "Round or stage label", example = "Round 1")
    private String roundStage;
}
