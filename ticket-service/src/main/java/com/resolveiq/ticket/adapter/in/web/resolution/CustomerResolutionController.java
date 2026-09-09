package com.resolveiq.ticket.adapter.in.web.resolution;

import com.resolveiq.ticket.application.dto.resolution.ResolutionDtos.*;
import com.resolveiq.ticket.application.service.resolution.ResolutionServicePort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/tickets/{ticketId}")
public class CustomerResolutionController {

    private final ResolutionServicePort resolutionService;

    public CustomerResolutionController(ResolutionServicePort resolutionService) {
        this.resolutionService = resolutionService;
    }

    @PostMapping("/resolution-outcome")
    public ResponseEntity<ResolutionAttemptResponse> submitOutcome(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId,
        @PathVariable UUID ticketId,
        @RequestBody ResolutionOutcomeRequest request
    ) {
        ResolutionAttemptResponse response = resolutionService.recordOutcome(
            tenantId, ticketId, customerId, request.rating(), request.reason()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reopen")
    public ResponseEntity<ResolutionAttemptResponse> reopenTicket(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId,
        @PathVariable UUID ticketId,
        @RequestBody(required = false) ResolutionReopenRequest request
    ) {
        String reason = request != null ? request.reason() : "Customer requested ticket reopen";
        ResolutionAttemptResponse response = resolutionService.reopenTicket(
            tenantId, ticketId, customerId, reason
        );
        return ResponseEntity.ok(response);
    }
}
