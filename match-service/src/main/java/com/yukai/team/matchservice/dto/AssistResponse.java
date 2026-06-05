package com.yukai.team.matchservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Assist response")
public class AssistResponse {

    @Schema(description = "Assist record ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(description = "Goal record ID linked to the assist", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long goalRecordId;
    @Schema(description = "Assisting player ID", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long playerId;
    @Schema(description = "Assisting player name snapshot", example = "Bob", requiredMode = Schema.RequiredMode.REQUIRED)
    private String playerNameSnapshot;
    @Schema(description = "Assisting player jersey number snapshot", example = "8")
    private Integer jerseyNumberSnapshot;
    @Schema(description = "Assist minute", example = "42")
    private Integer assistMinute;
    @Schema(description = "Assist remark", example = "Through ball")
    private String remark;
}
