package com.yukai.team.teamservice.internal;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateOpponentTeamRequest {

    @NotBlank
    @Schema(description = "Opponent team name", example = "Team B")
    private String name;
}
