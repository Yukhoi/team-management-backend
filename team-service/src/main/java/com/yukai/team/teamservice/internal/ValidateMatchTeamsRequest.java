package com.yukai.team.teamservice.internal;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ValidateMatchTeamsRequest {

    @NotNull
    @Schema(description = "Our team ID", example = "1")
    private Long ourTeamId;

    @Schema(description = "Opponent team ID", example = "2")
    private Long opponentTeamId;
}
