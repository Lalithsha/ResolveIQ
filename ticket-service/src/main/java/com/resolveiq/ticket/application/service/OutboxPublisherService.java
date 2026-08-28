package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.domain.model.OutboxEvent;
import com.resolveiq.ticket.domain.model.OutboxStatus;
import com.resolveiq.ticket.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@EnableScheduling
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherService(
        OutboxEventRepository outboxRepository,
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PENDING,
            PageRequest.of(0, 50)
        );

        for (OutboxEvent event : pendingEvents) {
            try {
                // Topic name format: resolveiq.<aggregateType>.<eventType>
                String topic = event.getEventType();
                String partitionKey = event.getAggregateId().toString();

                kafkaTemplate.send(topic, partitionKey, event.getPayload()).get();
                event.markPublished();
                outboxRepository.save(event);
                log.debug("Successfully published outbox event {} to topic {}", event.getId(), topic);
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed();
                outboxRepository.save(event);
            }
        }
    }
}
