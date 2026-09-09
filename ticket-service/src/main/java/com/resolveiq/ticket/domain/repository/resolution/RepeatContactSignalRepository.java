package com.resolveiq.ticket.domain.repository.resolution;

import com.resolveiq.ticket.domain.model.resolution.RepeatContactSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepeatContactSignalRepository extends JpaRepository<RepeatContactSignal, UUID> {

    List<RepeatContactSignal> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    Optional<RepeatContactSignal> findByTenantIdAndCurrentTicketId(UUID tenantId, UUID currentTicketId);

    long countByTenantId(UUID tenantId);
}
