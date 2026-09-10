package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.ticket.application.dto.IncidentDtos.*;
import com.resolveiq.ticket.application.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/incidents")
public class CustomerIncidentController {

    private final IncidentService incidentService;

    public CustomerIncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<CustomerIncidentResponse>> getActiveIncidents(
        @org.springframework.security.core.annotation.AuthenticationPrincipal com.resolveiq.security.TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantIdHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID customerIdHeader
    ) {
        UUID tenantId = principal != null ? principal.tenantId() : tenantIdHeader;
        UUID customerId = principal != null ? principal.userId() : customerIdHeader;
        if (tenantId == null || customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<CustomerIncidentResponse> list = incidentService.getActiveCustomerIncidents(tenantId, customerId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/subscriptions")
    public ResponseEntity<Void> subscribe(
        @org.springframework.security.core.annotation.AuthenticationPrincipal com.resolveiq.security.TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantIdHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID customerIdHeader,
        @PathVariable UUID id,
        @Valid @RequestBody SubscribeCustomerRequest request
    ) {
        UUID tenantId = principal != null ? principal.tenantId() : tenantIdHeader;
        UUID customerId = principal != null ? principal.userId() : customerIdHeader;
        if (tenantId == null || customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String channel = (request != null && request.channel() != null) ? request.channel() : "PORTAL";
        incidentService.subscribeCustomer(tenantId, id, customerId, channel);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
