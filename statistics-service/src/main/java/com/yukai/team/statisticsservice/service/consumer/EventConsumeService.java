package com.yukai.team.statisticsservice.service.consumer;

import com.yukai.team.statisticsservice.entity.EventConsumeRecord;
import com.yukai.team.statisticsservice.repository.EventConsumeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumeService {

    public static final String CONSUMER_NAME = "STATISTICS_SERVICE";

    private final EventConsumeRecordRepository eventConsumeRecordRepository;

    @Transactional(readOnly = true)
    public boolean alreadyConsumed(UUID eventId, String consumerName) {
        return eventConsumeRecordRepository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    @Transactional
    public void markConsumed(UUID eventId, String consumerName) {
        if (alreadyConsumed(eventId, consumerName)) {
            log.info("Event consume record already exists, eventId={}, consumerName={}", eventId, consumerName);
            return;
        }

        try {
            eventConsumeRecordRepository.save(EventConsumeRecord.builder()
                    .eventId(eventId)
                    .consumerName(consumerName)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            log.info("Event consume record already exists, eventId={}, consumerName={}", eventId, consumerName);
        }
    }
}
