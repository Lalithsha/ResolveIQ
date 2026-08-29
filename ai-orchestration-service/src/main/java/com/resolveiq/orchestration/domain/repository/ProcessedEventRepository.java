package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
