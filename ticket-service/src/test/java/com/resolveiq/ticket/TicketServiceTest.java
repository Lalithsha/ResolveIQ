package com.resolveiq.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.ticket.application.dto.AddMessageRequest;
import com.resolveiq.ticket.application.dto.CreateTicketRequest;
import com.resolveiq.ticket.application.dto.TicketResponse;
import com.resolveiq.ticket.application.dto.UpdateStatusRequest;
import com.resolveiq.ticket.application.service.TicketService;
import com.resolveiq.ticket.domain.model.*;
import com.resolveiq.ticket.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMessageRepository messageRepository;
    @Mock
    private TicketStatusHistoryRepository historyRepository;
    @Mock
    private AiSuggestionRepository suggestionRepository;
    @Mock
    private SuggestionFeedbackRepository feedbackRepository;
    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyRepository;

    private ObjectMapper objectMapper;
    private TicketService ticketService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private StaffTeamMembershipRepository membershipRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        ticketService = new TicketService(
            ticketRepository,
            messageRepository,
            historyRepository,
            suggestionRepository,
            feedbackRepository,
            outboxRepository,
            idempotencyRepository,
            objectMapper,
            eventPublisher,
            membershipRepository
        );
        lenient().when(ticketRepository.getNextTicketSequenceVal()).thenReturn(100001L);
        lenient().when(idempotencyRepository.claim(any(), any(), any(), anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(1);
        lenient().when(idempotencyRepository.findByTenantIdAndActorIdAndOperationAndKey(any(), any(), anyString(), anyString()))
            .thenAnswer(invocation -> Optional.of(new IdempotencyKey(
                UUID.randomUUID(), invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2), invocation.getArgument(3), "claimed-request-hash",
                Instant.now().plusSeconds(3600)
            )));
    }

    @Test
    @DisplayName("Should create ticket and persist transactional outbox event")
    void testCreateTicketWithOutbox() {
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateTicketRequest request = new CreateTicketRequest(
            "Payment Failed",
            "Credit card was charged twice",
            "BILLING",
            TicketPriority.HIGH,
            "WEB",
            "en"
        );

        TicketResponse response = ticketService.createTicket(tenantId, customerId, request);

        assertThat(response).isNotNull();
        assertThat(response.ticketNumber()).startsWith("RIQ-");
        assertThat(response.status()).isEqualTo(TicketStatus.NEW);
        assertThat(response.aiTriageStatus()).isEqualTo("PENDING");

        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(historyRepository, times(1)).save(any(TicketStatusHistory.class));
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Should enforce ticket state machine transitions and prevent illegal jumps")
    void testStateMachineTransitions() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Ticket ticket = new Ticket(
            ticketId,
            "RIQ-2026-000101",
            tenantId,
            customerId,
            "Subject",
            "Desc",
            "BILLING",
            TicketPriority.HIGH,
            "WEB",
            "en"
        );

        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));

        // NEW -> READY_FOR_AGENT is valid
        ticketService.updateStatus(tenantId, ticketId, customerId, new UpdateStatusRequest(TicketStatus.READY_FOR_AGENT, "Triage completed"));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.READY_FOR_AGENT);

        // READY_FOR_AGENT -> IN_PROGRESS is valid
        ticketService.updateStatus(tenantId, ticketId, customerId, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, "Agent started working"));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        // IN_PROGRESS -> RESOLVED is valid
        ticketService.updateStatus(tenantId, ticketId, customerId, new UpdateStatusRequest(TicketStatus.RESOLVED, "Issue solved"));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        // RESOLVED -> CLOSED is valid
        ticketService.updateStatus(tenantId, ticketId, customerId, new UpdateStatusRequest(TicketStatus.CLOSED, "Closed by customer"));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);

        // CLOSED -> IN_PROGRESS is illegal
        assertThatThrownBy(() -> 
            ticketService.updateStatus(tenantId, ticketId, customerId, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, "Try reopen"))
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Illegal ticket state transition");
    }

    @Test
    @DisplayName("Should return cached response when valid Idempotency-Key is provided")
    void testIdempotentTicketCreation() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = "key-test-12345";
        CreateTicketRequest request = new CreateTicketRequest("Test Subject", "Test Description", "BILLING", TicketPriority.HIGH, "WEB", "en");

        TicketResponse mockCachedResponse = new TicketResponse(
            UUID.randomUUID(),
            "RIQ-2026-000999",
            tenantId,
            customerId,
            null,
            null,
            "Test Subject",
            "Test Description",
            "en",
            TicketStatus.NEW,
            TicketPriority.HIGH,
            "BILLING",
            null,
            null,
            null,
            null,
            "WEB",
            null,
            null,
            null,
            "PENDING",
            null,
            java.time.Instant.now(),
            java.time.Instant.now(),
            null,
            null,
            0L
        );

        String cachedJson = objectMapper.writeValueAsString(mockCachedResponse);
        IdempotencyKey keyRecord = new IdempotencyKey(
            UUID.randomUUID(), tenantId, customerId, "CREATE_TICKET", idempotencyKey,
            requestHash(request), Instant.now().plusSeconds(3600)
        );
        keyRecord.complete(201, cachedJson);
        when(idempotencyRepository.claim(any(), eq(tenantId), eq(customerId), anyString(), eq(idempotencyKey), anyString(), any(), any()))
            .thenReturn(0);
        when(idempotencyRepository.findByTenantIdAndActorIdAndOperationAndKey(tenantId, customerId, "CREATE_TICKET", idempotencyKey))
            .thenReturn(Optional.of(keyRecord));

        TicketResponse response = ticketService.createTicket(tenantId, customerId, request, idempotencyKey);

        assertThat(response).isNotNull();
        assertThat(response.ticketNumber()).isEqualTo("RIQ-2026-000999");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IdempotencyConflictException when key reused with different request payload")
    void testIdempotencyConflictOnPayloadMismatch() {
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = "key-conflict-test";

        CreateTicketRequest request = new CreateTicketRequest(
            "Payment Issue Changed",
            "Changed Description",
            "BILLING",
            TicketPriority.HIGH,
            "WEB",
            "en"
        );

        IdempotencyKey keyRecord = new IdempotencyKey(
            UUID.randomUUID(), tenantId, customerId, "CREATE_TICKET", idempotencyKey,
            "different_precomputed_hash_value", Instant.now().plusSeconds(3600)
        );
        when(idempotencyRepository.claim(any(), eq(tenantId), eq(customerId), anyString(), eq(idempotencyKey), anyString(), any(), any()))
            .thenReturn(0);
        when(idempotencyRepository.findByTenantIdAndActorIdAndOperationAndKey(tenantId, customerId, "CREATE_TICKET", idempotencyKey))
            .thenReturn(Optional.of(keyRecord));

        assertThatThrownBy(() -> ticketService.createTicket(tenantId, customerId, request, idempotencyKey))
            .isInstanceOf(com.resolveiq.ticket.domain.exception.IdempotencyConflictException.class)
            .hasMessageContaining("Idempotency key reused with differing request payload");
    }

    @Test
    @DisplayName("Should auto-transition status to IN_PROGRESS when customer replies while WAITING_ON_CUSTOMER")
    void testCustomerReplyTransitionsStatus() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Ticket ticket = new Ticket(
            ticketId,
            "RIQ-2026-000102",
            tenantId,
            customerId,
            "Subject",
            "Desc",
            "BILLING",
            TicketPriority.HIGH,
            "WEB",
            "en"
        );
        ticket.transitionTo(TicketStatus.READY_FOR_AGENT);
        ticket.transitionTo(TicketStatus.WAITING_ON_CUSTOMER);

        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));

        ticketService.addMessage(
            tenantId,
            ticketId,
            customerId,
            "CUSTOMER",
            new AddMessageRequest("Here is the requested screenshot", false)
        );

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(messageRepository, times(1)).save(any(TicketMessage.class));
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    private String requestHash(CreateTicketRequest request) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        String raw = String.format("%s|%s|%s|%s|%s|%s",
            request.subject().trim(), request.description().trim(), request.category().trim(),
            request.priority().name(), request.channel().trim(), request.language().trim());
        return java.util.HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
