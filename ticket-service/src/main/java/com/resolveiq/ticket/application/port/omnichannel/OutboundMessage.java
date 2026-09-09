package com.resolveiq.ticket.application.port.omnichannel;

import java.util.UUID;

public record OutboundMessage(
    UUID tenantId,
    UUID messageId,
    UUID conversationId,
    String recipientAddress,
    String subject,
    String bodyText,
    boolean isInternal
) {
    public OutboundMessage {
        if (isInternal) {
            throw new IllegalArgumentException("Internal notes are prohibited from outbound adapters!");
        }
    }
}
