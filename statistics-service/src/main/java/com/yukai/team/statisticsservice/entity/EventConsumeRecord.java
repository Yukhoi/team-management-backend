package com.yukai.team.statisticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        schema = "statistics",
        name = "event_consume_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_consume_record_event_consumer",
                        columnNames = {"event_id", "consumer_name"}
                )
        },
        indexes = {
                @Index(name = "idx_event_consume_record_event_id", columnList = "event_id")
        }
)
public class EventConsumeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @CreationTimestamp
    @Column(name = "consumed_at", nullable = false, updatable = false)
    private OffsetDateTime consumedAt;
}
