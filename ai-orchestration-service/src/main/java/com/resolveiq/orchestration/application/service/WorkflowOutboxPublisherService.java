package com.resolveiq.orchestration.application.service;

import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.orchestration.domain.repository.WorkflowOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowOutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOutboxPublisherService.class);

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
    @Transactional
    public void publishPendingEvents() {
        List<WorkflowOutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
            "PENDING",
            PageRequest.of(0, 50)
        );

        for (WorkflowOutboxEvent event : pendingEvents) {
            String topic = resolveTopicForEventType(event.getEventType());
            String key = event.getAggregateId().toString();

            try {
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(topic, key, event.getPayload()).get();
                }
                event.markPublished();
                outboxRepository.save(event);
                log.debug("Successfully published workflow event [{}] ID [{}] to topic [{}]", event.getEventType(), event.getId(), topic);
            } catch (Exception e) {
                log.warn("Failed to publish workflow outbox event [{}] ID [{}]: {}", event.getEventType(), event.getId(), e.getMessage());
                event.markFailed();
                outboxRepository.save(event);
            }
        }
    }

    private String resolveTopicForEventType(String eventType) {
        return switch (eventType) {
            case "TicketTriageCompleted.v1" -> "resolveiq.ticket.triage_completed";
            case "TicketTriageFailed.v1" -> "resolveiq.ticket.triage_failed";
            default -> "resolveiq.workflow.events";
        };
    }
}
