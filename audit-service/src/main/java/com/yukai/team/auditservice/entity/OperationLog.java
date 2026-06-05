package com.yukai.team.auditservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "biz_type", nullable = false, length = 80)
    private String bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "operator_username", length = 100)
    private String operatorUsername;

    @Column(name = "operator_name_snapshot", length = 100)
    private String operatorNameSnapshot;

    @Column(name = "trace_id", length = 120)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;

    @Column(name = "operated_at")
    private OffsetDateTime operatedAt;

    @PrePersist
    public void prePersist() {
        if (operatedAt == null) {
            operatedAt = OffsetDateTime.now();
        }
    }
}
