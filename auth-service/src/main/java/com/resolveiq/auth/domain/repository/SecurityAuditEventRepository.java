package com.resolveiq.auth.domain.repository;

import com.resolveiq.auth.domain.model.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, UUID> {
    List<SecurityAuditEvent> findByTenantIdOrderByOccurredAtDesc(UUID tenantId);
}
