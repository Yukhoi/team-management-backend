package com.yukai.team.matchservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaProducerConfigTest {

    @Test
    void should_configure_reliable_producer_defaults() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ProducerFactory<String, String> producerFactory = config.producerFactory("localhost:9092");
        Map<String, Object> properties = ((DefaultKafkaProducerFactory<String, String>) producerFactory)
                .getConfigurationProperties();

        assertEquals("all", properties.get(ProducerConfig.ACKS_CONFIG));
        assertEquals(3, properties.get(ProducerConfig.RETRIES_CONFIG));
        assertEquals(true, properties.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
        assertEquals(5, properties.get(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));
        assertEquals(10000, properties.get(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG));
        assertEquals(30000, properties.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG));
        assertEquals(10, properties.get(ProducerConfig.LINGER_MS_CONFIG));
        assertEquals(16384, properties.get(ProducerConfig.BATCH_SIZE_CONFIG));
    }
}
