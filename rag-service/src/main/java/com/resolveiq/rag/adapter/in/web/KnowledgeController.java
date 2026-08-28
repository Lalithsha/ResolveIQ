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
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @Valid @RequestBody CreateDocumentRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        KnowledgeDocument doc = knowledgeService.createDocument(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/articles")
    public ResponseEntity<List<KnowledgeDocument>> listArticles(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<KnowledgeDocument> docs = knowledgeService.listDocuments(tenantId);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<KnowledgeDocument> getArticle(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable("id") UUID documentId
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        KnowledgeDocument doc = knowledgeService.getDocument(tenantId, documentId);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/articles/{id}/publish")
    public ResponseEntity<KnowledgeDocument> publishVersion(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @PathVariable("id") UUID documentId,
        @Valid @RequestBody PublishVersionRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        KnowledgeDocument doc = knowledgeService.publishNewVersion(tenantId, documentId, userId, request);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/resolved-cases/approve")
    public ResponseEntity<ResolvedCase> approveResolvedCase(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @Valid @RequestBody ApproveResolvedCaseRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        ResolvedCase resolvedCase = knowledgeService.approveResolvedCase(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resolvedCase);
    }
}
