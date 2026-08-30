package com.resolveiq.analysis.adapter.in.web;

import com.resolveiq.analysis.application.dto.TicketAnalysisRequest;
import com.resolveiq.analysis.application.dto.TicketAnalysisResponse;
import com.resolveiq.analysis.application.service.TicketAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final TicketAnalysisService analysisService;

    public AnalysisController(TicketAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/classify")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SYSTEM','ADMIN')")
    public ResponseEntity<TicketAnalysisResponse> classifyTicket(
        @RequestHeader(value = "X-Tenant-Id") java.util.UUID tenantId,
        @Valid @RequestBody TicketAnalysisRequest request) {
        if (!tenantId.equals(request.tenantId())) throw new SecurityException("Request tenant does not match authenticated tenant");
        TicketAnalysisResponse response = analysisService.analyzeTicket(request);
        return ResponseEntity.ok(response);
    }
}
