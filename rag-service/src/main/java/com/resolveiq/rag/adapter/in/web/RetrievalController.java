package com.resolveiq.rag.adapter.in.web;

import com.resolveiq.rag.application.dto.RetrievalQueryRequest;
import com.resolveiq.rag.application.dto.RetrievalResultDto;
import com.resolveiq.rag.application.dto.TicketSimilarityRequest;
import com.resolveiq.rag.application.dto.TicketSimilarityResponse;
import com.resolveiq.rag.application.service.HybridRetrievalService;
import com.resolveiq.rag.application.service.TicketSimilarityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/retrieval")
public class RetrievalController {

    private final HybridRetrievalService retrievalService;
    private final TicketSimilarityService similarityService;

    public RetrievalController(HybridRetrievalService retrievalService, TicketSimilarityService similarityService) {
        this.retrievalService = retrievalService;
        this.similarityService = similarityService;
    }

    @PostMapping("/search")
    public ResponseEntity<RetrievalResultDto> search(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @Valid @RequestBody RetrievalQueryRequest request
    ) {
        int topK = request.topK() != null ? request.topK() : 5;

        RetrievalResultDto result = retrievalService.searchHybrid(
            tenantId,
            request.ticketId(),
            request.queryText(),
            request.strategy(),
            topK,
            request.category(),
            request.product(),
            request.language(),
            request.sourceTypes()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ticket-similarity")
    public ResponseEntity<TicketSimilarityResponse> computeTicketSimilarity(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @Valid @RequestBody TicketSimilarityRequest request
    ) {
        TicketSimilarityResponse response = similarityService.computeSimilarity(tenantId, request);
        return ResponseEntity.ok(response);
    }
}
