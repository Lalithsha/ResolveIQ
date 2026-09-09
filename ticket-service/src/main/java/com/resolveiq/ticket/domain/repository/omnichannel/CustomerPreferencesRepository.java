package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.CustomerPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPreferencesRepository extends JpaRepository<CustomerPreferences, UUID> {
    Optional<CustomerPreferences> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
