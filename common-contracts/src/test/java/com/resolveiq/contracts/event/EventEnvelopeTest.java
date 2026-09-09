package com.resolveiq.contracts.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should create event envelope with default correlation ID and timestamp")
    void testCreateEventEnvelope() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        TicketEvents.TicketCreatedPayload payload = new TicketEvents.TicketCreatedPayload(
            ticketId,
            "RIQ-2026-000100",
            tenantId,
            customerId,
            "Test ticket",
            "Description of test ticket",
            "WEB",
            "MEDIUM",
            "TECHNICAL",
            Instant.now()
        );

        EventEnvelope<TicketEvents.TicketCreatedPayload> envelope = EventEnvelope.create(
            TicketEvents.TICKET_CREATED,
            1,
            "ticket-service",
            tenantId,
            "ticket",
            ticketId,
            null,
            null,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
            payload
        );

        assertThat(envelope).isNotNull();
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("resolveiq.ticket.created");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.producer()).isEqualTo("ticket-service");
        assertThat(envelope.tenantId()).isEqualTo(tenantId);
        assertThat(envelope.aggregateId()).isEqualTo(ticketId);
        assertThat(envelope.correlationId()).isNotNull();
        assertThat(envelope.payload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Should serialize and deserialize IncidentProposed event")
    void testIncidentProposedEventSerialization() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID clusterId = UUID.randomUUID();
        IncidentEvents.IncidentProposedPayload payload = new IncidentEvents.IncidentProposedPayload(
            clusterId,
            tenantId,
            "BILLING",
            "Checkout API",
            12,
            3.5,
            List.of(UUID.randomUUID(), UUID.randomUUID()),
            List.of("ERR_PAYMENT_GATEWAY_TIMEOUT"),
            "Volume spike in billing checkouts",
            "cluster-v1.0",
            Instant.now()
        );

        EventEnvelope<IncidentEvents.IncidentProposedPayload> envelope = EventEnvelope.create(
            IncidentEvents.INCIDENT_PROPOSED,
            1,
            "ticket-service",
            tenantId,
            "incident_cluster",
            clusterId,
            null,
            null,
            "trace-1",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        EventEnvelope<IncidentEvents.IncidentProposedPayload> deserialized =
            objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(deserialized.eventType()).isEqualTo(IncidentEvents.INCIDENT_PROPOSED);
        assertThat(deserialized.payload().dominantCategory()).isEqualTo("BILLING");
        assertThat(deserialized.payload().ticketCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("Should serialize and deserialize ActionExecution events")
    void testActionEventsSerialization() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        ActionEvents.ActionExecutionCompletedPayload payload = new ActionEvents.ActionExecutionCompletedPayload(
            UUID.randomUUID(),
            proposalId,
            UUID.randomUUID(),
            tenantId,
            "REFUND_DUPLICATE_CHARGE",
            "SUCCEEDED",
            "sim-ref-12345",
            "RECONCILED",
            null,
            Instant.now()
        );

        EventEnvelope<ActionEvents.ActionExecutionCompletedPayload> envelope = EventEnvelope.create(
            ActionEvents.ACTION_EXECUTION_COMPLETED,
            1,
            "ai-orchestration-service",
            tenantId,
            "action_execution",
            proposalId,
            null,
            null,
            "trace-2",
            payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        EventEnvelope<ActionEvents.ActionExecutionCompletedPayload> deserialized =
            objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(deserialized.eventType()).isEqualTo(ActionEvents.ACTION_EXECUTION_COMPLETED);
        assertThat(deserialized.payload().status()).isEqualTo("SUCCEEDED");
        assertThat(deserialized.payload().reconciliationStatus()).isEqualTo("RECONCILED");
    }
}
