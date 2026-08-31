package com.resolveiq.rag;

import com.resolveiq.rag.application.dto.CreateDocumentRequest;
import com.resolveiq.rag.application.dto.CreateVersionRequest;
import com.resolveiq.rag.application.dto.ReindexKnowledgeResponse;
import com.resolveiq.rag.application.dto.RetrievalResultDto;
import com.resolveiq.rag.application.service.HybridRetrievalService;
import com.resolveiq.rag.application.service.KnowledgeIndexingService;
import com.resolveiq.rag.application.service.KnowledgeService;
import com.resolveiq.rag.domain.model.KnowledgeDocument;
import com.resolveiq.rag.domain.model.KnowledgeVersion;
import com.resolveiq.rag.domain.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "resolveiq.ai.embedding-provider=deterministic",
    "resolveiq.ai.embedding-dimension=1536"
})
class KnowledgeLifecycleIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("resolveiq")
        .withUsername("resolveiq_test")
        .withPassword("resolveiq_test_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired KnowledgeService knowledge;
    @Autowired KnowledgeIndexingService indexing;
    @Autowired HybridRetrievalService retrieval;
    @Autowired KnowledgeChunkRepository chunks;
    @Autowired JdbcTemplate jdbc;

    @Test
    void publishesOnlyApprovedActiveVersionsSupportsRepairRollbackMetadataAndTenantIsolation() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        KnowledgeDocument document = knowledge.createDocument(tenantId, authorId, new CreateDocumentRequest(
            "Payment reconciliation", "BILLING", "Billing Core", "en",
            "When a duplicate charge appears, compare processor transaction identifiers and authorization windows.",
            "Safe duplicate-payment reconciliation"
        ));
        KnowledgeVersion versionOne = knowledge.listVersions(tenantId, document.getId()).getFirst();

        assertThat(search(tenantId, "duplicate charge transaction", "BILLING", "Billing Core").citations()).isEmpty();
        knowledge.submitForReview(tenantId, document.getId(), versionOne.getId());
        knowledge.publishVersion(tenantId, document.getId(), versionOne.getId(), reviewerId, "Approved");

        RetrievalResultDto firstResult = search(tenantId,
            "duplicate charge invoice billing dispute credit card", "BILLING", "Billing Core");
        assertThat(firstResult.citations()).isNotEmpty();
        assertThat(firstResult.citations()).allMatch(citation -> versionOne.getId().equals(citation.versionId()));
        assertThat(search(otherTenantId, "duplicate charge", "BILLING", "Billing Core").citations()).isEmpty();
        assertThat(search(tenantId, "duplicate charge", "TECHNICAL", "Billing Core").citations()).isEmpty();

        jdbc.update("UPDATE rag_schema.knowledge_chunks SET embedding = NULL WHERE version_id = ?", versionOne.getId());
        assertThat(chunks.countInvalidIndexRows(tenantId, versionOne.getId(), 1536,
            "resolveiq-deterministic-embedding-v1")).isPositive();
        ReindexKnowledgeResponse repaired = indexing.reindexMissing(tenantId);
        assertThat(repaired.versionsReindexed()).isEqualTo(1);
        assertThat(chunks.countInvalidIndexRows(tenantId, versionOne.getId(), 1536,
            "resolveiq-deterministic-embedding-v1")).isZero();

        KnowledgeVersion versionTwo = knowledge.createVersion(tenantId, document.getId(), authorId,
            new CreateVersionRequest("For a pending bank authorization, explain the release window without creating a refund.",
                "Pending authorization guidance"));
        knowledge.submitForReview(tenantId, document.getId(), versionTwo.getId());
        knowledge.publishVersion(tenantId, document.getId(), versionTwo.getId(), reviewerId, "Approved replacement");
        assertThat(search(tenantId, "pending authorization release", "BILLING", "Billing Core").citations())
            .allMatch(citation -> versionTwo.getId().equals(citation.versionId()));

        knowledge.rollback(tenantId, document.getId(), versionOne.getId(), reviewerId, "Regression rollback");
        assertThat(search(tenantId, "duplicate charge transaction", "BILLING", "Billing Core").citations())
            .allMatch(citation -> versionOne.getId().equals(citation.versionId()));

        knowledge.archive(tenantId, document.getId());
        assertThat(search(tenantId, "duplicate charge transaction", "BILLING", "Billing Core").citations()).isEmpty();
    }

    private RetrievalResultDto search(UUID tenantId, String query, String category, String product) {
        return retrieval.searchHybrid(tenantId, null, query, "HYBRID_RRF", 5,
            category, product, "en", Set.of("KNOWLEDGE_ARTICLE"));
    }
}
