package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@Schema(description = "FLA standings sync result")
public class FlaSyncResponse {

    @Schema(description = "FLA championnat ID", example = "1365", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long championnatId;

    @Schema(description = "FLA saison ID", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long saisonId;

    @Schema(description = "Championnat display name stored in cache", example = "Division 3", requiredMode = Schema.RequiredMode.REQUIRED)
    private String championnatName;

    @Schema(description = "Number of standing entries received from FLA before de-duplication", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer receivedTeamCount;

    @Schema(description = "Number of FLA team cache rows created", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer createdTeamCount;

    @Schema(description = "Number of FLA team cache rows updated", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer updatedTeamCount;

    @Schema(description = "Number of unchanged FLA team cache rows", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer unchangedTeamCount;

    @Schema(description = "Synchronization completion time", example = "2026-08-04T21:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime syncedAt;
}
