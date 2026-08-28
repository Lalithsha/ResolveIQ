package com.resolveiq.ticket.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.EventEnvelope;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.contracts.tracing.CorrelationContext;
import com.resolveiq.ticket.application.dto.*;
import com.resolveiq.ticket.domain.model.*;
import com.resolveiq.ticket.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final TicketStatusHistoryRepository historyRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final SuggestionFeedbackRepository feedbackRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final AtomicLong TICKET_SEQUENCE = new AtomicLong(1000);

    public TicketService(
        TicketRepository ticketRepository,
        TicketMessageRepository messageRepository,
        TicketStatusHistoryRepository historyRepository,
        AiSuggestionRepository suggestionRepository,
        SuggestionFeedbackRepository feedbackRepository,
        OutboxEventRepository outboxRepository,
        ObjectMapper objectMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.historyRepository = historyRepository;
        this.suggestionRepository = suggestionRepository;
        this.feedbackRepository = feedbackRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TicketResponse createTicket(UUID tenantId, UUID customerId, CreateTicketRequest request) {
        String ticketNumber = "RIQ-2026-" + String.format("%06d", TICKET_SEQUENCE.incrementAndGet());

        Ticket ticket = new Ticket(
            UUID.randomUUID(),
            ticketNumber,
            tenantId,
            customerId,
            request.subject(),
            request.description(),
            request.category(),
            request.priority(),
            request.channel(),
            request.language()
        );
        ticketRepository.save(ticket);

        TicketStatusHistory history = new TicketStatusHistory(
            ticket.getId(),
            "NONE",
            TicketStatus.NEW.name(),
            customerId,
            "Ticket created by customer"
        );
        historyRepository.save(history);

        // Transactional Outbox Event
        TicketEvents.TicketCreatedPayload payload = new TicketEvents.TicketCreatedPayload(
            ticket.getId(),
            ticket.getTicketNumber(),
            ticket.getTenantId(),
            ticket.getCustomerId(),
            ticket.getSubject(),
            ticket.getDescription(),
            ticket.getChannel(),
            ticket.getPriority().name(),
            ticket.getCategory(),
            ticket.getCreatedAt()
        );

        EventEnvelope<TicketEvents.TicketCreatedPayload> envelope = EventEnvelope.create(
            TicketEvents.TICKET_CREATED,
            1,
            "ticket-service",
            tenantId,
            "ticket",
            ticket.getId(),
            CorrelationContext.getCorrelationId(),
            null,
            null,
            payload
        );

        saveOutboxEvent("ticket", ticket.getId(), TicketEvents.TICKET_CREATED, envelope);

        return TicketResponse.fromEntity(ticket);
    }

    @Transactional
    public TicketMessageResponse addMessage(
        UUID tenantId,
        UUID ticketId,
        UUID senderId,
        String senderRole,
        AddMessageRequest request
    ) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found with id: " + ticketId));

        TicketMessage message = new TicketMessage(
            ticket.getId(),
            tenantId,
            senderId,
            senderRole,
            request.content(),
            request.isInternal()
        );
        messageRepository.save(message);

        // If customer replied while waiting, move back to IN_PROGRESS
        if ("CUSTOMER".equalsIgnoreCase(senderRole) && ticket.getStatus() == TicketStatus.WAITING_ON_CUSTOMER) {
            ticket.transitionTo(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        TicketEvents.TicketMessageAddedPayload payload = new TicketEvents.TicketMessageAddedPayload(
            ticket.getId(),
            message.getId(),
            senderId,
            senderRole,
            message.getContent(),
            message.getCreatedAt()
        );

        EventEnvelope<TicketEvents.TicketMessageAddedPayload> envelope = EventEnvelope.create(
            TicketEvents.TICKET_MESSAGE_ADDED,
            1,
            "ticket-service",
            tenantId,
            "ticket",
            ticket.getId(),
            CorrelationContext.getCorrelationId(),
            null,
            null,
            payload
        );

        saveOutboxEvent("ticket", ticket.getId(), TicketEvents.TICKET_MESSAGE_ADDED, envelope);

        return TicketMessageResponse.fromEntity(message);
    }

    @Transactional
    public TicketResponse assignTicket(UUID tenantId, UUID ticketId, AssignTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found with id: " + ticketId));

        ticket.assign(request.teamId(), request.agentId());
        ticketRepository.save(ticket);

        TicketEvents.TicketAssignedPayload payload = new TicketEvents.TicketAssignedPayload(
            ticket.getId(),
            request.teamId(),
            request.agentId(),
            request.reason(),
            Instant.now()
        );

        EventEnvelope<TicketEvents.TicketAssignedPayload> envelope = EventEnvelope.create(
            TicketEvents.TICKET_ASSIGNED,
            1,
            "ticket-service",
            tenantId,
            "ticket",
            ticket.getId(),
            CorrelationContext.getCorrelationId(),
            null,
            null,
            payload
        );

        saveOutboxEvent("ticket", ticket.getId(), TicketEvents.TICKET_ASSIGNED, envelope);

        return TicketResponse.fromEntity(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(UUID tenantId, UUID ticketId, UUID userId, UpdateStatusRequest request) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found with id: " + ticketId));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.transitionTo(request.status());
        ticketRepository.save(ticket);

        TicketStatusHistory history = new TicketStatusHistory(
            ticket.getId(),
            oldStatus.name(),
            request.status().name(),
            userId,
            request.reason()
        );
        historyRepository.save(history);

        if (request.status() == TicketStatus.RESOLVED) {
            TicketEvents.TicketResolvedPayload payload = new TicketEvents.TicketResolvedPayload(
                ticket.getId(),
                request.reason(),
                userId,
                Instant.now()
            );

            EventEnvelope<TicketEvents.TicketResolvedPayload> envelope = EventEnvelope.create(
                TicketEvents.TICKET_RESOLVED,
                1,
                "ticket-service",
                tenantId,
                "ticket",
                ticket.getId(),
                CorrelationContext.getCorrelationId(),
                null,
                null,
                payload
            );

            saveOutboxEvent("ticket", ticket.getId(), TicketEvents.TICKET_RESOLVED, envelope);
        }

        return TicketResponse.fromEntity(ticket);
    }

    @Transactional
    public void recordFeedback(UUID tenantId, UUID ticketId, UUID agentId, SuggestionFeedbackRequest request) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found with id: " + ticketId));

        if (request.suggestionId() != null) {
            suggestionRepository.findByIdAndTenantId(request.suggestionId(), tenantId).ifPresent(sug -> {
                SuggestionStatus newStatus = switch (request.action().toUpperCase()) {
                    case "ACCEPTED" -> SuggestionStatus.ACCEPTED;
                    case "EDITED" -> SuggestionStatus.EDITED;
                    case "REJECTED" -> SuggestionStatus.REJECTED;
                    default -> SuggestionStatus.INVALIDATED;
                };
                sug.review(newStatus, agentId);
                suggestionRepository.save(sug);
            });
        }

        SuggestionFeedback feedback = new SuggestionFeedback(
            request.suggestionId() != null ? request.suggestionId() : UUID.randomUUID(),
            ticket.getId(),
            agentId,
            request.action(),
            request.rejectionReason(),
            request.editedContent(),
            request.rating()
        );
        feedbackRepository.save(feedback);

        TicketEvents.AgentFeedbackRecordedPayload payload = new TicketEvents.AgentFeedbackRecordedPayload(
            feedback.getSuggestionId(),
            ticket.getId(),
            agentId,
            request.action(),
            request.rejectionReason(),
            request.rating(),
            Instant.now()
        );

        EventEnvelope<TicketEvents.AgentFeedbackRecordedPayload> envelope = EventEnvelope.create(
            TicketEvents.AGENT_FEEDBACK_RECORDED,
            1,
            "ticket-service",
            tenantId,
            "suggestion",
            feedback.getSuggestionId(),
            CorrelationContext.getCorrelationId(),
            null,
            null,
            payload
        );

        saveOutboxEvent("suggestion", feedback.getSuggestionId(), TicketEvents.AGENT_FEEDBACK_RECORDED, envelope);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID tenantId, UUID ticketId) {
        return ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .map(TicketResponse::fromEntity)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found with id: " + ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listCustomerTickets(UUID tenantId, UUID customerId) {
        return ticketRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId)
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTeamTickets(UUID tenantId, UUID teamId) {
        return ticketRepository.findByTenantIdAndTeamIdOrderByCreatedAtDesc(tenantId, teamId)
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listAllTickets(UUID tenantId) {
        return ticketRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getTicketMessages(UUID tenantId, UUID ticketId) {
        return messageRepository.findByTicketIdAndTenantIdOrderByCreatedAtAsc(ticketId, tenantId)
            .stream().map(TicketMessageResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<AiSuggestionResponse> getTicketSuggestions(UUID tenantId, UUID ticketId) {
        return suggestionRepository.findByTicketIdAndTenantIdOrderByCreatedAtDesc(ticketId, tenantId)
            .stream().map(AiSuggestionResponse::fromEntity).toList();
    }

    private void saveOutboxEvent(String aggregateType, UUID aggregateId, String eventType, Object envelope) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            OutboxEvent outboxEvent = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
