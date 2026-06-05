package com.yukai.team.auditservice.dto;

import com.yukai.team.auditservice.entity.OperationLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Audit log response")
public class AuditLogResponse {

    @Schema(description = "Audit log ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(description = "Domain event ID", example = "4f5efefb-9b9f-42e0-8e0c-8bfb0704e9e7")
    private UUID eventId;
    @Schema(description = "Domain event type", example = "match.goal.created", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventType;
    @Schema(description = "Aggregate type stored in operation_log.biz_type", example = "match", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;
    @Schema(description = "Aggregate ID stored in operation_log.biz_id", example = "1")
    private Long bizId;
    @Schema(description = "Operator user ID", example = "1001")
    private Long operatorUserId;
    @Schema(description = "Operator username", example = "coach")
    private String operatorUsername;
    @Schema(description = "Operator display name snapshot", example = "Coach One")
    private String operatorNameSnapshot;
    @Schema(description = "Trace ID when available", example = "trace-123")
    private String traceId;
    @Schema(description = "JSON snapshot before operation", example = "{\"score\":1}")
    private String beforeData;
    @Schema(description = "JSON snapshot after operation", example = "{\"score\":2}")
    private String afterData;
    @Schema(description = "Operation occurrence time", example = "2026-06-01T20:30:00Z")
    private OffsetDateTime operatedAt;

    @Schema(description = "Frontend-compatible aggregate type alias", example = "match")
    public String getAggregateType() {
        return bizType;
    }

    @Schema(description = "Frontend-compatible aggregate ID alias", example = "1")
    public Long getAggregateId() {
        return bizId;
    }

    @Schema(description = "Frontend-compatible occurrence time alias", example = "2026-06-01T20:30:00Z")
    public OffsetDateTime getOccurredAt() {
        return operatedAt;
    }

    @Schema(description = "Frontend-compatible username alias", example = "coach")
    public String getUsername() {
        return operatorUsername;
    }

    @Schema(description = "Frontend-compatible user ID alias", example = "1001")
    public Long getUserId() {
        return operatorUserId;
    }

    @Schema(description = "Frontend-compatible operation data alias using afterData", example = "{\"goalId\":1}")
    public String getData() {
        return afterData;
    }

    @Schema(description = "Frontend-compatible created time alias", example = "2026-06-01T20:30:00Z")
    public OffsetDateTime getCreatedAt() {
        return operatedAt;
    }

    public static AuditLogResponse from(OperationLog operationLog) {
        return AuditLogResponse.builder()
                .id(operationLog.getId())
                .eventId(operationLog.getEventId())
                .eventType(operationLog.getEventType())
                .bizType(operationLog.getBizType())
                .bizId(operationLog.getBizId())
                .operatorUserId(operationLog.getOperatorUserId())
                .operatorUsername(operationLog.getOperatorUsername())
                .operatorNameSnapshot(operationLog.getOperatorNameSnapshot())
                .traceId(operationLog.getTraceId())
                .beforeData(operationLog.getBeforeData())
                .afterData(operationLog.getAfterData())
                .operatedAt(operationLog.getOperatedAt())
                .build();
    }
}
