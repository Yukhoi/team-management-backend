package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@Schema(description = "FLA team mapping response")
public class FlaTeamMappingResponse {

    @Schema(description = "Mapping ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Internal tournament ID", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long internalTournamentId;

    @Schema(description = "Internal team ID", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long internalTeamId;

    @Schema(description = "Internal team display name", example = "YEXIAO PARIS FC")
    private String internalTeamName;

    @Schema(description = "FLA championnat ID", example = "1365", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaChampionnatId;

    @Schema(description = "FLA saison ID", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaSaisonId;

    @Schema(description = "FLA team ID", example = "6580", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long flaTeamId;

    @Schema(description = "FLA team display name", example = "YEXIAO PARIS FC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String flaTeamName;

    @Schema(description = "Creation time", example = "2026-08-04T21:30:00Z")
    private OffsetDateTime createdAt;

    @Schema(description = "Last update time", example = "2026-08-04T21:40:00Z")
    private OffsetDateTime updatedAt;

    @Schema(description = "Optimistic lock version", example = "0")
    private Long version;
}
