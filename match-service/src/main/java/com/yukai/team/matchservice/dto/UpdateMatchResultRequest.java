package com.yukai.team.matchservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateMatchResultRequest {

    @NotNull
    @Min(0)
    @Schema(description = "Our team score", example = "2")
    private Integer ourScore;

    @NotNull
    @Min(0)
    @Schema(description = "Opponent score", example = "1")
    private Integer opponentScore;

    @NotNull
    @Schema(description = "Whether the match is finished", example = "true")
    private Boolean finished;

    @Schema(description = "Optional result remark")
    private String remark;
}
