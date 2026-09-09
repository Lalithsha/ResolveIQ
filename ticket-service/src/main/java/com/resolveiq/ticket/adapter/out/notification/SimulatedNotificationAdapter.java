package com.resolveiq.ticket.adapter.out.notification;

import com.resolveiq.ticket.application.port.NotificationPort;
import com.resolveiq.ticket.domain.model.DeliveryStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedNotificationAdapter implements NotificationPort {

    @Override
    public DeliveryResult sendNotification(
        UUID tenantId,
        UUID updateId,
        UUID recipientCustomerId,
        String channel,
        String title,
        String message,
        String deliveryKey
    ) {
        // Deterministic local adapter simulates instant successful delivery
        String providerMessageId = "sim-msg-" + UUID.nameUUIDFromBytes(deliveryKey.getBytes());
        return new DeliveryResult(DeliveryStatus.SENT, providerMessageId, null);
    }
}
