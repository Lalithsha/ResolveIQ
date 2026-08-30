package com.resolveiq.auth.domain.repository;

import com.resolveiq.auth.domain.model.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, UUID> {
    List<SecurityAuditEvent> findByTenantIdOrderByOccurredAtDesc(UUID tenantId);
}
