package com.yukai.team.matchservice.dto;

import com.yukai.team.matchservice.entity.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Goal response with optional assist details")
public class GoalResponse {

    @Schema(description = "Goal record ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(description = "Match ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long matchId;
    @Schema(description = "Scoring player ID; null for own goals", example = "10")
    private Long playerId;
    @Schema(description = "Scoring player name snapshot", example = "Alice")
    private String playerNameSnapshot;
    @Schema(description = "Scoring player jersey number snapshot", example = "10")
    private Integer jerseyNumberSnapshot;
    @Schema(description = "Goal minute", example = "42")
    private Integer goalMinute;
    @Schema(description = "Goal type", example = "NORMAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private GoalType goalType;
    @Schema(description = "Goal remark", example = "Header")
    private String remark;
    @Schema(description = "Nested assist details when present")
    private AssistResponse assist;
    @Schema(description = "Goal creation time", example = "2026-06-01T20:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime createdAt;
    @Schema(description = "Goal last update time", example = "2026-06-01T20:31:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime updatedAt;

    @Schema(description = "Frontend-compatible goal ID alias", example = "1")
    public Long getGoalId() {
        return id;
    }

    @Schema(description = "Flattened assist player ID", example = "11")
    public Long getAssistPlayerId() {
        return assist == null ? null : assist.getPlayerId();
    }

    @Schema(description = "Flattened assist player name snapshot", example = "Bob")
    public String getAssistPlayerNameSnapshot() {
        return assist == null ? null : assist.getPlayerNameSnapshot();
    }

    @Schema(description = "Flattened assist jersey number snapshot", example = "8")
    public Integer getAssistJerseyNumberSnapshot() {
        return assist == null ? null : assist.getJerseyNumberSnapshot();
    }
}
