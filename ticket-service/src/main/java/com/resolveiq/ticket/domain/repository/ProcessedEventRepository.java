package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
