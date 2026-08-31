package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_attachments", schema = "ticket_schema")
public class TicketAttachment {
    @Id private UUID id;
    @Column(name = "ticket_id", nullable = false) private UUID ticketId;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "uploader_id", nullable = false) private UUID uploaderId;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "storage_key", nullable = false) private String storageKey;
    @Column(name = "scan_status", nullable = false) private String scanStatus;
    @Column(length = 64) private String sha256;
    @Column(name = "scan_engine") private String scanEngine;
    @Column(name = "scan_details") private String scanDetails;
    @Column(name = "scanned_at") private Instant scannedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected TicketAttachment() {}

    public TicketAttachment(UUID ticketId, UUID tenantId, UUID uploaderId, String fileName, String contentType,
                            long sizeBytes, String storageKey, String sha256, String scanEngine, String scanDetails) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.scanStatus = "CLEAN";
        this.sha256 = sha256;
        this.scanEngine = scanEngine;
        this.scanDetails = scanDetails;
        this.scannedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUploaderId() { return uploaderId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public String getScanStatus() { return scanStatus; }
    public String getSha256() { return sha256; }
    public String getScanEngine() { return scanEngine; }
    public String getScanDetails() { return scanDetails; }
    public Instant getScannedAt() { return scannedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
