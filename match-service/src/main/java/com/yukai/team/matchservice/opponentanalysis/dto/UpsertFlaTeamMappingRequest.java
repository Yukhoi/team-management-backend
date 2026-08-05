package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Upsert FLA team mapping request")
public class UpsertFlaTeamMappingRequest {

    @NotNull
    @Positive
    @Schema(description = "FLA championnat ID", example = "1365", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaChampionnatId;

    @NotNull
    @Positive
    @Schema(description = "FLA saison ID", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaSaisonId;

    @NotNull
    @Positive
    @Schema(description = "FLA team ID", example = "6580", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaTeamId;
}
