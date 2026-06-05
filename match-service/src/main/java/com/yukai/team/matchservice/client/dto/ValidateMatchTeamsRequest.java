package com.yukai.team.matchservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateMatchTeamsRequest {

    @Schema(description = "Our team ID", example = "1")
    private Long ourTeamId;
    @Schema(description = "Opponent team ID", example = "2")
    private Long opponentTeamId;
}
