package com.yukai.team.matchservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PlayerAppearanceRequest {

    @NotNull
    @Schema(description = "Player ID", example = "10")
    private Long playerId;

    @NotBlank
    @Schema(description = "Player name snapshot", example = "Alice")
    private String playerNameSnapshot;

    @Schema(description = "Jersey number snapshot", example = "10")
    private Integer jerseyNumberSnapshot;

    @Schema(description = "Position snapshot", example = "FORWARD")
    private String positionSnapshot;

    @NotNull
    @Schema(description = "Whether the player started the match", example = "true")
    private Boolean starter;

    @Min(0)
    @Schema(description = "Minute the player entered", example = "0")
    private Integer onMinute;

    @Min(0)
    @Schema(description = "Minute the player left", example = "90")
    private Integer offMinute;

    @Schema(description = "Optional appearance remark")
    private String remark;
}
