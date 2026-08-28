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

        // 1. Lexical search candidates
        List<KnowledgeChunk> kbLexical = knowledgeChunkRepository.findByTenantId(tenantId);
        List<ResolvedCaseChunk> rcLexical = resolvedCaseChunkRepository.findByTenantId(tenantId);

        // 2. Vector search embeddings
        float[] queryEmbedding = embeddingPort.embed(queryText);

        // Score candidates using RRF (Reciprocal Rank Fusion k=60)
        Map<UUID, CandidateScore> scores = new HashMap<>();

        // Score KB chunks
        for (int i = 0; i < kbLexical.size(); i++) {
            KnowledgeChunk chunk = kbLexical.get(i);
            float[] chunkEmbedding = embeddingPort.embed(chunk.getContent());
            double cosineSim = computeCosineSimilarity(queryEmbedding, chunkEmbedding);
            double keywordSim = computeKeywordOverlap(queryText, chunk.getContent());

            double rrfScore = (1.0 / (60.0 + (1.0 - keywordSim) * 100)) + (1.0 / (60.0 + (1.0 - cosineSim) * 100));

            scores.put(chunk.getId(), new CandidateScore(
                "KNOWLEDGE_ARTICLE",
                chunk.getDocumentId(),
                chunk.getVersionId(),
                chunk.getId(),
                chunk.getContent(),
                rrfScore
            ));
        }

        // Score Resolved Case chunks
        for (int i = 0; i < rcLexical.size(); i++) {
            ResolvedCaseChunk chunk = rcLexical.get(i);
            float[] chunkEmbedding = embeddingPort.embed(chunk.getContent());
            double cosineSim = computeCosineSimilarity(queryEmbedding, chunkEmbedding);
            double keywordSim = computeKeywordOverlap(queryText, chunk.getContent());

            double rrfScore = (1.0 / (60.0 + (1.0 - keywordSim) * 100)) + (1.0 / (60.0 + (1.0 - cosineSim) * 100));

            scores.put(chunk.getId(), new CandidateScore(
                "RESOLVED_CASE",
                chunk.getResolvedCaseId(),
                null,
                chunk.getId(),
                chunk.getContent(),
                rrfScore
            ));
        }

        List<CandidateScore> sortedCandidates = scores.values().stream()
            .sorted(Comparator.comparingDouble(CandidateScore::score).reversed())
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
                title = documentRepository.findById(candidate.sourceId()).map(KnowledgeDocument::getTitle).orElse(title);
            } else if ("RESOLVED_CASE".equals(candidate.sourceType())) {
                title = resolvedCaseRepository.findById(candidate.sourceId()).map(ResolvedCase::getSanitizedSubject).orElse(title);
            }

            citations.add(new CitationDto(
                candidate.sourceType(),
                candidate.sourceId(),
                candidate.versionId(),
                candidate.chunkId(),
                title,
                candidate.content(),
                candidate.score()
            ));
        }

        return new RetrievalResultDto(
            run.getId(),
            queryText,
            run.getStrategy(),
            durationMs,
            citations
        );
    }

    private double computeCosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0;
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        double denom = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denom > 0.0 ? Math.max(0.0, dot / denom) : 0.0;
    }

    private double computeKeywordOverlap(String query, String content) {
        if (query == null || content == null) return 0.0;
        Set<String> queryWords = new HashSet<>(Arrays.asList(query.toLowerCase().split("\\s+")));
        Set<String> contentWords = new HashSet<>(Arrays.asList(content.toLowerCase().split("\\s+")));
        queryWords.retainAll(contentWords);
        return (double) queryWords.size() / Math.max(1, queryWords.size() + 2);
    }

    private record CandidateScore(
        String sourceType,
        UUID sourceId,
        UUID versionId,
        UUID chunkId,
        String content,
        double score
    ) {}
}
