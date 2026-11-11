package com.yukai.team.matchservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import java.time.Duration;
import java.util.Map;

@Configuration
@EnableKafka
public class TournamentKafkaConsumerConfig {
    @Bean
    public DefaultErrorHandler tournamentSyncErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", -1));
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(35));
        recoverer.setLogRecoveryRecord(true);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setCommitRecovered(true);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> tournamentKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String servers,
            @Value("${app.tournament-sync.group-id:match-service-tournament-sync}") String group,
            @Value("${app.tournament-sync.auto-offset-reset:earliest}") String offsetReset,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup,
            DefaultErrorHandler tournamentSyncErrorHandler) {
        var consumer = new DefaultKafkaConsumerFactory<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers, ConsumerConfig.GROUP_ID_CONFIG, group,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset, ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumer);
        factory.setAutoStartup(autoStartup);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(tournamentSyncErrorHandler);
        return factory;
    }
}
