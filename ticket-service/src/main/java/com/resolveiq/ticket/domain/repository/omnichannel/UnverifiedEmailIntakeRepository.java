package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.UnverifiedEmailIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UnverifiedEmailIntakeRepository extends JpaRepository<UnverifiedEmailIntake, UUID> {
    Optional<UnverifiedEmailIntake> findByIdAndTenantId(UUID id, UUID tenantId);
    List<UnverifiedEmailIntake> findByTenantIdAndSenderHmacAndStatus(UUID tenantId, String senderHmac, String status);
    List<UnverifiedEmailIntake> findByTenantIdAndStatusOrderByReceivedAtDesc(UUID tenantId, String status);
}
