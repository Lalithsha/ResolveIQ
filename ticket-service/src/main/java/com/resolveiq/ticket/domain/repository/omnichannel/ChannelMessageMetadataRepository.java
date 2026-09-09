package com.resolveiq.ticket.domain.repository.omnichannel;

import com.resolveiq.ticket.domain.model.omnichannel.ChannelMessageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ChannelMessageMetadataRepository extends JpaRepository<ChannelMessageMetadata, UUID> {
    Optional<ChannelMessageMetadata> findByMessageIdAndTenantId(UUID messageId, UUID tenantId);
    List<ChannelMessageMetadata> findByTenantIdAndConversationIdOrderByCreatedAtAsc(UUID tenantId, UUID conversationId);
    Optional<ChannelMessageMetadata> findByTenantIdAndExternalMessageId(UUID tenantId, String externalMessageId);
}
