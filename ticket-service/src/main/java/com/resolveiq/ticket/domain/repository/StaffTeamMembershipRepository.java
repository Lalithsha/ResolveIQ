package com.resolveiq.ticket.domain.repository;

import com.resolveiq.ticket.domain.model.StaffTeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StaffTeamMembershipRepository extends JpaRepository<StaffTeamMembership, UUID> {
    boolean existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(UUID tenantId, UUID userId, UUID teamId);
}
