package com.resolveiq.ticket.adapter.out.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.ticket.application.port.omnichannel.*;
import com.resolveiq.ticket.domain.model.omnichannel.ChannelType;
import com.resolveiq.ticket.domain.model.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimulatedEmailChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailChannelAdapter.class);
    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300L; // 5 minutes

    private final byte[] webhookSecret;
    private final ObjectMapper objectMapper;
    private final Map<String, OutboundMessage> simulatedMailbox = new ConcurrentHashMap<>();

    public SimulatedEmailChannelAdapter(
        @Value("${resolveiq.webhook.email-secret:resolveiq_fictional_email_webhook_secret_key_12345}") String secret,
        ObjectMapper objectMapper
    ) {
        this.webhookSecret = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    public ChannelType channel() {
        return ChannelType.EMAIL;
    }

    @Override
    public VerifiedInboundEvent verifyAndNormalize(WebhookRequest request) {
        Map<String, String> headers = request.headers();
        String signatureHeader = getHeader(headers, "X-ResolveIQ-Signature");
        String timestampHeader = getHeader(headers, "X-ResolveIQ-Timestamp");

        if (signatureHeader == null || timestampHeader == null) {
            throw new SecurityException("Missing required webhook authentication headers");
        }

        // 1. Validate Timestamp Tolerance
        long webhookEpochSeconds;
        try {
            webhookEpochSeconds = Long.parseLong(timestampHeader);
        } catch (NumberFormatException e) {
            try {
                webhookEpochSeconds = Instant.parse(timestampHeader).getEpochSecond();
            } catch (Exception ex) {
                throw new SecurityException("Invalid webhook timestamp format: " + timestampHeader);
            }
        }

        long currentEpochSeconds = Instant.now().getEpochSecond();
        if (Math.abs(currentEpochSeconds - webhookEpochSeconds) > MAX_TIMESTAMP_SKEW_SECONDS) {
            throw new SecurityException("Webhook timestamp skew exceeds maximum tolerance of 300 seconds");
        }

        // 2. Validate HMAC-SHA256 Signature
        String expectedPrefix = "sha256=";
        String rawSig = signatureHeader.startsWith(expectedPrefix) 
            ? signatureHeader.substring(expectedPrefix.length()) 
            : signatureHeader;

        String computedSig = computeHmacHex(request.rawBody(), timestampHeader);
        if (!MessageDigest.isEqual(rawSig.toLowerCase().getBytes(StandardCharsets.UTF_8),
                                  computedSig.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("Webhook signature verification failed");
        }

        // 3. Deserialize JSON Payload
        try {
            JsonNode root = objectMapper.readTree(request.rawBody());
            String providerEventId = root.has("eventId") ? root.get("eventId").asText() : UUID.randomUUID().toString();
            String from = root.path("from").asText();
            if (from.isBlank()) from = root.path("sender").asText();
            String to = root.path("to").asText();
            if (to.isBlank()) to = root.path("recipient").asText();
            String subject = root.path("subject").asText("");
            String body = root.path("body").asText();
            if (body.isBlank()) body = root.path("text").asText("");
            String externalMessageId = root.has("messageId") 
                ? root.get("messageId").asText() 
                : root.path("externalMessageId").asText(providerEventId);

            Instant providerTimestamp = root.has("timestamp") 
                ? Instant.parse(root.get("timestamp").asText()) 
                : Instant.ofEpochSecond(webhookEpochSeconds);

            return new VerifiedInboundEvent(
                providerEventId,
                from,
                to,
                subject,
                body,
                externalMessageId,
                providerTimestamp,
                Collections.emptyMap()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize normalized email payload: " + e.getMessage(), e);
        }
    }

    @Override
    public DeliveryReceipt send(OutboundMessage message, String idempotencyKey) {
        if (message.isInternal()) {
            throw new IllegalArgumentException("Internal notes are strictly prohibited from outbound channels!");
        }

        String providerMsgId = "sim_email_" + UUID.randomUUID().toString().replace("-", "");
        simulatedMailbox.put(providerMsgId, message);
        log.info("Simulated email dispatched: providerMsgId={}, recipient={}, subject='{}', idempotencyKey={}",
            providerMsgId, message.recipientAddress(), message.subject(), idempotencyKey);

        return new DeliveryReceipt(
            providerMsgId,
            DeliveryStatus.DELIVERED,
            Instant.now(),
            null
        );
    }

    @Override
    public DeliveryStatus fetchStatus(String providerMessageId) {
        if (simulatedMailbox.containsKey(providerMessageId)) {
            return DeliveryStatus.DELIVERED;
        }
        return DeliveryStatus.UNKNOWN;
    }

    public String computeHmacHex(byte[] body, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret, "HmacSHA256"));
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update(".".getBytes(StandardCharsets.UTF_8));
            byte[] hmacBytes = mac.doFinal(body);
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private String getHeader(Map<String, String> headers, String target) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(target)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
