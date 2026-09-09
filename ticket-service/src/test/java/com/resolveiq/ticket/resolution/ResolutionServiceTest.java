package com.resolveiq.ticket.resolution;

import com.resolveiq.ticket.application.dto.resolution.ResolutionDtos.*;
import com.resolveiq.ticket.application.service.resolution.ResolutionService;
import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketPriority;
import com.resolveiq.ticket.domain.model.TicketStatus;
import com.resolveiq.ticket.domain.model.resolution.*;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.resolution.ResolutionOutcomeRepository;
import com.resolveiq.ticket.domain.repository.resolution.TicketResolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolutionServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketResolutionRepository resolutionRepository;
    @Mock private ResolutionOutcomeRepository outcomeRepository;

    private ResolutionService resolutionService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID resolverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        resolutionService = new ResolutionService(ticketRepository, resolutionRepository, outcomeRepository);
    }

    @Test
    @DisplayName("Resolving ticket creates attempt 1 with 7-day confirmation window and status AWAITING_CONFIRMATION")
    void resolveTicketCreatesAttempt() {
        Ticket ticket = new Ticket(ticketId, "RIQ-1001", tenantId, customerId, "SAML Error", "SSO invalid", "SSO", TicketPriority.HIGH, "WEB", "en");
        ticket.transitionTo(TicketStatus.READY_FOR_AGENT);
        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));
        when(resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)).thenReturn(Optional.empty());
        when(resolutionRepository.save(any(TicketResolution.class))).thenAnswer(i -> i.getArgument(0));

        ResolutionAttemptResponse response = resolutionService.resolveTicket(tenantId, ticketId, resolverId, "FIX_SSO_CERT");

        assertThat(response.attemptNumber()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("AWAITING_CONFIRMATION");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.confirmationWindowExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Outcome YES confirms attempt, sets 24h grace period closure, score 100")
    void outcomeYesConfirmsAttempt() {
        Ticket ticket = new Ticket(ticketId, "RIQ-1001", tenantId, customerId, "SAML Error", "SSO invalid", "SSO", TicketPriority.HIGH, "WEB", "en");
        ticket.transitionTo(TicketStatus.READY_FOR_AGENT);
        ticket.transitionTo(TicketStatus.RESOLVED);
        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));

        TicketResolution res = new TicketResolution(tenantId, ticketId, 1, resolverId, "FIX_SSO_CERT");
        when(resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)).thenReturn(Optional.of(res));
        when(resolutionRepository.save(any(TicketResolution.class))).thenAnswer(i -> i.getArgument(0));

        ResolutionAttemptResponse response = resolutionService.recordOutcome(tenantId, ticketId, customerId, "YES", "Problem completely solved!");

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.score()).isEqualTo(100);
        assertThat(response.scheduledClosureAt()).isNotNull();
        verify(outcomeRepository).save(any(ResolutionOutcome.class));
    }

    @Test
    @DisplayName("Outcome PARTLY marks partial and transitions ticket back to IN_PROGRESS")
    void outcomePartlyReopensTicket() {
        Ticket ticket = new Ticket(ticketId, "RIQ-1001", tenantId, customerId, "SAML Error", "SSO invalid", "SSO", TicketPriority.HIGH, "WEB", "en");
        ticket.transitionTo(TicketStatus.READY_FOR_AGENT);
        ticket.transitionTo(TicketStatus.RESOLVED);
        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));

        TicketResolution res = new TicketResolution(tenantId, ticketId, 1, resolverId, "FIX_SSO_CERT");
        when(resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)).thenReturn(Optional.of(res));
        when(resolutionRepository.save(any(TicketResolution.class))).thenAnswer(i -> i.getArgument(0));

        ResolutionAttemptResponse response = resolutionService.recordOutcome(tenantId, ticketId, customerId, "PARTLY", "Login works but redirect is slow");

        assertThat(response.status()).isEqualTo("PARTIAL");
        assertThat(response.score()).isEqualTo(20);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Outcome NO marks rejected, reopens ticket to IN_PROGRESS immediately")
    void outcomeNoReopensTicketImmediately() {
        Ticket ticket = new Ticket(ticketId, "RIQ-1001", tenantId, customerId, "SAML Error", "SSO invalid", "SSO", TicketPriority.HIGH, "WEB", "en");
        ticket.transitionTo(TicketStatus.READY_FOR_AGENT);
        ticket.transitionTo(TicketStatus.RESOLVED);
        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));

        TicketResolution res = new TicketResolution(tenantId, ticketId, 1, resolverId, "FIX_SSO_CERT");
        when(resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)).thenReturn(Optional.of(res));
        when(resolutionRepository.save(any(TicketResolution.class))).thenAnswer(i -> i.getArgument(0));

        ResolutionAttemptResponse response = resolutionService.recordOutcome(tenantId, ticketId, customerId, "NO", "Still getting error");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.score()).isLessThanOrEqualTo(-50);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Explicit customer reopen supersedes resolution attempt with score penalty")
    void reopenSupersedesResolution() {
        Ticket ticket = new Ticket(ticketId, "RIQ-1001", tenantId, customerId, "SAML Error", "SSO invalid", "SSO", TicketPriority.HIGH, "WEB", "en");
        ticket.transitionTo(TicketStatus.READY_FOR_AGENT);
        ticket.transitionTo(TicketStatus.RESOLVED);
        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));

        TicketResolution res = new TicketResolution(tenantId, ticketId, 1, resolverId, "FIX_SSO_CERT");
        when(resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)).thenReturn(Optional.of(res));
        when(resolutionRepository.save(any(TicketResolution.class))).thenAnswer(i -> i.getArgument(0));

        ResolutionAttemptResponse response = resolutionService.reopenTicket(tenantId, ticketId, customerId, "Reopening ticket");

        assertThat(response.status()).isEqualTo("SUPERSEDED");
        assertThat(response.score()).isLessThanOrEqualTo(-50);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }
}
