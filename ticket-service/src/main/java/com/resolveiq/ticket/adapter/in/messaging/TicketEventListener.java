package com.resolveiq.ticket.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.ticket.domain.model.AiSuggestion;
import com.resolveiq.ticket.domain.model.ProcessedEvent;
import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.repository.AiSuggestionRepository;
import com.resolveiq.ticket.domain.repository.ProcessedEventRepository;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.TicketStatusHistoryRepository;
import com.resolveiq.ticket.domain.model.TicketStatusHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TicketEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventListener.class);
    private static final String CONSUMER_GROUP = "resolveiq-ticket-service";

    private final TicketRepository ticketRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final TicketStatusHistoryRepository historyRepository;

    public TicketEventListener(
        TicketRepository ticketRepository,
        AiSuggestionRepository suggestionRepository,
        ProcessedEventRepository processedEventRepository,
        ObjectMapper objectMapper,
        TicketStatusHistoryRepository historyRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.suggestionRepository = suggestionRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
        this.historyRepository = historyRepository;
    }

    @KafkaListener(topics = { TicketEvents.TICKET_TRIAGE_COMPLETED, TicketEvents.TICKET_TRIAGE_FAILED }, groupId = CONSUMER_GROUP)
    @Transactional
    public void handleTriageEvent(String messagePayload) {
        try {
            JsonNode root = objectMapper.readTree(messagePayload);
            UUID eventId = UUID.fromString(root.get("eventId").asText());
            String eventType = root.get("eventType").asText();
            if (!TicketEvents.TICKET_TRIAGE_COMPLETED.equals(eventType) && !TicketEvents.TICKET_TRIAGE_FAILED.equals(eventType)) {
                throw new IllegalArgumentException("Unsupported triage event type");
            }
            if (root.path("eventVersion").asInt(-1) != 1) throw new IllegalArgumentException("Unsupported triage event version");

            // Idempotency Check
            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Duplicate event {} ignored by consumer group {}", eventId, CONSUMER_GROUP);
                return;
            }

            JsonNode payload = root.get("payload");
            UUID ticketId = UUID.fromString(payload.get("ticketId").asText());
            UUID tenantId = UUID.fromString(root.get("tenantId").asText());
            if (!ticketId.toString().equals(root.path("aggregateId").asText())) {
                throw new IllegalArgumentException("Event aggregate does not match payload ticket");
            }

            Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found for triage event"));
            {
                if (TicketEvents.TICKET_TRIAGE_COMPLETED.equals(eventType)) {
                    String previousStatus = ticket.getStatus().name();
                    String category = payload.has("category") ? payload.get("category").asText() : null;
                    UUID teamId = payload.has("assignedTeamId") && !payload.get("assignedTeamId").isNull()
                        ? UUID.fromString(payload.get("assignedTeamId").asText())
                        : null;
                    UUID agentId = payload.has("assignedAgentId") && !payload.get("assignedAgentId").isNull()
                        ? UUID.fromString(payload.get("assignedAgentId").asText()) : null;
                    UUID slaPolicyId = payload.hasNonNull("slaPolicyId") ? UUID.fromString(payload.get("slaPolicyId").asText()) : null;
                    java.time.Instant firstDue = payload.hasNonNull("firstResponseDueAt") ? java.time.Instant.parse(payload.get("firstResponseDueAt").asText()) : null;
                    java.time.Instant resolutionDue = payload.hasNonNull("resolutionDueAt") ? java.time.Instant.parse(payload.get("resolutionDueAt").asText()) : null;

                    UUID suggestionId = null;
                    if (payload.has("suggestionId") && !payload.get("suggestionId").isNull()) {
                        suggestionId = UUID.fromString(payload.get("suggestionId").asText());
                    }

                    if (suggestionId == null || !payload.hasNonNull("suggestedResponse")) {
                        throw new IllegalArgumentException("Completed triage event is missing suggestion content");
                    }
                    if (!suggestionRepository.existsById(suggestionId)) {
                        suggestionRepository.save(new AiSuggestion(
                            suggestionId,
                            ticketId,
                            tenantId,
                            payload.get("suggestedResponse").asText(),
                            payload.hasNonNull("confidence") ? payload.get("confidence").asDouble() : null,
                            payload.path("modelName").asText("unknown"),
                            payload.path("promptVersion").asText("unknown"),
                            payload.path("citationsJson").asText("[]")
                        ));
                    }

                    ticket.updateTriageResult("SUCCESS", category, suggestionId, teamId, agentId);
                    ticket.setSlaDeadlines(slaPolicyId, firstDue, resolutionDue);
                    ticketRepository.save(ticket);
                    if (!previousStatus.equals(ticket.getStatus().name())) {
                        historyRepository.save(new TicketStatusHistory(ticketId, previousStatus, ticket.getStatus().name(),
                            new UUID(0, 0), "AI triage completed"));
                    }
                } else if (TicketEvents.TICKET_TRIAGE_FAILED.equals(eventType)) {
                    String previousStatus = ticket.getStatus().name();
                    ticket.updateTriageResult("FAILED", null, null, null, null);
                    ticketRepository.save(ticket);
                    if (!previousStatus.equals(ticket.getStatus().name())) {
                        historyRepository.save(new TicketStatusHistory(ticketId, previousStatus, ticket.getStatus().name(),
                            new UUID(0, 0), "AI triage failed"));
                    }
                }
            }

            // Mark event processed in same transaction
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP));
            log.info("Processed triage event {} for ticket {}", eventId, ticketId);
        } catch (Exception e) {
            log.error("Failed to process triage event: {}", e.getMessage(), e);
            throw new RuntimeException("Error handling triage event", e);
        }
    }
}
