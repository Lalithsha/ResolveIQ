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
    private final KnowledgeIndexingService indexingService;
    private final KnowledgePublicationService publicationService;

    public KnowledgeService(
        KnowledgeDocumentRepository documentRepository,
        KnowledgeVersionRepository versionRepository,
        KnowledgeChunkRepository chunkRepository,
        ResolvedCaseRepository resolvedCaseRepository,
        ResolvedCaseChunkRepository resolvedCaseChunkRepository,
        EmbeddingPort embeddingPort,
        KnowledgeIndexingService indexingService,
        KnowledgePublicationService publicationService
    ) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.chunkRepository = chunkRepository;
        this.resolvedCaseRepository = resolvedCaseRepository;
        this.resolvedCaseChunkRepository = resolvedCaseChunkRepository;
        this.embeddingPort = embeddingPort;
        this.indexingService = indexingService;
        this.publicationService = publicationService;
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

        return document;
    }

    @Transactional
    public KnowledgeVersion createVersion(UUID tenantId, UUID documentId, UUID userId, CreateVersionRequest request) {
        KnowledgeDocument document = documentRepository.findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
        if ("ARCHIVED".equals(document.getStatus())) throw new IllegalStateException("Archived knowledge cannot be edited");
        if (versionRepository.existsByDocumentIdAndStatusIn(documentId, List.of("DRAFT", "IN_REVIEW"))) {
            throw new IllegalStateException("Finish the existing draft or review before creating another version");
        }

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
        return versionRepository.save(version);
    }

    @Transactional
    public KnowledgeVersion submitForReview(UUID tenantId, UUID documentId, UUID versionId) {
        KnowledgeDocument document = requireDocument(tenantId, documentId);
        KnowledgeVersion version = requireVersion(documentId, versionId);
        version.submitForReview();
        versionRepository.save(version);
        document.markInReview();
        documentRepository.save(document);
        return version;
    }

    @Transactional
    public KnowledgeVersion rejectVersion(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note) {
        KnowledgeDocument document = requireDocument(tenantId, documentId);
        KnowledgeVersion version = requireVersion(documentId, versionId);
        version.reject(reviewerId, note);
        versionRepository.save(version);
        document.markDraft();
        documentRepository.save(document);
        return version;
    }

    public KnowledgeDocument publishVersion(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note) {
        KnowledgeVersion version = requireVersion(documentId, versionId);
        if (!"IN_REVIEW".equals(version.getStatus())) {
            throw new IllegalStateException("Only an in-review version can be published");
        }
        indexingService.index(tenantId, documentId, versionId);
        return publicationService.activate(tenantId, documentId, versionId, reviewerId, note);
    }

    public KnowledgeDocument rollback(UUID tenantId, UUID documentId, UUID versionId, UUID reviewerId, String note) {
        KnowledgeVersion version = requireVersion(documentId, versionId);
        if (!Set.of("PUBLISHED", "SUPERSEDED").contains(version.getStatus())) {
            throw new IllegalStateException("Only a previously published version can be restored");
        }
        indexingService.index(tenantId, documentId, versionId);
        return publicationService.rollback(tenantId, documentId, versionId, reviewerId, note);
    }

    @Transactional
    public KnowledgeDocument archive(UUID tenantId, UUID documentId) {
        KnowledgeDocument document = requireDocument(tenantId, documentId);
        document.archive();
        return documentRepository.save(document);
    }

    public KnowledgeDocument publishNewVersion(UUID tenantId, UUID documentId, UUID userId, PublishVersionRequest request) {
        KnowledgeVersion version = createVersion(
            tenantId, documentId, userId, new CreateVersionRequest(request.content(), request.summary()));
        submitForReview(tenantId, documentId, version.getId());
        return publishVersion(tenantId, documentId, version.getId(), userId, "Compatibility publish command");
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
            resolvedCaseChunkRepository.saveAndFlush(chunk);
            storeResolvedCaseEmbedding(chunk, tenantId, chunkContent);
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

    @Transactional(readOnly = true)
    public List<KnowledgeVersion> listVersions(UUID tenantId, UUID documentId) {
        requireDocument(tenantId, documentId);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
    }

    @Transactional(readOnly = true)
    public List<ResolvedCase> listResolvedCases(UUID tenantId) {
        return resolvedCaseRepository.findByTenantId(tenantId);
    }

    private KnowledgeDocument requireDocument(UUID tenantId, UUID documentId) {
        return documentRepository.findByIdAndTenantId(documentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + documentId));
    }

    private KnowledgeVersion requireVersion(UUID documentId, UUID versionId) {
        return versionRepository.findByIdAndDocumentId(versionId, documentId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge version not found: " + versionId));
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

    private void storeResolvedCaseEmbedding(ResolvedCaseChunk chunk, UUID tenantId, String content) {
        float[] embedding = requireValidEmbedding(embeddingPort.embed(content));
        int updated = resolvedCaseChunkRepository.storeEmbedding(
            chunk.getId(), tenantId, formatVector(embedding), embeddingPort.getModelName());
        if (updated != 1) throw new IllegalStateException("Resolved-case embedding was not stored");
    }

    private float[] requireValidEmbedding(float[] embedding) {
        if (embedding == null || embedding.length != embeddingPort.getDimension()) {
            throw new IllegalStateException("Embedding dimension does not match configured provider dimension");
        }
        for (float value : embedding) {
            if (!Float.isFinite(value)) throw new IllegalStateException("Embedding contains non-finite values");
        }
        return embedding;
    }

    private String formatVector(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) result.append(',');
            result.append(vector[i]);
        }
        return result.append(']').toString();
    }
}
