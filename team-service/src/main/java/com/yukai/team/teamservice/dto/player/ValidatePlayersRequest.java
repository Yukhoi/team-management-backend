package com.yukai.team.teamservice.dto.player;

import jakarta.validation.constraints.NotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
public class ValidatePlayersRequest {

    @NotEmpty
    @Schema(description = "Player IDs to validate", example = "[1,2]")
    private Set<Long> playerIds;
}
