package com.resolveiq.ticket.application.service.omnichannel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class IdentityHashService {

    private final byte[] secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public IdentityHashService(
        @Value("${resolveiq.identity.hmac-secret:resolveiq_fictional_identity_hmac_secret_key_1234567890}") String secret
    ) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Normalizes email address:
     * - Preserves mailbox name (does NOT strip plus-addressing or dots)
     * - Lowercases domain case-insensitively
     */
    public String normalizeEmail(String rawEmail) {
        if (rawEmail == null || !rawEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email address: " + rawEmail);
        }
        String trimmed = rawEmail.trim();
        int atIdx = trimmed.lastIndexOf('@');
        String mailbox = trimmed.substring(0, atIdx);
        String domain = trimmed.substring(atIdx + 1).toLowerCase();
        return mailbox + "@" + domain;
    }

    /**
     * Computes a keyed HMAC-SHA256 of the normalized address.
     */
    public String computeAddressHmac(String normalizedEmail) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(normalizedEmail.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC for address", e);
        }
    }

    /**
     * Generates a 6-digit challenge code for linking.
     */
    public String generateChallengeCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Hashes challenge code with SHA-256 for secure storage.
     */
    public String hashChallengeCode(String challengeCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(challengeCode.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash challenge code", e);
        }
    }

    public boolean verifyChallenge(String rawCode, String storedHash) {
        if (rawCode == null || storedHash == null) {
            return false;
        }
        String computed = hashChallengeCode(rawCode);
        return MessageDigest.isEqual(
            computed.getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
