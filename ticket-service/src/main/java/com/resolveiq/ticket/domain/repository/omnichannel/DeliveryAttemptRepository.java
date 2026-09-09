package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
    List<DeliveryAttempt> findByTenantIdAndMessageIdOrderByAttemptNumberAsc(UUID tenantId, UUID messageId);
}
