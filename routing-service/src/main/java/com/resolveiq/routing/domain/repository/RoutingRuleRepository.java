package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {
    List<RoutingRule> findByTenantIdAndActiveTrueOrderByPriorityOrderAsc(UUID tenantId);
    List<RoutingRule> findByTenantIdOrderByPriorityOrderAsc(UUID tenantId);
    java.util.Optional<RoutingRule> findByIdAndTenantId(UUID id, UUID tenantId);
}
