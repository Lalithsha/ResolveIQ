package com.resolveiq.ticket.application.dto.omnichannel;

import com.resolveiq.ticket.domain.model.DeliveryStatus;
import com.resolveiq.ticket.domain.model.omnichannel.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OmnichannelDtos {

    public record EmailChallengeRequest(
        String email
    ) {}

    public record EmailChallengeResponse(
        String email,
        Instant expiresAt,
        String message
    ) {}

    public record EmailVerifyRequest(
        String email,
        String token
    ) {}

    public record EmailVerifyResponse(
        UUID channelIdentityId,
        String email,
        boolean isVerified,
        int linkedPendingIntakes,
        String message
    ) {}

    public record ChannelIdentityResponse(
        UUID id,
        UUID customerId,
        ChannelType channel,
        String displayAddress,
        boolean isVerified,
        Instant verifiedAt,
        double confidence
    ) {}

    public record ChannelPreferenceUpdateRequest(
        ChannelType preferredChannel,
        Boolean emailNotificationsEnabled,
        Boolean marketingConsent
    ) {}

    public record CustomerPreferencesResponse(
        UUID customerId,
        ChannelType preferredChannel,
        boolean emailNotificationsEnabled,
        boolean marketingConsent,
        Instant updatedAt
    ) {}

    public record ConsentUpdateRequest(
        ChannelType channel,
        String purpose,
        boolean consented
    ) {}

    public record AddConversationMessageRequest(
        String content,
        boolean isInternal,
        ChannelType channel,
        String subject,
        String idempotencyKey
    ) {}

    public record TimelineMessageItem(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String senderRole,
        String content,
        boolean isInternal,
        ChannelType channel,
        ChannelDirection direction,
        DeliveryStatus deliveryStatus,
        String senderAddress,
        String recipientAddress,
        Instant createdAt
    ) {}

    public record TimelineResponse(
        UUID conversationId,
        UUID ticketId,
        ConversationStatus status,
        HandoffState handoffState,
        ChannelType preferredChannel,
        List<TimelineMessageItem> messages,
        HandoffSummaryResponse latestHandoff
    ) {}

    public record ConversationResponse(
        UUID id,
        UUID tenantId,
        UUID ticketId,
        ConversationStatus status,
        UUID primaryCustomerId,
        UUID assignedAgentId,
        HandoffState handoffState,
        Instant handoffRequestedAt,
        ChannelType preferredChannel,
        Long version,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record HandoffRequest(
        String reason
    ) {}

    public record HandoffAssignRequest(
        UUID agentId
    ) {}

    public record HandoffSummaryResponse(
        UUID id,
        UUID conversationId,
        UUID ticketId,
        String issueSummary,
        String verifiedFacts,
        String attemptedSteps,
        String promisedActions,
        String sentiment,
        String openQuestions,
        Instant createdAt
    ) {}

    public record HandoffResponse(
        UUID conversationId,
        HandoffState state,
        HandoffSummaryResponse summary,
        String message
    ) {}

    public record MergeConversationRequest(
        UUID targetConversationId,
        String reason
    ) {}

    public record SplitConversationRequest(
        UUID mergeRecordId
    ) {}

    public record InboundWebhookResult(
        boolean isDuplicate,
        String status,
        String message,
        UUID conversationId,
        UUID messageId,
        UUID pendingIntakeId
    ) {}
}
