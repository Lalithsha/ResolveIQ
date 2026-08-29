package com.resolveiq.orchestration.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.orchestration.application.service.TriageWorkflowOrchestrator;
import com.resolveiq.orchestration.domain.model.ProcessedEvent;
import com.resolveiq.orchestration.domain.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TicketCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(TicketCreatedListener.class);

    private final TriageWorkflowOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEvents;
    private static final String CONSUMER_GROUP = "resolveiq-orchestration-service";

    public TicketCreatedListener(TriageWorkflowOrchestrator orchestrator, ObjectMapper objectMapper,
                                 ProcessedEventRepository processedEvents) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = TicketEvents.TICKET_CREATED, groupId = CONSUMER_GROUP)
    public void handleTicketCreated(String messagePayload) {
        try {
            JsonNode root = objectMapper.readTree(messagePayload);
            UUID eventId = UUID.fromString(root.get("eventId").asText());
            if (!TicketEvents.TICKET_CREATED.equals(root.path("eventType").asText()) || root.path("eventVersion").asInt(-1) != 1) {
                throw new IllegalArgumentException("Unsupported TicketCreated event contract");
            }
            if (processedEvents.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Ignoring duplicate TicketCreated event {}", eventId);
                return;
            }
            JsonNode payload = root.get("payload");

            UUID ticketId = UUID.fromString(payload.get("ticketId").asText());
            UUID tenantId = UUID.fromString(root.get("tenantId").asText());
            if (!ticketId.toString().equals(root.path("aggregateId").asText())
                || (payload.hasNonNull("tenantId") && !tenantId.toString().equals(payload.get("tenantId").asText()))) {
                throw new IllegalArgumentException("TicketCreated envelope and payload identity mismatch");
            }
            String subject = payload.get("subject").asText();
            String description = payload.get("description").asText();
            String priority = payload.has("priority") ? payload.get("priority").asText() : "MEDIUM";
            String channel = payload.has("channel") ? payload.get("channel").asText() : "WEB";

            log.info("Received TicketCreated event for ticket {}", ticketId);
            orchestrator.executeTriageWorkflow(ticketId, tenantId, subject, description, priority, channel);
            processedEvents.save(new ProcessedEvent(eventId, CONSUMER_GROUP));
        } catch (Exception e) {
            log.error("Failed to process TicketCreated event payload: {}", e.getMessage(), e);
            throw new IllegalStateException("TicketCreated event processing failed", e);
        }
    }
}
