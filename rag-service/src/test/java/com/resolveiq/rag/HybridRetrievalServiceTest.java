package com.resolveiq.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.resolveiq.rag.adapter.out.ai.MockEmbeddingAdapter;
import com.resolveiq.rag.application.dto.CitationDto;
import com.resolveiq.rag.application.dto.RetrievalResultDto;
import com.resolveiq.rag.application.service.HybridRetrievalService;
import com.resolveiq.rag.domain.model.KnowledgeChunk;
import com.resolveiq.rag.domain.model.KnowledgeDocument;
import com.resolveiq.rag.domain.model.ResolvedCase;
import com.resolveiq.rag.domain.model.ResolvedCaseChunk;
import com.resolveiq.rag.domain.model.RetrievalRun;
import com.resolveiq.rag.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class HybridRetrievalServiceTest {

    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private ResolvedCaseChunkRepository resolvedCaseChunkRepository;
    @Mock
    private KnowledgeDocumentRepository documentRepository;
    @Mock
    private ResolvedCaseRepository resolvedCaseRepository;
    @Mock
    private RetrievalRunRepository retrievalRunRepository;
    @Mock
    private CitationRecordRepository citationRecordRepository;

    private MockEmbeddingAdapter embeddingAdapter;
    private HybridRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        embeddingAdapter = new MockEmbeddingAdapter("mock-embedding-v1", 1536);
        retrievalService = new HybridRetrievalService(
            knowledgeChunkRepository,
            resolvedCaseChunkRepository,
            documentRepository,
            resolvedCaseRepository,
            retrievalRunRepository,
            citationRecordRepository,
            embeddingAdapter
        );
    }

    @Test
    @DisplayName("Should retrieve relevant citations using hybrid lexical and vector scoring")
    void testHybridRetrieval() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        KnowledgeDocument doc = new KnowledgeDocument(docId, tenantId, "Payment Reconciliation", "BILLING", "Core", "en");
        when(documentRepository.findByIdAndTenantId(docId, tenantId)).thenReturn(Optional.of(doc));

        KnowledgeChunk chunk1 = new KnowledgeChunk(
            tenantId,
            docId,
            versionId,
            0,
            "When duplicate charge occurs, check gateway status and reconcile balance.",
            "hash1",
            "mock-embedding-v1"
        );

        ResolvedCase resolvedCase = new ResolvedCase(
            caseId,
            tenantId,
            UUID.randomUUID(),
            "Refund for duplicate charge",
            "Customer had double billing",
            "Manually voided extra transaction",
            "BILLING",
            UUID.randomUUID()
        );
        when(resolvedCaseRepository.findByIdAndTenantId(caseId, tenantId)).thenReturn(Optional.of(resolvedCase));

        ResolvedCaseChunk caseChunk = new ResolvedCaseChunk(
            tenantId,
            caseId,
            0,
            "Resolved duplicate billing by voiding extra card authorization.",
            "hash2",
            "mock-embedding-v1"
        );

        when(knowledgeChunkRepository.searchLexical(tenantId, "duplicate charge on card", 50)).thenReturn(List.of(chunk1));
        when(knowledgeChunkRepository.searchVector(eq(tenantId), anyString(), eq(50))).thenReturn(List.of(chunk1));
        when(resolvedCaseChunkRepository.searchLexical(tenantId, "duplicate charge on card", 30)).thenReturn(List.of(caseChunk));
        when(resolvedCaseChunkRepository.searchVector(eq(tenantId), anyString(), eq(30))).thenReturn(List.of(caseChunk));

        RetrievalResultDto result = retrievalService.searchHybrid(
            tenantId,
            ticketId,
            "duplicate charge on card",
            "HYBRID_RRF",
            5
        );

        assertThat(result).isNotNull();
        assertThat(result.queryText()).isEqualTo("duplicate charge on card");
        assertThat(result.citations()).hasSize(2);

        CitationDto topCitation = result.citations().get(0);
        assertThat(topCitation.citationText()).isNotEmpty();
        assertThat(topCitation.score()).isGreaterThan(0.0);

        verify(retrievalRunRepository, times(1)).save(any(RetrievalRun.class));
    }
}
