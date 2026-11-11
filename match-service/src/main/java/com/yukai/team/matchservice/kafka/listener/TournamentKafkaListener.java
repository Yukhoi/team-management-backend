package com.yukai.team.matchservice.kafka.listener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.service.TournamentSyncService;
import com.yukai.team.matchservice.service.TournamentSyncServiceImpl;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TournamentKafkaListener {
    private final ObjectMapper mapper;
    private final TournamentClient client;
    private final TournamentSyncService sync;

    @KafkaListener(topics = "${app.kafka.topics.tournament-events}",
            groupId = "${app.tournament-sync.group-id:match-service-tournament-sync}",
            containerFactory = "tournamentKafkaListenerContainerFactory")
    public void onMessage(String message, Acknowledgment acknowledgment) {
        Envelope event = deserialize(message);
        try {
            if (event == null || event.eventId() == null || event.occurredAt() == null
                    || event.eventType() == null || event.eventType().isBlank()
                    || !"tournament".equals(event.aggregateType()) || event.aggregateId() == null || event.aggregateId() <= 0) {
                throw new IllegalArgumentException("Invalid tournament DomainEvent identity");
            }
            if (!"tournament.updated".equals(event.eventType())) {
                log.debug("Ignored tournament event, eventId={}, tournamentId={}, eventType={}", event.eventId(), event.aggregateId(), event.eventType());
                acknowledgment.acknowledge();
                return;
            }
            var data = event.data();
            if (data == null || !data.isObject() || (!data.path("tournamentId").isIntegralNumber() || !data.path("tournamentId").canConvertToLong())
                    || data.path("tournamentId").longValue() != event.aggregateId()) {
                throw new IllegalArgumentException("Tournament event payload/aggregate identity mismatch");
            }
            Long id = event.aggregateId();
            String name;
            String season;
            Long version;
            if (!data.has("name") && !data.has("season") && !data.has("version")) {
                TournamentSnapshotResponse latest = client.getTournamentSnapshot(id);
                if (latest == null || !id.equals(latest.getId())) {
                    throw new IllegalStateException("Authoritative tournament snapshot identity mismatch");
                }
                name = latest.getName(); season = latest.getSeason(); version = latest.getVersion();
                // An old Tournament deployment without version cannot safely satisfy a legacy event.
                if (version == null) throw new IllegalStateException("Authoritative tournament snapshot has no version");
            } else {
                if (!data.path("name").isTextual() || !data.path("season").isTextual() || (!data.path("version").isIntegralNumber() || !data.path("version").canConvertToLong())) {
                    throw new IllegalArgumentException("Incomplete versioned tournament payload");
                }
                name = data.path("name").asText(); season = data.path("season").asText(); version = data.path("version").longValue();
            }
            TournamentSyncServiceImpl.validate(id, name, season, version);
            sync.synchronizeTournament(id, name, season, version);
            // The proxied service has committed before acknowledging on the consumer thread.
            acknowledgment.acknowledge();
            log.info("Tournament event applied or superseded, eventId={}, tournamentId={}, eventType={}, version={}", event.eventId(), id, event.eventType(), version);
        } catch (RuntimeException ex) {
            log.error("Tournament event failed, eventId={}, tournamentId={}, eventType={}, reason={}",
                    event == null ? null : event.eventId(), event == null ? null : event.aggregateId(),
                    event == null ? null : event.eventType(), ex.getMessage());
            throw ex;
        }
    }

    private Envelope deserialize(String message) {
        try {
            return mapper.readerFor(Envelope.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(message);
        } catch (JsonProcessingException ex) {
            log.error("Malformed tournament DomainEvent: {}", ex.getOriginalMessage());
            throw new IllegalArgumentException("Malformed tournament DomainEvent", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(UUID eventId, String eventType, String aggregateType, Long aggregateId,
                           OffsetDateTime occurredAt, JsonNode data) { }
}
