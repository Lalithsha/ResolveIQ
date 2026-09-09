package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.ConversationMergeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ConversationMergeRecordRepository extends JpaRepository<ConversationMergeRecord, UUID> {
    Optional<ConversationMergeRecord> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ConversationMergeRecord> findByTenantIdAndTargetConversationIdOrderByCreatedAtDesc(UUID tenantId, UUID targetConversationId);
}
