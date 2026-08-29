package com.resolveiq.analysis.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_results", schema = "analysis_schema")
public class AnalysisResult {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String intent;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String sentiment;

    @Column(name = "sentiment_confidence")
    private Double sentimentConfidence;

    @Column(nullable = false, length = 50)
    private String urgency;

    @Column(name = "urgency_confidence")
    private Double urgencyConfidence;

    @Column(nullable = false, length = 10)
    private String language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redacted_entities", columnDefinition = "JSONB")
    private String redactedEntities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_flags", columnDefinition = "JSONB")
    private String policyFlags;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "raw_output_hash", nullable = false)
    private String rawOutputHash;

    @Column(name = "validation_outcome", nullable = false, length = 50)
    private String validationOutcome;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "tokens_used", nullable = false)
    private int tokensUsed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AnalysisResult() {}

    public AnalysisResult(
        UUID ticketId,
        UUID tenantId,
        String intent,
        String category,
        String sentiment,
        Double sentimentConfidence,
        String urgency,
        Double urgencyConfidence,
        String language,
        String redactedEntities,
        String policyFlags,
        String modelName,
        String promptVersion,
        String rawOutputHash,
        String validationOutcome,
        long latencyMs,
        int tokensUsed
    ) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.intent = intent;
        this.category = category;
        this.sentiment = sentiment;
        this.sentimentConfidence = sentimentConfidence;
        this.urgency = urgency;
        this.urgencyConfidence = urgencyConfidence;
        this.language = language != null ? language : "en";
        this.redactedEntities = redactedEntities;
        this.policyFlags = policyFlags;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.rawOutputHash = rawOutputHash;
        this.validationOutcome = validationOutcome;
        this.latencyMs = latencyMs;
        this.tokensUsed = tokensUsed;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public String getIntent() { return intent; }
    public String getCategory() { return category; }
    public String getSentiment() { return sentiment; }
    public Double getSentimentConfidence() { return sentimentConfidence; }
    public String getUrgency() { return urgency; }
    public Double getUrgencyConfidence() { return urgencyConfidence; }
    public String getLanguage() { return language; }
    public String getRedactedEntities() { return redactedEntities; }
    public String getPolicyFlags() { return policyFlags; }
    public String getModelName() { return modelName; }
    public String getPromptVersion() { return promptVersion; }
    public String getRawOutputHash() { return rawOutputHash; }
    public String getValidationOutcome() { return validationOutcome; }
    public long getLatencyMs() { return latencyMs; }
    public int getTokensUsed() { return tokensUsed; }
    public Instant getCreatedAt() { return createdAt; }
}
