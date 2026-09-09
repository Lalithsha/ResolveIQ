package com.resolveiq.ticket.application.port;

import com.resolveiq.ticket.domain.model.DeliveryStatus;

import java.util.UUID;

public interface NotificationPort {
    record DeliveryResult(DeliveryStatus status, String providerMessageId, String failureReason) {}

    DeliveryResult sendNotification(
        UUID tenantId,
        UUID updateId,
        UUID recipientCustomerId,
        String channel,
        String title,
        String message,
        String deliveryKey
    );
}
