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
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final TicketStatusHistoryRepository historyRepository;
    private final AiSuggestionRepository suggestionRepository;
    private final SuggestionFeedbackRepository feedbackRepository;
    private final OutboxEventRepository outboxRepository;
    private final IdempotencyKeyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final StaffTeamMembershipRepository membershipRepository;

    private static final String CREATE_TICKET_OPERATION = "CREATE_TICKET";

    public TicketService(
        TicketRepository ticketRepository,
        TicketMessageRepository messageRepository,
        TicketStatusHistoryRepository historyRepository,
        AiSuggestionRepository suggestionRepository,
        SuggestionFeedbackRepository feedbackRepository,
        OutboxEventRepository outboxRepository,
        IdempotencyKeyRepository idempotencyRepository,
        ObjectMapper objectMapper,
        ApplicationEventPublisher eventPublisher,
        StaffTeamMembershipRepository membershipRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.historyRepository = historyRepository;
        this.suggestionRepository = suggestionRepository;
        this.feedbackRepository = feedbackRepository;
        this.outboxRepository = outboxRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.membershipRepository = membershipRepository;
    }

    private String computeRequestHash(CreateTicketRequest request) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String raw = String.format("%s|%s|%s|%s|%s|%s",
                request.subject() != null ? request.subject().trim() : "",
                request.description() != null ? request.description().trim() : "",
                request.category() != null ? request.category().trim() : "",
                request.priority() != null ? request.priority().name() : "",
                request.channel() != null ? request.channel().trim() : "",
                request.language() != null ? request.language().trim() : ""
            );
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return String.valueOf(request.hashCode());
        }
    }

    private String generateTicketNumber() {
        int currentYear = java.time.Year.now(java.time.ZoneId.of("UTC")).getValue();
        Long seq = ticketRepository.getNextTicketSequenceVal();
        if (seq == null || seq < 1) {
            throw new IllegalStateException("Ticket number sequence is unavailable");
        }
        return String.format("RIQ-%d-%06d", currentYear, seq);
    }

    @Transactional
    public TicketResponse createTicket(UUID tenantId, UUID customerId, CreateTicketRequest request, String idempotencyKey) {
        String reqHash = computeRequestHash(request);

        String normalizedKey = (idempotencyKey != null && !idempotencyKey.isBlank())
            ? idempotencyKey.trim()
            : UUID.randomUUID().toString();
        if (normalizedKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 255 characters");
        }

        UUID commandId = UUID.randomUUID();
        Instant now = Instant.now();
        int claimed = idempotencyRepository.claim(
            commandId, tenantId, customerId, CREATE_TICKET_OPERATION, normalizedKey,
            reqHash, now, now.plusSeconds(86400)
        );
        IdempotencyKey command = idempotencyRepository
            .findByTenantIdAndActorIdAndOperationAndKey(tenantId, customerId, CREATE_TICKET_OPERATION, normalizedKey)
            .orElseThrow(() -> new IllegalStateException("Unable to claim idempotent command"));
        if (claimed == 0) {
            if (!command.getRequestHash().equals(reqHash)) {
                throw new com.resolveiq.ticket.domain.exception.IdempotencyConflictException("Idempotency key reused with differing request payload");
            }
            if (command.isCompleted() && command.getResponseBody() != null) {
                try {
                    return objectMapper.readValue(command.getResponseBody(), TicketResponse.class);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Stored idempotency response is invalid", e);
                }
            }
            throw new com.resolveiq.ticket.domain.exception.IdempotencyConflictException("A request with this idempotency key is already in progress");
        }

        String ticketNumber = generateTicketNumber();

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
        publish("ticket.created", ticket);

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

        TicketResponse response = TicketResponse.fromEntity(ticket);

        try {
            command.complete(201, objectMapper.writeValueAsString(response));
            idempotencyRepository.save(command);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize idempotent response", e);
        }

        return response;
    }

    @Transactional
    public TicketResponse createTicket(UUID tenantId, UUID customerId, CreateTicketRequest request) {
        return createTicket(tenantId, customerId, request, UUID.randomUUID().toString());
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

        // If ticket was WAITING_ON_CUSTOMER and customer replies, transition back to IN_PROGRESS
        if ("CUSTOMER".equalsIgnoreCase(senderRole) && ticket.getStatus() == TicketStatus.WAITING_ON_CUSTOMER) {
            ticket.transitionTo(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
        if ("CUSTOMER".equalsIgnoreCase(senderRole)) {
            suggestionRepository.findByTicketIdAndTenantIdOrderByCreatedAtDesc(ticketId, tenantId).forEach(suggestion -> {
                suggestion.invalidate();
                suggestionRepository.save(suggestion);
            });
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
        publish("ticket.message.added", ticket);

        return TicketMessageResponse.fromEntity(message);
    }

    @Transactional
    public TicketResponse assignTicket(UUID tenantId, UUID ticketId, AssignTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found with id: " + ticketId));

        if (request.agentId() != null && request.teamId() == null) {
            throw new IllegalArgumentException("An agent assignment requires a team");
        }
        if (request.agentId() != null && !membershipRepository.existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(
            tenantId, request.agentId(), request.teamId())) {
            throw new IllegalArgumentException("The selected agent is not an active member of the selected team");
        }

        ticket.assign(request.teamId(), request.agentId());
        ticketRepository.save(ticket);
        publish("ticket.assigned", ticket);

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
        publish("ticket.updated", ticket);

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

        AiSuggestion suggestion = suggestionRepository.findByIdAndTenantId(request.suggestionId(), tenantId)
            .filter(value -> value.getTicketId().equals(ticketId))
            .orElseThrow(() -> new IllegalArgumentException("Suggestion not found for ticket"));
        SuggestionStatus newStatus = switch (request.action().toUpperCase()) {
            case "ACCEPTED" -> SuggestionStatus.ACCEPTED;
            case "EDITED" -> SuggestionStatus.EDITED;
            case "REJECTED" -> SuggestionStatus.REJECTED;
            default -> throw new IllegalArgumentException("Unsupported feedback action");
        };
        if (newStatus == SuggestionStatus.EDITED && (request.editedContent() == null || request.editedContent().isBlank())) {
            throw new IllegalArgumentException("Edited content is required for EDITED feedback");
        }
        if (newStatus == SuggestionStatus.REJECTED && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new IllegalArgumentException("Rejection reason is required for REJECTED feedback");
        }
        suggestion.review(newStatus, agentId);
        suggestionRepository.save(suggestion);

        SuggestionFeedback feedback = new SuggestionFeedback(
            request.suggestionId(),
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
    public TicketResponse getCustomerTicketById(UUID tenantId, UUID customerId, UUID ticketId) {
        return ticketRepository.findByIdAndTenantIdAndCustomerId(ticketId, tenantId, customerId)
            .map(TicketResponse::fromEntity)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found or access denied for id: " + ticketId));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listCustomerTickets(UUID tenantId, UUID customerId) {
        return listCustomerTickets(tenantId, customerId, 0, 50);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listCustomerTickets(UUID tenantId, UUID customerId, int page, int size) {
        return ticketRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId, page(page, size))
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTeamTickets(UUID tenantId, UUID teamId) {
        return ticketRepository.findByTenantIdAndTeamIdOrderByCreatedAtDesc(tenantId, teamId, page(0, 100))
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listAllTickets(UUID tenantId) {
        return ticketRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, page(0, 100))
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listAssignedTickets(UUID tenantId, UUID agentId) {
        return ticketRepository.findByTenantIdAndAssignedAgentIdOrderByCreatedAtDesc(tenantId, agentId, page(0, 100))
            .stream().map(TicketResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getTicketMessages(UUID tenantId, UUID ticketId) {
        return messageRepository.findByTicketIdAndTenantIdOrderByCreatedAtAsc(ticketId, tenantId)
            .stream().map(TicketMessageResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketMessageResponse> getCustomerTicketMessages(UUID tenantId, UUID customerId, UUID ticketId) {
        // Enforce customer ownership
        ticketRepository.findByIdAndTenantIdAndCustomerId(ticketId, tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found or access denied for id: " + ticketId));

        return messageRepository.findByTicketIdAndTenantIdAndIsInternalFalseOrderByCreatedAtAsc(ticketId, tenantId)
            .stream().map(TicketMessageResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<AiSuggestionResponse> getTicketSuggestions(UUID tenantId, UUID ticketId) {
        return suggestionRepository.findByTicketIdAndTenantIdOrderByCreatedAtDesc(ticketId, tenantId)
            .stream().map(AiSuggestionResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public void assertStaffCanRead(TicketResponse ticket, UUID actorId, java.util.Set<String> roles) {
        if (roles.contains("ADMIN") || roles.contains("AUDITOR")) return;
        if ((roles.contains("AGENT") || roles.contains("TEAM_LEAD")) && (actorId.equals(ticket.assignedAgentId()) ||
            (ticket.teamId() != null && membershipRepository.existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(
                ticket.tenantId(), actorId, ticket.teamId())))) return;
        throw new SecurityException("Ticket is outside the authorized queue");
    }

    @Transactional(readOnly = true)
    public void assertTeamAccess(UUID tenantId, UUID actorId, UUID teamId) {
        if (teamId == null || !membershipRepository.existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(tenantId, actorId, teamId)) {
            throw new SecurityException("Team is outside the authorized scope");
        }
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

    private org.springframework.data.domain.Pageable page(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be non-negative");
        return org.springframework.data.domain.PageRequest.of(page, Math.max(1, Math.min(size, 100)));
    }

    private void publish(String eventType, Ticket ticket) {
        eventPublisher.publishEvent(new TicketChangedEvent(eventType, TicketResponse.fromEntity(ticket)));
    }
}
