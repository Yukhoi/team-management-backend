package com.yukai.team.teamservice.dto.player;

import com.yukai.team.teamservice.entity.PlayerCurrentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Change player status request")
public class ChangePlayerStatusRequest {

    @Schema(description = "New player status", example = "INJURED")
    @NotNull(message = "newStatus must not be null")
    private PlayerCurrentStatus newStatus;

    @Schema(description = "Operator ID", example = "1001")
    private Long changedBy;

    @Schema(description = "Status change remark", example = "ankle injury")
    private String remark;

    public PlayerCurrentStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PlayerCurrentStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
