package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel_identities", schema = "ticket_schema")
public class ChannelIdentity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Column(name = "address_hmac", nullable = false, length = 128)
    private String addressHmac;

    @Column(name = "display_address", nullable = false, length = 255)
    private String displayAddress;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "verification_challenge_hash", length = 128)
    private String verificationChallengeHash;

    @Column(name = "challenge_expires_at")
    private Instant challengeExpiresAt;

    @Column(name = "challenge_attempts", nullable = false)
    private int challengeAttempts;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ChannelIdentity() {}

    public ChannelIdentity(UUID tenantId, UUID customerId, ChannelType channel, String addressHmac, String displayAddress, boolean isVerified) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.channel = channel;
        this.addressHmac = addressHmac;
        this.displayAddress = displayAddress;
        this.isVerified = isVerified;
        this.challengeAttempts = 0;
        this.confidence = 1.0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (isVerified) {
            this.verifiedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public ChannelType getChannel() { return channel; }
    public String getAddressHmac() { return addressHmac; }
    public String getDisplayAddress() { return displayAddress; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) {
        this.isVerified = verified;
        if (verified) {
            this.verifiedAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }
    public String getVerificationChallengeHash() { return verificationChallengeHash; }
    public Instant getChallengeExpiresAt() { return challengeExpiresAt; }
    public int getChallengeAttempts() { return challengeAttempts; }
    public void incrementChallengeAttempts() { this.challengeAttempts++; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public double getConfidence() { return confidence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setChallenge(String challengeHash, Instant expiresAt) {
        this.verificationChallengeHash = challengeHash;
        this.challengeExpiresAt = expiresAt;
        this.challengeAttempts = 0;
        this.updatedAt = Instant.now();
    }

    public void markVerified(UUID verifiedCustomerId) {
        this.customerId = verifiedCustomerId;
        this.isVerified = true;
        this.verifiedAt = Instant.now();
        this.verificationChallengeHash = null;
        this.challengeExpiresAt = null;
        this.updatedAt = Instant.now();
    }
}
