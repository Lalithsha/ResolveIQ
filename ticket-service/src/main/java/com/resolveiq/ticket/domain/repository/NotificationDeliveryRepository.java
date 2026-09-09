package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.DeliveryStatus;
import com.resolveiq.ticket.domain.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    List<NotificationDelivery> findByTenantIdAndUpdateId(UUID tenantId, UUID updateId);
    Optional<NotificationDelivery> findByTenantIdAndUpdateIdAndRecipientCustomerIdAndChannel(
        UUID tenantId, UUID updateId, UUID recipientCustomerId, String channel
    );
    long countByTenantIdAndUpdateIdAndStatus(UUID tenantId, UUID updateId, DeliveryStatus status);
}
