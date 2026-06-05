package com.yukai.team.matchservice.dto;

import com.yukai.team.matchservice.entity.GoalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateGoalRequest {

    @Schema(description = "Scoring player ID", example = "10")
    private Long playerId;

    @Min(0)
    @Schema(description = "Goal minute", example = "42")
    private Integer goalMinute;

    @NotNull
    @Schema(description = "Goal type", example = "NORMAL")
    private GoalType goalType;

    @Schema(description = "Optional goal remark")
    private String remark;
}
