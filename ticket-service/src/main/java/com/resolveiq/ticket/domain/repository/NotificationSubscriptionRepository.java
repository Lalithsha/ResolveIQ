package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, UUID> {
    List<NotificationSubscription> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);
    List<NotificationSubscription> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Optional<NotificationSubscription> findByTenantIdAndIncidentIdAndCustomerIdAndChannel(UUID tenantId, UUID incidentId, UUID customerId, String channel);
}
