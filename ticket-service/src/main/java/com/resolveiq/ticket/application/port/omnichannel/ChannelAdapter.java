package com.resolveiq.ticket.application.port.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.ChannelType;
import com.resolveiq.ticket.domain.model.DeliveryStatus;

public interface ChannelAdapter {
    ChannelType channel();
    VerifiedInboundEvent verifyAndNormalize(WebhookRequest request);
    DeliveryReceipt send(OutboundMessage message, String idempotencyKey);
    DeliveryStatus fetchStatus(String providerMessageId);
}
