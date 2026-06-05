package com.yukai.team.auditservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "event_consume_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_audit_event_consume",
                columnNames = {"event_id", "consumer_name"}
        )
)
public class EventConsumeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 120)
    private String consumerName;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @PrePersist
    public void prePersist() {
        if (consumedAt == null) {
            consumedAt = OffsetDateTime.now();
        }
    }
}
