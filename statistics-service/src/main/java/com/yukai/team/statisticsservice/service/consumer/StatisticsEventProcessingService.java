package com.yukai.team.statisticsservice.service.consumer;

import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.kafka.dispatcher.StatisticsEventDispatcher;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsEventProcessingService {

    private final EventConsumeService eventConsumeService;
    private final StatisticsEventDispatcher statisticsEventDispatcher;
    private final StatisticsCacheInvalidationService statisticsCacheInvalidationService;

    @Transactional
    public boolean process(StatisticsEventEnvelope event) {
        if (eventConsumeService.alreadyConsumed(event.getEventId(), EventConsumeService.CONSUMER_NAME)) {
            log.info("Duplicate event skipped, eventId={}, eventType={}", event.getEventId(), event.getEventType());
            return false;
        }

        statisticsEventDispatcher.dispatch(event);
        eventConsumeService.markConsumed(event.getEventId(), EventConsumeService.CONSUMER_NAME);
        evictCachesAfterCommit();
        return true;
    }

    private void evictCachesAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                statisticsCacheInvalidationService.evictStatisticsReadCaches();
            }
        });
    }
}
