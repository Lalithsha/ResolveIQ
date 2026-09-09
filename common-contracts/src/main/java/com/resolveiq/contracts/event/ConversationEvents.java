package com.resolveiq.contracts.event;

import java.time.Instant;
import java.util.UUID;

public final class ConversationEvents {

    private ConversationEvents() {}

    public static final String CONVERSATION_MESSAGE_RECEIVED = "resolveiq.conversation.message_received";

    public record ConversationMessageReceivedPayload(
        UUID conversationId,
        UUID messageId,
        UUID ticketId,
        UUID tenantId,
        String channel, // PORTAL, EMAIL
        String direction, // INBOUND, OUTBOUND
        String senderIdentity,
        String content,
        String externalMessageId,
        Instant occurredAt
    ) {}
}
