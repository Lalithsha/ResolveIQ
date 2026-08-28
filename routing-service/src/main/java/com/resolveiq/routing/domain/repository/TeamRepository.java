package com.resolveiq.routing.domain.repository;

import com.resolveiq.routing.domain.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByTenantId(UUID tenantId);
}
