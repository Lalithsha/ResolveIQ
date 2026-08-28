package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resolved_cases", schema = "rag_schema")
public class ResolvedCase {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "original_ticket_id", nullable = false)
    private UUID originalTicketId;

    @Column(name = "sanitized_subject", nullable = false, length = 500)
    private String sanitizedSubject;

    @Column(name = "sanitized_description", nullable = false, columnDefinition = "TEXT")
    private String sanitizedDescription;

    @Column(name = "sanitized_resolution", nullable = false, columnDefinition = "TEXT")
    private String sanitizedResolution;

    @Column(length = 100)
    private String category;

    @Column(name = "approved_by_user_id", nullable = false)
    private UUID approvedByUserId;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    public ResolvedCase() {}

    public ResolvedCase(
        UUID id,
        UUID tenantId,
        UUID originalTicketId,
        String sanitizedSubject,
        String sanitizedDescription,
        String sanitizedResolution,
        String category,
        UUID approvedByUserId
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.originalTicketId = originalTicketId;
        this.sanitizedSubject = sanitizedSubject;
        this.sanitizedDescription = sanitizedDescription;
        this.sanitizedResolution = sanitizedResolution;
        this.category = category;
        this.approvedByUserId = approvedByUserId;
        this.approvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getOriginalTicketId() { return originalTicketId; }
    public String getSanitizedSubject() { return sanitizedSubject; }
    public String getSanitizedDescription() { return sanitizedDescription; }
    public String getSanitizedResolution() { return sanitizedResolution; }
    public String getCategory() { return category; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public Instant getApprovedAt() { return approvedAt; }
}
