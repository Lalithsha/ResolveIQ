package com.resolveiq.ticket.application.port.omnichannel;

import com.resolveiq.ticket.domain.model.DeliveryStatus;
import java.time.Instant;

public record DeliveryReceipt(
    String providerMessageId,
    DeliveryStatus status,
    Instant timestamp,
    String errorDetails
) {}
