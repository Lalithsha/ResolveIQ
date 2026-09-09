package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_merge_records", schema = "ticket_schema")
public class ConversationMergeRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_conversation_id", nullable = false)
    private UUID sourceConversationId;

    @Column(name = "target_conversation_id", nullable = false)
    private UUID targetConversationId;

    @Column(name = "merged_by_user_id", nullable = false)
    private UUID mergedByUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "split_at")
    private Instant splitAt;

    @Column(name = "split_by_user_id")
    private UUID splitByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ConversationMergeRecord() {}

    public ConversationMergeRecord(UUID tenantId, UUID sourceConversationId, UUID targetConversationId, UUID mergedByUserId, String reason) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.sourceConversationId = sourceConversationId;
        this.targetConversationId = targetConversationId;
        this.mergedByUserId = mergedByUserId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSourceConversationId() { return sourceConversationId; }
    public UUID getTargetConversationId() { return targetConversationId; }
    public UUID getMergedByUserId() { return mergedByUserId; }
    public String getReason() { return reason; }
    public Instant getSplitAt() { return splitAt; }
    public UUID getSplitByUserId() { return splitByUserId; }
    public Instant getCreatedAt() { return createdAt; }

    public void split(UUID splitBy) {
        this.splitAt = Instant.now();
        this.splitByUserId = splitBy;
    }
}
