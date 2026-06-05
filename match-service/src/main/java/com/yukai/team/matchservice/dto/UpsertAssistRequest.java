package com.yukai.team.matchservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpsertAssistRequest {

    @NotNull
    @Schema(description = "Assisting player ID", example = "11")
    private Long playerId;

    @Min(0)
    @Schema(description = "Assist minute", example = "42")
    private Integer assistMinute;

    @Schema(description = "Optional assist remark")
    private String remark;
}
