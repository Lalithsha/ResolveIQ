package com.resolveiq.ticket.omnichannel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.ticket.adapter.out.email.SimulatedEmailChannelAdapter;
import com.resolveiq.ticket.application.port.omnichannel.OutboundMessage;
import com.resolveiq.ticket.application.port.omnichannel.VerifiedInboundEvent;
import com.resolveiq.ticket.application.port.omnichannel.WebhookRequest;
import com.resolveiq.ticket.domain.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedEmailChannelAdapterTest {

    private static final String SECRET = "test_webhook_secret_key_12345678";
    private SimulatedEmailChannelAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new SimulatedEmailChannelAdapter(SECRET, objectMapper);
    }

    @Test
    void testValidSignedWebhook() {
        String body = """
            {
                "eventId": "evt_101",
                "from": "user@example.com",
                "to": "support@resolveiq.io",
                "subject": "Need help with login",
                "body": "I cannot access my account.",
                "messageId": "msg_ext_001"
            }
            """;
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String hmac = adapter.computeHmacHex(bodyBytes, timestamp);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-ResolveIQ-Signature", "sha256=" + hmac);
        headers.put("X-ResolveIQ-Timestamp", timestamp);

        WebhookRequest req = new WebhookRequest(headers, bodyBytes, "/webhooks/v1/email/test");
        VerifiedInboundEvent event = adapter.verifyAndNormalize(req);

        assertNotNull(event);
        assertEquals("evt_101", event.providerEventId());
        assertEquals("user@example.com", event.senderAddress());
        assertEquals("Need help with login", event.subject());
        assertEquals("I cannot access my account.", event.bodyText());
    }

    @Test
    void testInvalidSignatureThrowsSecurityException() {
        String body = "{\"eventId\":\"evt_102\"}";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Map<String, String> headers = new HashMap<>();
        headers.put("X-ResolveIQ-Signature", "sha256=invalid_hex_signature");
        headers.put("X-ResolveIQ-Timestamp", timestamp);

        WebhookRequest req = new WebhookRequest(headers, bodyBytes, "/webhooks/v1/email/test");
        SecurityException ex = assertThrows(SecurityException.class, () -> adapter.verifyAndNormalize(req));
        assertTrue(ex.getMessage().contains("signature verification failed"));
    }

    @Test
    void testStaleTimestampThrowsSecurityException() {
        String body = "{\"eventId\":\"evt_103\"}";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        // 10 minutes ago (> 300s)
        String staleTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
        String hmac = adapter.computeHmacHex(bodyBytes, staleTimestamp);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-ResolveIQ-Signature", "sha256=" + hmac);
        headers.put("X-ResolveIQ-Timestamp", staleTimestamp);

        WebhookRequest req = new WebhookRequest(headers, bodyBytes, "/webhooks/v1/email/test");
        SecurityException ex = assertThrows(SecurityException.class, () -> adapter.verifyAndNormalize(req));
        assertTrue(ex.getMessage().contains("skew exceeds maximum tolerance"));
    }

    @Test
    void testOutboundSendSuccessAndInternalNoteRejection() {
        UUID tenantId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        // 1. Sending normal customer message succeeds
        OutboundMessage customerMsg = new OutboundMessage(
            tenantId, messageId, convId, "customer@example.com", "Ticket Update", "We solved it", false
        );
        var receipt = adapter.send(customerMsg, "idem_1");
        assertNotNull(receipt.providerMessageId());
        assertEquals(DeliveryStatus.DELIVERED, receipt.status());

        // 2. OutboundMessage constructor strictly rejects internal notes
        assertThrows(IllegalArgumentException.class, () -> new OutboundMessage(
            tenantId, messageId, convId, "customer@example.com", "Internal note", "Secret data", true
        ));
    }
}
