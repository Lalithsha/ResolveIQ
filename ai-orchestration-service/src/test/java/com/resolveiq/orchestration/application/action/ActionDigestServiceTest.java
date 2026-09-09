package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.application.action.digest.ActionDigestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActionDigestServiceTest {

    private ActionDigestService digestService;

    @BeforeEach
    void setUp() {
        digestService = new ActionDigestService();
    }

    @Test
    @DisplayName("Canonical JSON digest v1 is stable regardless of map insertion order")
    void canonicalDigestStableRegardlessOfKeyOrder() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-09-10T12:00:00Z");

        Map<String, Object> inputOrder1 = new LinkedHashMap<>();
        inputOrder1.put("currency", "USD");
        inputOrder1.put("amountCents", 2500L);
        inputOrder1.put("paymentReference", "pay_123");

        Map<String, Object> inputOrder2 = new LinkedHashMap<>();
        inputOrder2.put("paymentReference", "pay_123");
        inputOrder2.put("amountCents", 2500L);
        inputOrder2.put("currency", "USD");

        var digest1 = digestService.generateDigestV1(
                tenantId, ticketId, proposalId, "REFUND_DUPLICATE_CHARGE", "pay_123",
                inputOrder1, "1", "1.0", Map.of("requiredApprovalCount", 1), expiresAt);

        var digest2 = digestService.generateDigestV1(
                tenantId, ticketId, proposalId, "REFUND_DUPLICATE_CHARGE", "pay_123",
                inputOrder2, "1", "1.0", Map.of("requiredApprovalCount", 1), expiresAt);

        assertThat(digest1.digestHex()).isEqualTo(digest2.digestHex());
        assertThat(digest1.canonicalBytes()).isEqualTo(digest2.canonicalBytes());
    }

    @Test
    @DisplayName("Any material modification in input alters the canonical digest")
    void materialInputModificationAltersDigest() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-09-10T12:00:00Z");

        var original = digestService.generateDigestV1(
                tenantId, ticketId, proposalId, "REFUND_DUPLICATE_CHARGE", "pay_123",
                Map.of("amountCents", 2500L, "currency", "USD"), "1", "1.0",
                Map.of("requiredApprovalCount", 1), expiresAt);

        var modifiedAmount = digestService.generateDigestV1(
                tenantId, ticketId, proposalId, "REFUND_DUPLICATE_CHARGE", "pay_123",
                Map.of("amountCents", 2501L, "currency", "USD"), "1", "1.0",
                Map.of("requiredApprovalCount", 1), expiresAt);

        var modifiedTarget = digestService.generateDigestV1(
                tenantId, ticketId, proposalId, "REFUND_DUPLICATE_CHARGE", "pay_456",
                Map.of("amountCents", 2500L, "currency", "USD"), "1", "1.0",
                Map.of("requiredApprovalCount", 1), expiresAt);

        var modifiedStateVersion = digestService.generateDigestV1(
                tenantId, ticketId, proposalId, "REFUND_DUPLICATE_CHARGE", "pay_123",
                Map.of("amountCents", 2500L, "currency", "USD"), "2", "1.0",
                Map.of("requiredApprovalCount", 1), expiresAt);

        assertThat(original.digestHex()).isNotEqualTo(modifiedAmount.digestHex());
        assertThat(original.digestHex()).isNotEqualTo(modifiedTarget.digestHex());
        assertThat(original.digestHex()).isNotEqualTo(modifiedStateVersion.digestHex());
    }
}
