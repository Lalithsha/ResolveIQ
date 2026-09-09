package com.resolveiq.ticket.application.port.omnichannel;

import java.time.Instant;
import java.util.Map;

public record VerifiedInboundEvent(
    String providerEventId,
    String senderAddress,
    String recipientAddress,
    String subject,
    String bodyText,
    String externalMessageId,
    Instant providerTimestamp,
    Map<String, Object> metadata
) {}
