package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {
    List<RoutingRule> findByTenantIdAndActiveTrueOrderByPriorityOrderAsc(UUID tenantId);
}
