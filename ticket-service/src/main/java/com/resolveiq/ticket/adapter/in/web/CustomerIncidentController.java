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
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId
    ) {
        List<CustomerIncidentResponse> list = incidentService.getActiveCustomerIncidents(tenantId, customerId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/subscriptions")
    public ResponseEntity<Void> subscribe(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId,
        @PathVariable UUID id,
        @Valid @RequestBody SubscribeCustomerRequest request
    ) {
        String channel = (request != null && request.channel() != null) ? request.channel() : "PORTAL";
        incidentService.subscribeCustomer(tenantId, id, customerId, channel);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
