package com.yukai.team.matchservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePlayersRequest {

    @Schema(description = "Player IDs to validate", example = "[1,2]")
    private Set<Long> playerIds;
}
