package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.domain.model.OutboxEvent;
import com.resolveiq.ticket.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxTransactionManager {

    private final OutboxEventRepository repository;

    public OutboxTransactionManager(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimDue(String workerId, int batchSize, Duration leaseDuration) {
        List<OutboxEvent> events = repository.claimDueEvents(Instant.now().minus(leaseDuration), batchSize);
        events.forEach(event -> event.markClaimed(workerId));
        return repository.saveAll(events);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordResult(UUID eventId, boolean success, String safeErrorCode) {
        OutboxEvent event = repository.findById(eventId)
            .orElseThrow(() -> new IllegalStateException("Claimed outbox event disappeared: " + eventId));
        if (success) event.markPublished();
        else event.markFailed(safeErrorCode == null ? "DELIVERY_FAILED" : safeErrorCode);
        repository.save(event);
    }
}
