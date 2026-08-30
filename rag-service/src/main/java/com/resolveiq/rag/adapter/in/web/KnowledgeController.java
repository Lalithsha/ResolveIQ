package com.resolveiq.rag.adapter.in.web;

import com.resolveiq.rag.application.dto.*;
import com.resolveiq.rag.application.service.KnowledgeService;
import com.resolveiq.rag.domain.model.KnowledgeDocument;
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

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
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

    @PostMapping("/resolved-cases/approve")
    public ResponseEntity<ResolvedCase> approveResolvedCase(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID userId,
        @Valid @RequestBody ApproveResolvedCaseRequest request
    ) {
        ResolvedCase resolvedCase = knowledgeService.approveResolvedCase(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resolvedCase);
    }
}
