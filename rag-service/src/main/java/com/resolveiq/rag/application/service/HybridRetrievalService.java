package com.resolveiq.rag.application.service;

import com.resolveiq.rag.application.dto.CitationDto;
import com.resolveiq.rag.application.dto.RetrievalResultDto;
import com.resolveiq.rag.application.port.EmbeddingPort;
import com.resolveiq.rag.domain.model.*;
import com.resolveiq.rag.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HybridRetrievalService {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ResolvedCaseChunkRepository resolvedCaseChunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final ResolvedCaseRepository resolvedCaseRepository;
    private final RetrievalRunRepository retrievalRunRepository;
    private final CitationRecordRepository citationRecordRepository;
    private final EmbeddingPort embeddingPort;

    public HybridRetrievalService(
        KnowledgeChunkRepository knowledgeChunkRepository,
        ResolvedCaseChunkRepository resolvedCaseChunkRepository,
        KnowledgeDocumentRepository documentRepository,
        ResolvedCaseRepository resolvedCaseRepository,
        RetrievalRunRepository retrievalRunRepository,
        CitationRecordRepository citationRecordRepository,
        EmbeddingPort embeddingPort
    ) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.resolvedCaseChunkRepository = resolvedCaseChunkRepository;
        this.documentRepository = documentRepository;
        this.resolvedCaseRepository = resolvedCaseRepository;
        this.retrievalRunRepository = retrievalRunRepository;
        this.citationRecordRepository = citationRecordRepository;
        this.embeddingPort = embeddingPort;
    }

    @Transactional
    public RetrievalResultDto searchHybrid(UUID tenantId, UUID ticketId, String queryText, String strategy, int topK) {
        long startTime = System.currentTimeMillis();
        int limit = topK > 0 ? topK : 5;

        // 1. Generate query embedding ONCE
        float[] queryEmbedding = embeddingPort.embed(queryText);
        if (queryEmbedding == null || queryEmbedding.length != embeddingPort.getDimension()) {
            throw new IllegalStateException("Query embedding dimension does not match configured provider");
        }
        for (float value : queryEmbedding) {
            if (!Float.isFinite(value)) throw new IllegalStateException("Query embedding contains non-finite values");
        }
        String embeddingStr = formatVector(queryEmbedding);

        // 2. Fetch candidates using Lexical & Vector SQL queries
        List<KnowledgeChunk> kbLexical = new ArrayList<>();
        List<KnowledgeChunk> kbVector = new ArrayList<>();
        List<ResolvedCaseChunk> rcLexical = new ArrayList<>();
        List<ResolvedCaseChunk> rcVector = new ArrayList<>();

        kbLexical = knowledgeChunkRepository.searchLexical(tenantId, queryText, 50);
        kbVector = knowledgeChunkRepository.searchVector(tenantId, embeddingStr, 50);
        rcLexical = resolvedCaseChunkRepository.searchLexical(tenantId, queryText, 30);
        rcVector = resolvedCaseChunkRepository.searchVector(tenantId, embeddingStr, 30);

        // 3. Reciprocal Rank Fusion (RRF k=60)
        Map<UUID, CandidateScore> fusedCandidates = new HashMap<>();

        // Lexical ranking (RRF score = 1 / (60 + rank))
        for (int rank = 0; rank < kbLexical.size(); rank++) {
            KnowledgeChunk chunk = kbLexical.get(rank);
            double rrf = 1.0 / (60.0 + rank + 1);
            fusedCandidates.compute(chunk.getId(), (k, v) -> v == null
                ? new CandidateScore("KNOWLEDGE_ARTICLE", chunk.getDocumentId(), chunk.getVersionId(), chunk.getId(), chunk.getContent(), rrf)
                : v.addScore(rrf));
        }

        for (int rank = 0; rank < rcLexical.size(); rank++) {
            ResolvedCaseChunk chunk = rcLexical.get(rank);
            double rrf = 1.0 / (60.0 + rank + 1);
            fusedCandidates.compute(chunk.getId(), (k, v) -> v == null
                ? new CandidateScore("RESOLVED_CASE", chunk.getResolvedCaseId(), null, chunk.getId(), chunk.getContent(), rrf)
                : v.addScore(rrf));
        }

        // Vector ranking (RRF score = 1 / (60 + rank))
        for (int rank = 0; rank < kbVector.size(); rank++) {
            KnowledgeChunk chunk = kbVector.get(rank);
            double rrf = 1.0 / (60.0 + rank + 1);
            fusedCandidates.compute(chunk.getId(), (k, v) -> v == null
                ? new CandidateScore("KNOWLEDGE_ARTICLE", chunk.getDocumentId(), chunk.getVersionId(), chunk.getId(), chunk.getContent(), rrf)
                : v.addScore(rrf));
        }

        for (int rank = 0; rank < rcVector.size(); rank++) {
            ResolvedCaseChunk chunk = rcVector.get(rank);
            double rrf = 1.0 / (60.0 + rank + 1);
            fusedCandidates.compute(chunk.getId(), (k, v) -> v == null
                ? new CandidateScore("RESOLVED_CASE", chunk.getResolvedCaseId(), null, chunk.getId(), chunk.getContent(), rrf)
                : v.addScore(rrf));
        }

        List<CandidateScore> sortedCandidates = fusedCandidates.values().stream()
            .sorted(Comparator.comparingDouble(CandidateScore::score).reversed().thenComparing(CandidateScore::chunkId))
            .limit(limit)
            .toList();

        long durationMs = System.currentTimeMillis() - startTime;

        RetrievalRun run = new RetrievalRun(
            ticketId != null ? ticketId : UUID.randomUUID(),
            tenantId,
            queryText,
            strategy != null ? strategy : "HYBRID_RRF",
            limit,
            durationMs
        );
        retrievalRunRepository.save(run);

        List<CitationDto> citations = new ArrayList<>();
        for (CandidateScore candidate : sortedCandidates) {
            String title = "Resource " + candidate.sourceId().toString().substring(0, 8);
            if ("KNOWLEDGE_ARTICLE".equals(candidate.sourceType())) {
                title = documentRepository.findByIdAndTenantId(candidate.sourceId(), tenantId).map(KnowledgeDocument::getTitle).orElse(title);
            } else if ("RESOLVED_CASE".equals(candidate.sourceType())) {
                title = resolvedCaseRepository.findByIdAndTenantId(candidate.sourceId(), tenantId).map(ResolvedCase::getSanitizedSubject).orElse(title);
            }

            String snippet = candidate.content().length() > 200 ? candidate.content().substring(0, 200) + "..." : candidate.content();

            CitationRecord citationRecord = new CitationRecord(
                UUID.randomUUID(),
                run.getId(),
                candidate.sourceType(),
                candidate.sourceId(),
                candidate.versionId(),
                candidate.chunkId(),
                snippet,
                candidate.score()
            );
            citationRecordRepository.save(citationRecord);

            citations.add(new CitationDto(
                candidate.sourceType(),
                candidate.sourceId(),
                candidate.versionId(),
                candidate.chunkId(),
                title,
                snippet,
                candidate.score()
            ));
        }

        return new RetrievalResultDto(
            run.getId(),
            queryText,
            strategy != null ? strategy : "HYBRID_RRF",
            durationMs,
            citations
        );
    }

    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private record CandidateScore(
        String sourceType,
        UUID sourceId,
        UUID versionId,
        UUID chunkId,
        String content,
        double score
    ) {
        CandidateScore addScore(double additionalScore) {
            return new CandidateScore(sourceType, sourceId, versionId, chunkId, content, this.score + additionalScore);
        }
    }
}
