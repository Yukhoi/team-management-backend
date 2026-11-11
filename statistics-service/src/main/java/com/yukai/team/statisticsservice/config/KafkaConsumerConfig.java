package com.yukai.team.statisticsservice.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:statistics-service}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset:earliest}") String autoOffsetReset
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup,
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler statisticsErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setAutoStartup(autoStartup);
        factory.setCommonErrorHandler(statisticsErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
    @Bean
    public ProducerFactory<String, String> statisticsDltProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"));
    }

    @Bean
    public KafkaTemplate<String, String> statisticsDltKafkaTemplate(
            ProducerFactory<String, String> statisticsDltProducerFactory) {
        return new KafkaTemplate<>(statisticsDltProducerFactory);
    }

    @Bean
    public DefaultErrorHandler statisticsErrorHandler(
            KafkaTemplate<String, String> statisticsDltKafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(statisticsDltKafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", -1));
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(35));
        recoverer.setLogRecoveryRecord(true);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 3));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setCommitRecovered(true);
        return handler;
    }
}
