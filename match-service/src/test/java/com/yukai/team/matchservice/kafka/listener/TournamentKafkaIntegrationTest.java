package com.yukai.team.matchservice.kafka.listener;

import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.config.KafkaProducerConfig;
import com.yukai.team.matchservice.config.TournamentKafkaConsumerConfig;
import com.yukai.team.matchservice.service.TournamentSyncService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(TournamentKafkaIntegrationTest.Config.class)
@EmbeddedKafka(partitions=1, topics={"sync-test","sync-test.DLT"}, bootstrapServersProperty="spring.kafka.bootstrap-servers")
@TestPropertySource(properties={"app.kafka.topics.tournament-events=sync-test","app.tournament-sync.group-id=sync-integration"})
@DirtiesContext
class TournamentKafkaIntegrationTest {
    @Autowired KafkaTemplate<String,String> kafka;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired TournamentSyncService sync;

    @Test
    void retriesTransientFailureQuarantinesMalformedMessageAndContinuesConsumption() throws Exception {
        var calls = new AtomicInteger();
        var applied = new CountDownLatch(1);
        var afterPoison = new CountDownLatch(1);
        doAnswer(invocation -> {
            Long version = invocation.getArgument(3);
            if (version == 1L) {
                if (calls.incrementAndGet() == 1) throw new DataAccessResourceFailureException("temporary database outage");
                applied.countDown();
            } else if (version == 3L) {
                throw new DataAccessResourceFailureException("persistent database outage");
            } else afterPoison.countDown();
            return null;
        }).when(sync).synchronizeTournament(anyLong(),anyString(),anyString(),anyLong());
        var props = KafkaTestUtils.consumerProps("sync-dlt-inspection", "false", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        try (var dlt = new KafkaConsumer<String,String>(props,new StringDeserializer(),new StringDeserializer())) {
            broker.consumeFromAnEmbeddedTopic(dlt,"sync-test.DLT");
            kafka.send("sync-test","tournament:5",event(1)).get(10,TimeUnit.SECONDS);
            assertThat(applied.await(20,TimeUnit.SECONDS)).isTrue();
            assertThat(calls.get()).isEqualTo(2);
            kafka.send("sync-test","tournament:5","malformed event").get(10,TimeUnit.SECONDS);
            var quarantined = KafkaTestUtils.getSingleRecord(dlt,"sync-test.DLT",java.time.Duration.ofSeconds(20));
            assertThat(quarantined.value()).isEqualTo("malformed event");
            kafka.send("sync-test","tournament:5",event(3)).get(10,TimeUnit.SECONDS);
            var exhausted = KafkaTestUtils.getSingleRecord(dlt,"sync-test.DLT",java.time.Duration.ofSeconds(20));
            assertThat(exhausted.value()).contains("\"version\":3");
            kafka.send("sync-test","tournament:5",event(4)).get(10,TimeUnit.SECONDS);
            assertThat(afterPoison.await(20,TimeUnit.SECONDS)).isTrue();
            verify(sync,times(7)).synchronizeTournament(anyLong(),anyString(),anyString(),anyLong());
        }
    }
    private String event(long version) {
        return """
                {"eventId":"%s","eventType":"tournament.updated","aggregateType":"tournament","aggregateId":5,
                 "occurredAt":"2026-11-10T12:00:00Z","data":{"tournamentId":5,"name":"Latest","season":"2027","version":%d}}
                """.formatted(UUID.randomUUID(),version);
    }
    @Configuration
    @Import({JacksonConfig.class,KafkaProducerConfig.class,TournamentKafkaConsumerConfig.class,TournamentKafkaListener.class})
    static class Config {
        @Bean TournamentSyncService sync() { return mock(TournamentSyncService.class); }
        @Bean TournamentClient tournamentClient() { return mock(TournamentClient.class); }
    }
}
