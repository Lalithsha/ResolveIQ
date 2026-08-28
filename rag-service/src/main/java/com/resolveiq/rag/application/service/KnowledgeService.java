package com.resolveiq.rag.application.service;

import com.resolveiq.rag.application.dto.*;
import com.resolveiq.rag.application.port.EmbeddingPort;
import com.resolveiq.rag.domain.model.*;
import com.resolveiq.rag.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class KnowledgeService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeVersionRepository versionRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final ResolvedCaseRepository resolvedCaseRepository;
    private final ResolvedCaseChunkRepository resolvedCaseChunkRepository;
    private final EmbeddingPort embeddingPort;

    public KnowledgeService(
        KnowledgeDocumentRepository documentRepository,
        KnowledgeVersionRepository versionRepository,
        KnowledgeChunkRepository chunkRepository,
        ResolvedCaseRepository resolvedCaseRepository,
        ResolvedCaseChunkRepository resolvedCaseChunkRepository,
        EmbeddingPort embeddingPort
    ) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.chunkRepository = chunkRepository;
        this.resolvedCaseRepository = resolvedCaseRepository;
        this.resolvedCaseChunkRepository = resolvedCaseChunkRepository;
        this.embeddingPort = embeddingPort;
    }

    @Transactional
    public KnowledgeDocument createDocument(UUID tenantId, UUID userId, CreateDocumentRequest request) {
        KnowledgeDocument document = new KnowledgeDocument(
            UUID.randomUUID(),
            tenantId,
            request.title(),
            request.category(),
            request.product(),
            request.language()
        );
        documentRepository.save(document);

        KnowledgeVersion version = new KnowledgeVersion(
            document.getId(),
            1,
            request.content(),
            request.summary(),
            userId
        );
        versionRepository.save(version);

        // Chunk and embed
        List<String> chunks = chunkText(request.content(), 600);
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            String hash = computeHash(chunkContent);
            KnowledgeChunk chunk = new KnowledgeChunk(
                tenantId,
                document.getId(),
                version.getId(),
                i,
                chunkContent,
                hash,
                embeddingPort.getModelName()
            );
            chunkRepository.save(chunk);
        }

        document.publishVersion(version.getId());
        return documentRepository.save(document);
    }

    @Transactional
    public KnowledgeDocument publishNewVersion(UUID tenantId, UUID documentId, UUID userId, PublishVersionRequest request) {
        KnowledgeDocument document = documentRepository.findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));

        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
            .map(v -> v.getVersionNumber() + 1)
            .orElse(1);

        KnowledgeVersion version = new KnowledgeVersion(
            document.getId(),
            nextVersion,
            request.content(),
            request.summary(),
            userId
        );
        versionRepository.save(version);

        List<String> chunks = chunkText(request.content(), 600);
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            String hash = computeHash(chunkContent);
            KnowledgeChunk chunk = new KnowledgeChunk(
                tenantId,
                document.getId(),
                version.getId(),
                i,
                chunkContent,
                hash,
                embeddingPort.getModelName()
            );
            chunkRepository.save(chunk);
        }

        document.publishVersion(version.getId());
        return documentRepository.save(document);
    }

    @Transactional
    public ResolvedCase approveResolvedCase(UUID tenantId, UUID userId, ApproveResolvedCaseRequest request) {
        ResolvedCase resolvedCase = new ResolvedCase(
            UUID.randomUUID(),
            tenantId,
            request.originalTicketId(),
            request.sanitizedSubject(),
            request.sanitizedDescription(),
            request.sanitizedResolution(),
            request.category(),
            userId
        );
        resolvedCaseRepository.save(resolvedCase);

        String combinedContent = request.sanitizedSubject() + "\n" + request.sanitizedDescription() + "\n" + request.sanitizedResolution();
        List<String> chunks = chunkText(combinedContent, 600);
        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);
            String hash = computeHash(chunkContent);
            ResolvedCaseChunk chunk = new ResolvedCaseChunk(
                tenantId,
                resolvedCase.getId(),
                i,
                chunkContent,
                hash,
                embeddingPort.getModelName()
            );
            resolvedCaseChunkRepository.save(chunk);
        }

        return resolvedCase;
    }

    @Transactional(readOnly = true)
    public KnowledgeDocument getDocument(UUID tenantId, UUID documentId) {
        return documentRepository.findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> listDocuments(UUID tenantId) {
        return documentRepository.findByTenantId(tenantId);
    }

    private List<String> chunkText(String text, int targetChars) {
        if (text == null || text.isBlank()) return List.of();
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String p : paragraphs) {
            if (currentChunk.length() + p.length() > targetChars && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(p).append("\n\n");
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }
}
