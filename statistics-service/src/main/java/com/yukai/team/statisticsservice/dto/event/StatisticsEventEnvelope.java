package com.yukai.team.statisticsservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatisticsEventEnvelope {

    private UUID eventId;

    private String eventType;

    private String aggregateType;

    private Long aggregateId;

    private OffsetDateTime occurredAt;

    private JsonNode data;
}
