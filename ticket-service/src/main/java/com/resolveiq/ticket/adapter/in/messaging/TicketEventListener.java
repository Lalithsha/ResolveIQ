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

    public TicketEventListener(
        TicketRepository ticketRepository,
        AiSuggestionRepository suggestionRepository,
        ProcessedEventRepository processedEventRepository,
        ObjectMapper objectMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.suggestionRepository = suggestionRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = { TicketEvents.TICKET_TRIAGE_COMPLETED, TicketEvents.TICKET_TRIAGE_FAILED }, groupId = CONSUMER_GROUP)
    @Transactional
    public void handleTriageEvent(String messagePayload) {
        try {
            JsonNode root = objectMapper.readTree(messagePayload);
            UUID eventId = UUID.fromString(root.get("eventId").asText());
            String eventType = root.get("eventType").asText();

            // Idempotency Check
            if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, CONSUMER_GROUP)) {
                log.info("Duplicate event {} ignored by consumer group {}", eventId, CONSUMER_GROUP);
                return;
            }

            JsonNode payload = root.get("payload");
            UUID ticketId = UUID.fromString(payload.get("ticketId").asText());
            UUID tenantId = UUID.fromString(root.get("tenantId").asText());

            ticketRepository.findById(ticketId).ifPresent(ticket -> {
                if (TicketEvents.TICKET_TRIAGE_COMPLETED.equals(eventType)) {
                    String category = payload.has("category") ? payload.get("category").asText() : null;
                    UUID teamId = payload.has("assignedTeamId") && !payload.get("assignedTeamId").isNull()
                        ? UUID.fromString(payload.get("assignedTeamId").asText())
                        : null;

                    UUID suggestionId = null;
                    if (payload.has("suggestionId") && !payload.get("suggestionId").isNull()) {
                        suggestionId = UUID.fromString(payload.get("suggestionId").asText());
                    }

                    ticket.updateTriageResult("SUCCESS", category, suggestionId, teamId);
                    ticketRepository.save(ticket);
                } else if (TicketEvents.TICKET_TRIAGE_FAILED.equals(eventType)) {
                    ticket.updateTriageResult("FAILED", null, null, null);
                    ticketRepository.save(ticket);
                }
            });

            // Mark event processed in same transaction
            processedEventRepository.save(new ProcessedEvent(eventId, CONSUMER_GROUP));
            log.info("Processed triage event {} for ticket {}", eventId, ticketId);
        } catch (Exception e) {
            log.error("Failed to process triage event: {}", e.getMessage(), e);
            throw new RuntimeException("Error handling triage event", e);
        }
    }
}
