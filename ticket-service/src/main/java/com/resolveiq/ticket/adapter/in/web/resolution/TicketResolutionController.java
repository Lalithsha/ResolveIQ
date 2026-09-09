package com.resolveiq.ticket.adapter.in.web.resolution;

import com.resolveiq.ticket.application.dto.resolution.ResolutionDtos.*;
import com.resolveiq.ticket.application.service.resolution.ResolutionServicePort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class TicketResolutionController {

    private final ResolutionServicePort resolutionService;

    public TicketResolutionController(ResolutionServicePort resolutionService) {
        this.resolutionService = resolutionService;
    }

    @PostMapping("/api/v1/tickets/{ticketId}/resolve")
    public ResponseEntity<ResolutionAttemptResponse> resolveTicket(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID resolverId,
        @PathVariable UUID ticketId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String fingerprint = body != null ? body.getOrDefault("solutionFingerprint", "STANDARD_RESOLUTION") : "STANDARD_RESOLUTION";
        ResolutionAttemptResponse response = resolutionService.resolveTicket(tenantId, ticketId, resolverId, fingerprint);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/tickets/{ticketId}/resolution")
    public ResponseEntity<List<ResolutionAttemptResponse>> getResolutionHistory(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID ticketId
    ) {
        return ResponseEntity.ok(resolutionService.getResolutionHistory(tenantId, ticketId));
    }

    @GetMapping("/api/v1/governance/resolution-metrics")
    public ResponseEntity<ResolutionMetricsResponse> getMetrics(
        @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        return ResponseEntity.ok(resolutionService.getResolutionMetrics(tenantId));
    }
}
