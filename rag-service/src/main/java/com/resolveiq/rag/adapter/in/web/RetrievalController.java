package com.resolveiq.rag.adapter.in.web;

import com.resolveiq.rag.application.dto.RetrievalQueryRequest;
import com.resolveiq.rag.application.dto.RetrievalResultDto;
import com.resolveiq.rag.application.service.HybridRetrievalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/retrieval")
public class RetrievalController {

    private final HybridRetrievalService retrievalService;

    public RetrievalController(HybridRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/search")
    public ResponseEntity<RetrievalResultDto> search(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @Valid @RequestBody RetrievalQueryRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        int topK = request.topK() != null ? request.topK() : 5;

        RetrievalResultDto result = retrievalService.searchHybrid(
            tenantId,
            request.ticketId(),
            request.queryText(),
            request.strategy(),
            topK
        );
        return ResponseEntity.ok(result);
    }
}
