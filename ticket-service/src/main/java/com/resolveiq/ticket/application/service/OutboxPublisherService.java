package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.domain.model.OutboxEvent;
import com.resolveiq.ticket.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final OutboxTransactionManager transactionManager;

    public OutboxPublisherService(
        OutboxEventRepository outboxRepository,
        KafkaTemplate<String, String> kafkaTemplate,
        OutboxTransactionManager transactionManager
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionManager = transactionManager;
    }

    @Scheduled(fixedDelay = 1000)
    public void processOutboxEvents() {
        List<OutboxEvent> claimedEvents = transactionManager.claimDue(workerId, 20, Duration.ofMinutes(2));
        if (claimedEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : claimedEvents) {
            publishAndRecord(event);
        }
    }

    private void publishAndRecord(OutboxEvent event) {
        String topic = event.getEventType();
        String partitionKey = event.getAggregateId().toString();

        boolean success = false;
        String errorCode = null;

        try {
            // Publish outside database transaction with bounded 5-second wait
            kafkaTemplate.send(topic, partitionKey, event.getPayload()).get(5, TimeUnit.SECONDS);
            success = true;
            log.debug("Successfully published outbox event [{}] to topic [{}]", event.getId(), topic);
        } catch (Exception e) {
            errorCode = classifyError(e);
            log.warn("Failed to publish outbox event [{}] to topic [{}]: {}", event.getId(), topic, e.getMessage());
        }

        transactionManager.recordResult(event.getId(), success, errorCode);
    }

    private String classifyError(Exception error) {
        if (error instanceof java.util.concurrent.TimeoutException) return "KAFKA_TIMEOUT";
        if (error instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return "PUBLISH_INTERRUPTED";
        }
        return "KAFKA_PUBLISH_FAILED";
    }
}
