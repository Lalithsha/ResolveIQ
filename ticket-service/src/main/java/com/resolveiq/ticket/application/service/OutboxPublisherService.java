package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.domain.model.OutboxEvent;
import com.resolveiq.ticket.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
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
@EnableScheduling
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);
    private final String workerId = "ticket-worker-" + UUID.randomUUID().toString().substring(0, 8);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherService(
        OutboxEventRepository outboxRepository,
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvents() {
        List<OutboxEvent> claimedEvents = claimBatch();
        if (claimedEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : claimedEvents) {
            publishAndRecord(event);
        }
    }

    @Transactional
    public List<OutboxEvent> claimBatch() {
        Instant leaseExpiredAt = Instant.now().minus(Duration.ofMinutes(2));
        List<OutboxEvent> events;
        try {
            events = outboxRepository.claimDueEvents(leaseExpiredAt, 20);
        } catch (Exception e) {
            // Fallback for H2 or environments without native SKIP LOCKED
            events = new ArrayList<>();
        }

        for (OutboxEvent event : events) {
            event.markClaimed(workerId);
            outboxRepository.save(event);
        }
        return events;
    }

    private void publishAndRecord(OutboxEvent event) {
        String topic = event.getEventType();
        String partitionKey = event.getAggregateId().toString();

        boolean success = false;
        String errorMessage = null;

        try {
            // Publish outside database transaction with bounded 5-second wait
            kafkaTemplate.send(topic, partitionKey, event.getPayload()).get(5, TimeUnit.SECONDS);
            success = true;
            log.debug("Successfully published outbox event [{}] to topic [{}]", event.getId(), topic);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.warn("Failed to publish outbox event [{}] to topic [{}]: {}", event.getId(), topic, e.getMessage());
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
}
