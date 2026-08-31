package com.resolveiq.rag.adapter.in.web;

import com.resolveiq.rag.application.dto.*;
import com.resolveiq.rag.application.service.KnowledgeService;
import com.resolveiq.rag.application.service.KnowledgeIndexingService;
import com.resolveiq.rag.domain.model.KnowledgeDocument;
import com.resolveiq.rag.domain.model.KnowledgeVersion;
import com.resolveiq.rag.domain.model.ResolvedCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeIndexingService indexingService;

    public KnowledgeController(KnowledgeService knowledgeService, KnowledgeIndexingService indexingService) {
        this.knowledgeService = knowledgeService;
        this.indexingService = indexingService;
    }

    @PostMapping("/articles")
    public ResponseEntity<KnowledgeDocument> createArticle(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @Valid @RequestBody CreateDocumentRequest request
    ) {
        KnowledgeDocument doc = knowledgeService.createDocument(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/articles")
    public ResponseEntity<List<KnowledgeDocument>> listArticles(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId
    ) {
        List<KnowledgeDocument> docs = knowledgeService.listDocuments(tenantId);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<KnowledgeDocument> getArticle(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable(value = "id") UUID documentId
    ) {
        KnowledgeDocument doc = knowledgeService.getDocument(tenantId, documentId);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/articles/{id}/publish")
    public ResponseEntity<KnowledgeDocument> publishVersion(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @PathVariable(value = "id") UUID documentId,
        @Valid @RequestBody PublishVersionRequest request
    ) {
        KnowledgeDocument doc = knowledgeService.publishNewVersion(tenantId, documentId, userId, request);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/articles/{id}/versions")
    public ResponseEntity<KnowledgeVersion> createVersion(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @PathVariable(value = "id") UUID documentId,
        @Valid @RequestBody CreateVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(knowledgeService.createVersion(tenantId, documentId, userId, request));
    }

    @GetMapping("/articles/{id}/versions")
    public ResponseEntity<List<KnowledgeVersion>> listVersions(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable(value = "id") UUID documentId
    ) {
        return ResponseEntity.ok(knowledgeService.listVersions(tenantId, documentId));
    }

    @PostMapping("/articles/{id}/versions/{versionId}/submit")
    public ResponseEntity<KnowledgeVersion> submitForReview(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable(value = "id") UUID documentId,
        @PathVariable UUID versionId
    ) {
        return ResponseEntity.ok(knowledgeService.submitForReview(tenantId, documentId, versionId));
    }

    @PostMapping("/articles/{id}/versions/{versionId}/reject")
    public ResponseEntity<KnowledgeVersion> rejectVersion(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @PathVariable(value = "id") UUID documentId,
        @PathVariable UUID versionId,
        @Valid @RequestBody ReviewDecisionRequest request
    ) {
        return ResponseEntity.ok(knowledgeService.rejectVersion(
            tenantId, documentId, versionId, userId, request.note()));
    }

    @PostMapping("/articles/{id}/versions/{versionId}/publish")
    public ResponseEntity<KnowledgeDocument> publishReviewedVersion(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @PathVariable(value = "id") UUID documentId,
        @PathVariable UUID versionId,
        @Valid @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        return ResponseEntity.ok(knowledgeService.publishVersion(
            tenantId, documentId, versionId, userId, request == null ? null : request.note()));
    }

    @PostMapping("/articles/{id}/rollback/{versionId}")
    public ResponseEntity<KnowledgeDocument> rollback(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @PathVariable(value = "id") UUID documentId,
        @PathVariable UUID versionId,
        @Valid @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        return ResponseEntity.ok(knowledgeService.rollback(
            tenantId, documentId, versionId, userId, request == null ? null : request.note()));
    }

    @PostMapping("/articles/{id}/archive")
    public ResponseEntity<KnowledgeDocument> archive(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable(value = "id") UUID documentId
    ) {
        return ResponseEntity.ok(knowledgeService.archive(tenantId, documentId));
    }

    @PostMapping("/resolved-cases/approve")
    public ResponseEntity<ResolvedCase> approveResolvedCase(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @Valid @RequestBody ApproveResolvedCaseRequest request
    ) {
        ResolvedCase resolvedCase = knowledgeService.approveResolvedCase(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resolvedCase);
    }

    @GetMapping("/resolved-cases")
    public ResponseEntity<List<ResolvedCase>> listResolvedCases(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId
    ) {
        return ResponseEntity.ok(knowledgeService.listResolvedCases(tenantId));
    }

    @PostMapping("/admin/reindex-missing")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('KNOWLEDGE_MANAGER','ADMIN')")
    public ResponseEntity<ReindexKnowledgeResponse> reindexMissing(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId
    ) {
        return ResponseEntity.ok(indexingService.reindexMissing(tenantId));
    }
}
