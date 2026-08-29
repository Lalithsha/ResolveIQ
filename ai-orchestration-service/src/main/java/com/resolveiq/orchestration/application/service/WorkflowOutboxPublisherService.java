package com.resolveiq.orchestration.application.service;

import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.orchestration.domain.repository.WorkflowOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class WorkflowOutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOutboxPublisherService.class);
    private final String workerId = "orch-worker-" + UUID.randomUUID().toString().substring(0, 8);

    private final WorkflowOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WorkflowOutboxPublisherService(
        WorkflowOutboxRepository outboxRepository,
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${resolveiq.outbox.poll-interval-ms:1000}")
    public void processOutboxEvents() {
        List<WorkflowOutboxEvent> claimedEvents = claimBatch();
        if (claimedEvents.isEmpty()) {
            return;
        }

        for (WorkflowOutboxEvent event : claimedEvents) {
            publishAndRecord(event);
        }
    }

    @Transactional
    public List<WorkflowOutboxEvent> claimBatch() {
        Instant leaseExpiredAt = Instant.now().minus(Duration.ofMinutes(2));
        List<WorkflowOutboxEvent> events;
        try {
            events = outboxRepository.claimDueEvents(leaseExpiredAt, 20);
        } catch (Exception e) {
            events = new ArrayList<>();
        }

        for (WorkflowOutboxEvent event : events) {
            event.markClaimed(workerId);
            outboxRepository.save(event);
        }
        return events;
    }

    private void publishAndRecord(WorkflowOutboxEvent event) {
        String topic = resolveTopicForEventType(event.getEventType());
        String key = event.getAggregateId().toString();

        boolean success = false;
        String errorMessage = null;

        try {
            if (kafkaTemplate != null) {
                kafkaTemplate.send(topic, key, event.getPayload()).get(5, TimeUnit.SECONDS);
            }
            success = true;
            log.debug("Successfully published workflow event [{}] ID [{}] to topic [{}]", event.getEventType(), event.getId(), topic);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.warn("Failed to publish workflow outbox event [{}] ID [{}]: {}", event.getEventType(), event.getId(), e.getMessage());
        }

        recordResult(event.getId(), success, errorMessage);
    }

    @Transactional
    public void recordResult(UUID eventId, boolean success, String errorMessage) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            if (success) {
                event.markPublished();
            } else {
                event.markFailed(errorMessage != null ? errorMessage : "DELIVERY_TIMEOUT");
            }
            outboxRepository.save(event);
        });
    }

    private String resolveTopicForEventType(String eventType) {
        return switch (eventType) {
            case "TicketTriageCompleted.v1" -> "resolveiq.ticket.triage_completed";
            case "TicketTriageFailed.v1" -> "resolveiq.ticket.triage_failed";
            default -> "resolveiq.workflow.events";
        };
    }
}
