package com.resolveiq.ticket.domain.model.omnichannel;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants", schema = "ticket_schema")
public class ConversationParticipant {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private ParticipantRole role;

    @Column(name = "channel_identity_id")
    private UUID channelIdentityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_state", nullable = false, length = 50)
    private VerificationState verificationState;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    public ConversationParticipant() {}

    public ConversationParticipant(UUID tenantId, UUID conversationId, UUID userId, ParticipantRole role, UUID channelIdentityId, VerificationState verificationState) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.channelIdentityId = channelIdentityId;
        this.verificationState = verificationState;
        this.joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getConversationId() { return conversationId; }
    public UUID getUserId() { return userId; }
    public ParticipantRole getRole() { return role; }
    public UUID getChannelIdentityId() { return channelIdentityId; }
    public VerificationState getVerificationState() { return verificationState; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public void leave() { this.leftAt = Instant.now(); }
}
