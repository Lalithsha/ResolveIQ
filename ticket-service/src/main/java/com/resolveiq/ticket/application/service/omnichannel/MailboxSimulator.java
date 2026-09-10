package com.resolveiq.ticket.application.service.omnichannel;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated Mailbox Simulator for simulated email delivery and test inspection.
 * Production API responses never leak challenge tokens or magic links.
 */
@Component
public class MailboxSimulator {

    public record ChallengeRecord(
        UUID tenantId,
        String email,
        String challengeCode,
        Instant expiresAt,
        Instant sentAt
    ) {}

    private final Map<String, ChallengeRecord> mailbox = new ConcurrentHashMap<>();

    public void deliverChallenge(UUID tenantId, String normalizedEmail, String challengeCode, Instant expiresAt) {
        String key = buildKey(tenantId, normalizedEmail);
        mailbox.put(key, new ChallengeRecord(tenantId, normalizedEmail, challengeCode, expiresAt, Instant.now()));
    }

    public Optional<String> getLatestChallengeCode(UUID tenantId, String normalizedEmail) {
        String key = buildKey(tenantId, normalizedEmail);
        ChallengeRecord record = mailbox.get(key);
        if (record != null && Instant.now().isBefore(record.expiresAt())) {
            return Optional.of(record.challengeCode());
        }
        return Optional.empty();
    }

    public void clear(UUID tenantId, String normalizedEmail) {
        mailbox.remove(buildKey(tenantId, normalizedEmail));
    }

    public void reset() {
        mailbox.clear();
    }

    private String buildKey(UUID tenantId, String normalizedEmail) {
        return tenantId + ":" + (normalizedEmail != null ? normalizedEmail.trim().toLowerCase() : "");
    }
}
