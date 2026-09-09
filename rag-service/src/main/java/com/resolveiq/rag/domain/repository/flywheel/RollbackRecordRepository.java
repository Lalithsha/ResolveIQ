package com.resolveiq.rag.domain.repository.flywheel;

import com.resolveiq.rag.domain.model.flywheel.RollbackRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RollbackRecordRepository extends JpaRepository<RollbackRecord, UUID> {

    List<RollbackRecord> findByTenantIdOrderByOccurredAtDesc(UUID tenantId);
}
