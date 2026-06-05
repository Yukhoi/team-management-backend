package com.yukai.team.statisticsservice.repository;

import com.yukai.team.statisticsservice.entity.EventConsumeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventConsumeRecordRepository extends JpaRepository<EventConsumeRecord, Long> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);
}
