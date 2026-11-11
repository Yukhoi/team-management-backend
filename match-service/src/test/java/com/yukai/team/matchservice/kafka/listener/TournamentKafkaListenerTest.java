package com.yukai.team.matchservice.kafka.listener;

import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.service.TournamentSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.kafka.support.Acknowledgment;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TournamentKafkaListenerTest {
    private final TournamentSyncService sync = mock(TournamentSyncService.class);
    private final TournamentClient client = mock(TournamentClient.class);
    private final Acknowledgment ack = mock(Acknowledgment.class);
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new JacksonConfig().objectMapper();
    private final TournamentKafkaListener listener = new TournamentKafkaListener(mapper, client, sync);

    @Test
    void modernPayloadUsesEnvelopeDataAndAcknowledgesOnlyAfterServiceReturns() throws Exception {
        listener.onMessage(event("tournament.updated", Map.of("tournamentId",5,"name","Ligue B","season","2027","version",3)), ack);
        var ordered = inOrder(sync, ack);
        ordered.verify(sync).synchronizeTournament(5L,"Ligue B","2027",3L); ordered.verify(ack).acknowledge();
        verifyNoInteractions(client);
    }
    @Test
    void transactionFailurePropagatesWithoutAcknowledgment() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(sync).synchronizeTournament(any(), any(), any(), any());
        assertThatThrownBy(() -> listener.onMessage(event("tournament.updated", Map.of("tournamentId",5,"name","Name","season","Season","version",1)), ack)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(ack);
    }
    @Test
    void unrelatedEventTypesAreSafelyIgnored() throws Exception {
        listener.onMessage(event("tournament.finished", Map.of("tournamentId",5)), ack);
        verify(ack).acknowledge(); verifyNoInteractions(sync,client);
    }
    @Test
    void legacyPayloadUsesAuthoritativeVersionWithoutInventingOne() throws Exception {
        var response = new TournamentSnapshotResponse(); response.setId(5L); response.setName("Latest"); response.setSeason("2027"); response.setVersion(8L);
        when(client.getTournamentSnapshot(5L)).thenReturn(response);
        listener.onMessage(event("tournament.updated", Map.of("tournamentId",5)), ack);
        verify(sync).synchronizeTournament(5L,"Latest","2027",8L); verify(ack).acknowledge();
        response.setVersion(null);
        assertThatThrownBy(() -> listener.onMessage(event("tournament.updated",Map.of("tournamentId",5)),ack)).isInstanceOf(IllegalStateException.class);
    }
    @ParameterizedTest
    @ValueSource(strings={"{}","null","not json","{\"tournamentId\":5}","{\"eventId\":\"bad\"}"})
    void malformedIdentityIsRejected(String message) {
        assertThatThrownBy(() -> listener.onMessage(message,ack)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(sync,client,ack);
    }
    @Test
    void inconsistentAndPartialPayloadsAreNotMistakenForLegacyEvents() throws Exception {
        for (var data : java.util.List.of(Map.of("tournamentId",6),Map.of("tournamentId",5,"name","Partial"),Map.of("tournamentId",5,"name","Name","season","Season","version",-1))) {
            assertThatThrownBy(() -> listener.onMessage(event("tournament.updated",data),ack)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(sync,client,ack);
    }
    private String event(String type, Object data) throws Exception {
        return mapper.writeValueAsString(Map.of("eventId",UUID.randomUUID(),"aggregateType","tournament","aggregateId",5,"eventType",type,"occurredAt","2026-11-10T12:00:00Z","operator",Map.of("username","system"),"data",data));
    }
}
