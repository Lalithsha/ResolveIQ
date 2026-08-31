package com.resolveiq.analysis.adapter.in.web;

import com.resolveiq.analysis.application.dto.TicketAnalysisRequest;
import com.resolveiq.analysis.application.dto.TicketAnalysisResponse;
import com.resolveiq.analysis.application.service.TicketAnalysisService;
import com.resolveiq.analysis.application.dto.AnalysisGovernanceSummary;
import com.resolveiq.analysis.application.service.AnalysisGovernanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final TicketAnalysisService analysisService;
    private final AnalysisGovernanceService governanceService;

    public AnalysisController(TicketAnalysisService analysisService, AnalysisGovernanceService governanceService) {
        this.analysisService = analysisService;
        this.governanceService = governanceService;
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

    @GetMapping("/governance/summary")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<AnalysisGovernanceSummary> governance(
        @RequestHeader(value = "X-Tenant-Id") java.util.UUID tenantId
    ) {
        return ResponseEntity.ok(governanceService.summary(tenantId));
    }

    @GetMapping("/invocations")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<org.springframework.data.domain.Page<com.resolveiq.analysis.application.dto.AnalysisTraceResponse>> invocations(
        @RequestHeader(value = "X-Tenant-Id") java.util.UUID tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(governanceService.invocations(tenantId, page, size));
    }
}
