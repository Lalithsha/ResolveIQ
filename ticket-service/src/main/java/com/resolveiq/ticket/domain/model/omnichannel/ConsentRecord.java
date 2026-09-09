package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records", schema = "ticket_schema")
public class ConsentRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private ChannelType channel;

    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose;

    @Column(name = "consented", nullable = false)
    private boolean consented;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "ip_or_source", nullable = false, length = 100)
    private String ipOrSource;

    public ConsentRecord() {}

    public ConsentRecord(UUID tenantId, UUID customerId, ChannelType channel, String purpose, boolean consented, String ipOrSource) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.channel = channel;
        this.purpose = purpose;
        this.consented = consented;
        this.recordedAt = Instant.now();
        this.ipOrSource = ipOrSource;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCustomerId() { return customerId; }
    public ChannelType getChannel() { return channel; }
    public String getPurpose() { return purpose; }
    public boolean isConsented() { return consented; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getIpOrSource() { return ipOrSource; }
}
