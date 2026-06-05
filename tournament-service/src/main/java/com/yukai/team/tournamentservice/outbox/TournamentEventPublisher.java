package com.yukai.team.tournamentservice.outbox;

import com.yukai.team.tournamentservice.entity.OutboxEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TournamentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String tournamentEventsTopic;

    public TournamentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topics.tournament-events}") String tournamentEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.tournamentEventsTopic = tournamentEventsTopic;
    }

    public String publish(OutboxEvent event) throws Exception {
        String key = event.getAggregateType() + ":" + event.getAggregateId();
        kafkaTemplate.send(tournamentEventsTopic, key, event.getPayloadJson().toString()).get();
        return key;
    }

    public String getTournamentEventsTopic() {
        return tournamentEventsTopic;
    }
}
