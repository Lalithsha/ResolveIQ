package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.ChannelIdentity;
import com.resolveiq.ticket.domain.model.omnichannel.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ChannelIdentityRepository extends JpaRepository<ChannelIdentity, UUID> {
    Optional<ChannelIdentity> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<ChannelIdentity> findByTenantIdAndChannelAndAddressHmac(UUID tenantId, ChannelType channel, String addressHmac);
    List<ChannelIdentity> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
