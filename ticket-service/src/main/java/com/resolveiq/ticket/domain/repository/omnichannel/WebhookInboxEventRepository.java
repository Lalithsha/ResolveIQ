package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.WebhookInboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookInboxEventRepository extends JpaRepository<WebhookInboxEvent, UUID> {
    boolean existsByTenantIdAndProviderAndExternalEventId(UUID tenantId, String provider, String externalEventId);
    Optional<WebhookInboxEvent> findByTenantIdAndProviderAndExternalEventId(UUID tenantId, String provider, String externalEventId);
}
