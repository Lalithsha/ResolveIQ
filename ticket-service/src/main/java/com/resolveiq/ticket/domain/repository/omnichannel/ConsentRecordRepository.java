package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.ChannelType;
import com.resolveiq.ticket.domain.model.omnichannel.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    List<ConsentRecord> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Optional<ConsentRecord> findFirstByTenantIdAndCustomerIdAndChannelAndPurposeOrderByRecordedAtDesc(
        UUID tenantId, UUID customerId, ChannelType channel, String purpose
    );
}
