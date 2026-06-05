package com.yukai.team.statisticsservice.service.consumer;

import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import com.yukai.team.statisticsservice.kafka.dispatcher.StatisticsEventDispatcher;
import com.yukai.team.statisticsservice.service.cache.StatisticsCacheInvalidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsEventProcessingServiceTest {

    @Mock
    private EventConsumeService eventConsumeService;

    @Mock
    private StatisticsEventDispatcher statisticsEventDispatcher;

    @Mock
    private StatisticsCacheInvalidationService statisticsCacheInvalidationService;

    @InjectMocks
    private StatisticsEventProcessingService statisticsEventProcessingService;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsCachesOnlyAfterTransactionCommit() {
        UUID eventId = UUID.randomUUID();
        StatisticsEventEnvelope event = StatisticsEventEnvelope.builder()
                .eventId(eventId)
                .eventType("match.goal.created")
                .build();
        when(eventConsumeService.alreadyConsumed(eventId, EventConsumeService.CONSUMER_NAME)).thenReturn(false);
        TransactionSynchronizationManager.initSynchronization();

        assertThat(statisticsEventProcessingService.process(event)).isTrue();

        verify(statisticsCacheInvalidationService, never()).evictStatisticsReadCaches();
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(statisticsCacheInvalidationService).evictStatisticsReadCaches();
        verify(statisticsEventDispatcher).dispatch(event);
        verify(eventConsumeService).markConsumed(eventId, EventConsumeService.CONSUMER_NAME);
    }
}
