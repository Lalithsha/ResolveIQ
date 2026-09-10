package com.resolveiq.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.ticket.application.dto.IncidentDtos.*;
import com.resolveiq.ticket.application.port.NotificationPort;
import com.resolveiq.ticket.application.port.SignalAdapter;
import com.resolveiq.ticket.application.port.TicketSimilarityPort;
import com.resolveiq.ticket.application.service.IncidentService;
import com.resolveiq.ticket.domain.model.*;
import com.resolveiq.ticket.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private SupportIncidentRepository incidentRepository;
    @Mock private IncidentClusterRepository clusterRepository;
    @Mock private IncidentTicketLinkRepository linkRepository;
    @Mock private IncidentComponentRepository componentRepository;
    @Mock private IncidentUpdateRepository updateRepository;
    @Mock private CustomerImpactRepository impactRepository;
    @Mock private NotificationSubscriptionRepository subscriptionRepository;
    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private TicketSimilarityPort similarityPort;
    @Mock private SignalAdapter signalAdapter;
    @Mock private NotificationPort notificationPort;

    private IncidentService incidentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        incidentService = new IncidentService(
            incidentRepository,
            clusterRepository,
            linkRepository,
            componentRepository,
            updateRepository,
            impactRepository,
            subscriptionRepository,
            deliveryRepository,
            ticketRepository,
            outboxRepository,
            similarityPort,
            signalAdapter,
            notificationPort,
            objectMapper
        );
    }

    @Test
    @DisplayName("Confirming a proposed incident should transition to INVESTIGATING and record actor")
    void testConfirmIncident() {
        UUID tenantId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        SupportIncident incident = new SupportIncident(
            incidentId,
            tenantId,
            "INC-1001",
            "Payment gateway timeout spike",
            IncidentStatus.PROPOSED,
            IncidentSeverity.HIGH,
            Instant.now(),
            UUID.randomUUID(),
            "Volume spike in billing checkouts",
            "v1.0",
            0.92
        );

        when(incidentRepository.findByTenantIdAndId(tenantId, incidentId)).thenReturn(Optional.of(incident));
        when(clusterRepository.findAll()).thenReturn(List.of());

        IncidentResponse response = incidentService.confirmIncident(tenantId, incidentId, leadId);

        assertThat(response.status()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(incident.getOwnerUserId()).isEqualTo(leadId);
        verify(incidentRepository, times(1)).save(incident);
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("High-severity updates must enforce two-person rule preventing author from approving own update")
    void testTwoPersonRuleForHighSeverityIncident() {
        UUID tenantId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();

        SupportIncident incident = new SupportIncident(
            incidentId,
            tenantId,
            "INC-1002",
            "Critical payment outage",
            IncidentStatus.INVESTIGATING,
            IncidentSeverity.HIGH,
            Instant.now(),
            authorId,
            "Major billing failure",
            "v1.0",
            0.95
        );

        IncidentUpdate update = new IncidentUpdate(
            updateId,
            tenantId,
            incidentId,
            1,
            IncidentUpdateStatus.DRAFT,
            "Investigating billing timeouts",
            "Engineering is actively rolling back the canary deployment.",
            AudienceType.ALL_AFFECTED,
            5,
            authorId
        );

        when(incidentRepository.findByTenantIdAndId(tenantId, incidentId)).thenReturn(Optional.of(incident));
        when(updateRepository.findByTenantIdAndId(tenantId, updateId)).thenReturn(Optional.of(update));

        assertThatThrownBy(() -> incidentService.approveUpdate(tenantId, incidentId, updateId, authorId, Set.of("TEAM_LEAD")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Two-person rule violation");
    }

    @Test
    @DisplayName("Publishing approved update must deliver notification to all affected customers")
    void testPublishUpdateDispatchesDeliveries() {
        UUID tenantId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID updateId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        SupportIncident incident = new SupportIncident(
            incidentId,
            tenantId,
            "INC-1003",
            "API degradation",
            IncidentStatus.INVESTIGATING,
            IncidentSeverity.MEDIUM,
            Instant.now(),
            publisherId,
            "API failure",
            "v1.0",
            0.88
        );

        IncidentUpdate update = new IncidentUpdate(
            updateId,
            tenantId,
            incidentId,
            1,
            IncidentUpdateStatus.APPROVED,
            "Mitigation in progress",
            "Traffic rerouted to secondary provider.",
            AudienceType.ALL_AFFECTED,
            1,
            UUID.randomUUID()
        );

        CustomerImpact impact = new CustomerImpact(UUID.randomUUID(), tenantId, incidentId, customerId, ticketId, "DIRECT");

        when(incidentRepository.findByTenantIdAndId(tenantId, incidentId)).thenReturn(Optional.of(incident));
        when(updateRepository.findByTenantIdAndId(tenantId, updateId)).thenReturn(Optional.of(update));
        when(impactRepository.findByTenantIdAndIncidentId(tenantId, incidentId)).thenReturn(List.of(impact));
        when(deliveryRepository.findByTenantIdAndUpdateIdAndRecipientCustomerIdAndChannel(any(), any(), any(), any()))
            .thenReturn(Optional.empty());
        when(notificationPort.sendNotification(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new NotificationPort.DeliveryResult(DeliveryStatus.SENT, "provider-msg-123", null));

        IncidentUpdateResponse res = incidentService.publishUpdate(tenantId, incidentId, updateId, publisherId);

        assertThat(res.status()).isEqualTo(IncidentUpdateStatus.PUBLISHED);
        verify(notificationPort, times(1)).sendNotification(any(), any(), eq(customerId), eq("PORTAL"), any(), any(), any());
        verify(deliveryRepository, times(2)).save(any(NotificationDelivery.class));
    }

    @Test
    @DisplayName("Detection scan creates proposal when anomaly threshold is exceeded")
    void testDetectionScanVolumeSpike() {
        UUID tenantId = UUID.randomUUID();

        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID customerId = UUID.randomUUID();
            Ticket t = new Ticket(
                UUID.randomUUID(),
                "RIQ-2026-" + (200 + i),
                tenantId,
                customerId,
                "Checkout failed ERR_PAYMENT_GATEWAY_TIMEOUT",
                "Customer unable to place order due to timeout ERR_PAYMENT_GATEWAY_TIMEOUT",
                "BILLING",
                TicketPriority.HIGH,
                "PORTAL",
                "en"
            );
            tickets.add(t);
        }

        when(ticketRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(tenantId), any(Instant.class), any(Pageable.class)))
            .thenReturn(tickets);
        when(clusterRepository.findByTenantIdAndCentroidHash(eq(tenantId), anyString()))
            .thenReturn(Optional.empty());

        DetectionRunResult result = incidentService.runDetectionScan(tenantId);

        assertThat(result.evaluatedTickets()).isEqualTo(10);
        assertThat(result.proposalsCreated()).isEqualTo(1);
        assertThat(result.ticketsLinked()).isEqualTo(10);
        verify(incidentRepository, times(1)).save(any(SupportIncident.class));
        verify(clusterRepository, times(1)).save(any(IncidentCluster.class));
    }

    @Test
    @DisplayName("C2: Old tickets outside 15-minute window or sub-threshold tickets do not create proposals")
    void testOldTicketsDoNotCreateProposals() {
        UUID tenantId = UUID.randomUUID();

        // 1. Zero recent tickets in window -> no incident
        when(ticketRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(tenantId), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of());

        DetectionRunResult resultEmpty = incidentService.runDetectionScan(tenantId, 10, 8);
        assertThat(resultEmpty.evaluatedTickets()).isEqualTo(0);
        assertThat(resultEmpty.proposalsCreated()).isEqualTo(0);

        // 2. 9 recent tickets (below minTickets 10) -> no incident
        List<Ticket> nineTickets = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nineTickets.add(new Ticket(
                UUID.randomUUID(), "RIQ-2026-" + (300 + i), tenantId, UUID.randomUUID(),
                "Checkout failed timeout", "Description", "BILLING", TicketPriority.HIGH, "PORTAL", "en"
            ));
        }
        when(ticketRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(tenantId), any(Instant.class), any(Pageable.class)))
            .thenReturn(nineTickets);

        DetectionRunResult resultNine = incidentService.runDetectionScan(tenantId, 10, 8);
        assertThat(resultNine.evaluatedTickets()).isEqualTo(9);
        assertThat(resultNine.proposalsCreated()).isEqualTo(0);

        // 3. 10 tickets but only 7 distinct customers (below minCustomers 8) -> no incident
        List<Ticket> sevenCustomerTickets = new ArrayList<>();
        UUID[] customers = new UUID[7];
        for (int i = 0; i < 7; i++) customers[i] = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            sevenCustomerTickets.add(new Ticket(
                UUID.randomUUID(), "RIQ-2026-" + (400 + i), tenantId, customers[i % 7],
                "Checkout failed timeout", "Description", "BILLING", TicketPriority.HIGH, "PORTAL", "en"
            ));
        }
        when(ticketRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(tenantId), any(Instant.class), any(Pageable.class)))
            .thenReturn(sevenCustomerTickets);

        DetectionRunResult resultSevenCust = incidentService.runDetectionScan(tenantId, 10, 8);
        assertThat(resultSevenCust.evaluatedTickets()).isEqualTo(10);
        assertThat(resultSevenCust.proposalsCreated()).isEqualTo(0);
    }
}
