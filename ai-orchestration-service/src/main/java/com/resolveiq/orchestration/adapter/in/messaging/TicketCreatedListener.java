package com.resolveiq.orchestration.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.orchestration.application.service.TriageWorkflowOrchestrator;
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

    public TicketCreatedListener(TriageWorkflowOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TicketEvents.TICKET_CREATED, groupId = "resolveiq-orchestration-service")
    public void handleTicketCreated(String messagePayload) {
        try {
            JsonNode root = objectMapper.readTree(messagePayload);
            JsonNode payload = root.get("payload");

            UUID ticketId = UUID.fromString(payload.get("ticketId").asText());
            UUID tenantId = UUID.fromString(root.get("tenantId").asText());
            String subject = payload.get("subject").asText();
            String description = payload.get("description").asText();
            String priority = payload.has("priority") ? payload.get("priority").asText() : "MEDIUM";
            String channel = payload.has("channel") ? payload.get("channel").asText() : "WEB";

            log.info("Received TicketCreated event for ticket {}", ticketId);
            orchestrator.executeTriageWorkflow(ticketId, tenantId, subject, description, priority, channel);
        } catch (Exception e) {
            log.error("Failed to parse TicketCreated event payload: {}", e.getMessage(), e);
        }
    }
}
