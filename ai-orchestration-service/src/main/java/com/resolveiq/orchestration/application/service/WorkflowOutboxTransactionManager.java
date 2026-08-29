package com.resolveiq.orchestration.application.service;

import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.orchestration.domain.repository.WorkflowOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowOutboxTransactionManager {

    private final WorkflowOutboxRepository repository;

    public WorkflowOutboxTransactionManager(WorkflowOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<WorkflowOutboxEvent> claimDue(String workerId, int batchSize, Duration leaseDuration) {
        List<WorkflowOutboxEvent> events = repository.claimDueEvents(Instant.now().minus(leaseDuration), batchSize);
        events.forEach(event -> event.markClaimed(workerId));
        return repository.saveAll(events);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordResult(UUID eventId, boolean success, String safeErrorCode) {
        WorkflowOutboxEvent event = repository.findById(eventId)
            .orElseThrow(() -> new IllegalStateException("Claimed workflow outbox event disappeared: " + eventId));
        if (success) event.markPublished();
        else event.markFailed(safeErrorCode == null ? "DELIVERY_FAILED" : safeErrorCode);
        repository.save(event);
    }
}
