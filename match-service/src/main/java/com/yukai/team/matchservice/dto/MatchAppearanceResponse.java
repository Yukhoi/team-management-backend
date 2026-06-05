package com.yukai.team.matchservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Match appearance response")
public class MatchAppearanceResponse {

    @Schema(description = "Appearance record ID", example = "1")
    private Long id;
    @Schema(description = "Player ID", example = "10")
    private Long playerId;
    @Schema(description = "Player name snapshot", example = "Alice")
    private String playerNameSnapshot;
    @Schema(description = "Jersey number snapshot", example = "10")
    private Integer jerseyNumberSnapshot;
    @Schema(description = "Position snapshot", example = "FORWARD")
    private String positionSnapshot;
    @Schema(description = "Whether the player appeared", example = "true")
    private Boolean appeared;
    @Schema(description = "Whether the player started", example = "true")
    private Boolean starter;
    @Schema(description = "Minute the player entered", example = "0")
    private Integer onMinute;
    @Schema(description = "Minute the player left", example = "90")
    private Integer offMinute;
    @Schema(description = "Appearance remark", example = "Full match")
    private String remark;
}
