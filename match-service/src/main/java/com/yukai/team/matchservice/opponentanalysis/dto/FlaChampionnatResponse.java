package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "FLA championnat cache response")
public class FlaChampionnatResponse {

    @Schema(description = "Internal cache row ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "FLA championnat ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long championnatId;

    @Schema(description = "FLA saison ID", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long saisonId;

    @Schema(description = "Championnat display name", example = "Football Loisir Amateur - Division 1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
