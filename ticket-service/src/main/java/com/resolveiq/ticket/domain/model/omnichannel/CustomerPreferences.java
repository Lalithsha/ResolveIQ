package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_preferences", schema = "ticket_schema")
public class CustomerPreferences {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel", nullable = false, length = 50)
    private ChannelType preferredChannel;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CustomerPreferences() {}

    public CustomerPreferences(UUID tenantId, UUID customerId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.preferredChannel = ChannelType.PORTAL;
        this.emailNotificationsEnabled = true;
        this.marketingConsent = false;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCustomerId() { return customerId; }
    public ChannelType getPreferredChannel() { return preferredChannel; }
    public void setPreferredChannel(ChannelType preferredChannel) {
        this.preferredChannel = preferredChannel;
        this.updatedAt = Instant.now();
    }
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.updatedAt = Instant.now();
    }
    public boolean isMarketingConsent() { return marketingConsent; }
    public void setMarketingConsent(boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
        this.updatedAt = Instant.now();
    }
    public Instant getUpdatedAt() { return updatedAt; }
}
