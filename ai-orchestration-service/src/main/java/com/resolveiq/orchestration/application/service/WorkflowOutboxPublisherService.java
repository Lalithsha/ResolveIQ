package com.resolveiq.orchestration.application.service;

import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class WorkflowOutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOutboxPublisherService.class);
    private final String workerId = "orch-worker-" + UUID.randomUUID().toString().substring(0, 8);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WorkflowOutboxTransactionManager transactionManager;

    public WorkflowOutboxPublisherService(
        KafkaTemplate<String, Object> kafkaTemplate,
        WorkflowOutboxTransactionManager transactionManager
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.transactionManager = transactionManager;
    }

    @Scheduled(fixedDelayString = "${resolveiq.outbox.poll-interval-ms:1000}")
    public void processOutboxEvents() {
        List<WorkflowOutboxEvent> claimedEvents = transactionManager.claimDue(workerId, 20, Duration.ofMinutes(2));
        if (claimedEvents.isEmpty()) {
            return;
        }

        for (WorkflowOutboxEvent event : claimedEvents) {
            publishAndRecord(event);
        }
    }

    private void publishAndRecord(WorkflowOutboxEvent event) {
        String topic = resolveTopicForEventType(event.getEventType());
        String key = event.getAggregateId().toString();

        boolean success = false;
        String errorCode = null;

        try {
            if (kafkaTemplate != null) {
                kafkaTemplate.send(topic, key, event.getPayload()).get(5, TimeUnit.SECONDS);
            }
            success = true;
        } catch (Exception e) {
            errorCode = e.getClass().getSimpleName();
            log.error("Failed to publish workflow outbox event id: {} to topic: {}", event.getId(), topic, e);
        }

        transactionManager.recordResult(event.getId(), success, errorCode);
    }

    private String resolveTopicForEventType(String eventType) {
        if (TicketEvents.TICKET_TRIAGE_COMPLETED.equals(eventType)) {
            return "resolveiq.tickets.triage-completed.v1";
        } else if (TicketEvents.TICKET_TRIAGE_FAILED.equals(eventType)) {
            return "resolveiq.tickets.triage-failed.v1";
        }
        return "resolveiq.orchestration.events.v1";
    }
}
