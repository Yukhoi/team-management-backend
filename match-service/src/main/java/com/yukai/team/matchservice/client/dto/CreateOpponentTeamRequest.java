package com.yukai.team.matchservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOpponentTeamRequest {

    @Schema(description = "Opponent team name", example = "Team B")
    private String name;
}
