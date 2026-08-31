package com.resolveiq.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketPriority;
import com.resolveiq.ticket.domain.model.TicketStatus;
import com.resolveiq.ticket.domain.repository.AiSuggestionRepository;
import com.resolveiq.ticket.domain.repository.ProcessedEventRepository;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "resolveiq.attachments.scanner=deterministic"
})
class KafkaDeliveryIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("resolveiq")
        .withUsername("resolveiq_test")
        .withPassword("resolveiq_test_password");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void dependencies(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired TicketRepository tickets;
    @Autowired ProcessedEventRepository processedEvents;
    @Autowired AiSuggestionRepository suggestions;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ObjectMapper objectMapper;

    @Test
    void duplicateKafkaDeliveryChangesTheTicketExactlyOnce() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        tickets.saveAndFlush(new Ticket(ticketId, "RIQ-IT-" + ticketId.toString().substring(0, 8),
            tenantId, UUID.randomUUID(), "Duplicate payment", "A payment was captured twice",
            "BILLING", TicketPriority.HIGH, "WEB", "en"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketId", ticketId);
        payload.put("intent", "billing_dispute");
        payload.put("category", "BILLING");
        payload.put("sentiment", "NEGATIVE");
        payload.put("urgency", "HIGH");
        payload.put("confidence", 0.91);
        payload.put("suggestionId", suggestionId);
        payload.put("suggestedResponse", "We are reviewing the duplicate authorization safely.");
        payload.put("modelName", "deterministic-it");
        payload.put("promptVersion", "it-v1");
        payload.put("citationsJson", "[]");

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", TicketEvents.TICKET_TRIAGE_COMPLETED);
        envelope.put("eventVersion", 1);
        envelope.put("aggregateId", ticketId);
        envelope.put("tenantId", tenantId);
        envelope.put("occurredAt", Instant.now());
        envelope.put("payload", payload);
        String message = objectMapper.writeValueAsString(envelope);

        kafka.send(TicketEvents.TICKET_TRIAGE_COMPLETED, ticketId.toString(), message).get();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(processedEvents.existsByEventIdAndConsumerGroup(eventId, "resolveiq-ticket-service")).isTrue();
            assertThat(suggestions.findById(suggestionId)).isPresent();
            assertThat(tickets.findById(ticketId).orElseThrow().getStatus()).isEqualTo(TicketStatus.READY_FOR_AGENT);
        });

        kafka.send(TicketEvents.TICKET_TRIAGE_COMPLETED, ticketId.toString(), message).get();
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(processedEvents.count()).isEqualTo(1);
            assertThat(suggestions.count()).isEqualTo(1);
            assertThat(tickets.findById(ticketId).orElseThrow().getLatestSuggestionId()).isEqualTo(suggestionId);
        });
    }
}
