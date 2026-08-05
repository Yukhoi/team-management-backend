package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "FLA team cache response")
public class FlaTeamResponse {

    @Schema(description = "Internal cache row ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "FLA championnat ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long championnatId;

    @Schema(description = "FLA saison ID", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long saisonId;

    @Schema(description = "FLA team ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaTeamId;

    @Schema(description = "Team display name", example = "Paris FC Loisir", requiredMode = Schema.RequiredMode.REQUIRED)
    private String teamName;
}
