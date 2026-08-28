package com.resolveiq.rag.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retrieval_runs", schema = "rag_schema")
public class RetrievalRun {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(nullable = false, length = 50)
    private String strategy;

    @Column(name = "top_k", nullable = false)
    private int topK;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RetrievalRun() {}

    public RetrievalRun(UUID ticketId, UUID tenantId, String queryText, String strategy, int topK, long durationMs) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.queryText = queryText;
        this.strategy = strategy;
        this.topK = topK;
        this.durationMs = durationMs;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public String getQueryText() { return queryText; }
    public String getStrategy() { return strategy; }
    public int getTopK() { return topK; }
    public long getDurationMs() { return durationMs; }
    public Instant getCreatedAt() { return createdAt; }
}
