package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.ticket.application.dto.IncidentDtos.*;
import com.resolveiq.ticket.application.service.IncidentService;
import com.resolveiq.ticket.domain.model.IncidentSeverity;
import com.resolveiq.ticket.domain.model.IncidentStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> list(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestParam(required = false) IncidentStatus status,
        @RequestParam(required = false) IncidentSeverity severity,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<IncidentResponse> result = incidentService.listIncidents(
            tenantId,
            status,
            severity,
            PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "detectedAt"))
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> get(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(incidentService.getIncident(tenantId, id));
    }

    @GetMapping("/proposals")
    public ResponseEntity<List<IncidentProposalResponse>> proposals(
        @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        return ResponseEntity.ok(incidentService.listProposals(tenantId));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<IncidentResponse> confirm(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(incidentService.confirmIncident(tenantId, id, userId));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<IncidentResponse> dismiss(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(incidentService.dismissIncident(tenantId, id, userId, reason));
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<IncidentResponse> transition(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @Valid @RequestBody TransitionIncidentRequest request
    ) {
        return ResponseEntity.ok(incidentService.transitionIncident(tenantId, id, request.status(), request.resolutionSummary(), userId));
    }

    @PostMapping("/{id}/tickets/{ticketId}/link")
    public ResponseEntity<Void> linkTicket(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @PathVariable UUID ticketId,
        @RequestParam(defaultValue = "0.85") double similarityScore
    ) {
        incidentService.linkTicket(tenantId, id, ticketId, userId, similarityScore);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/tickets/{ticketId}")
    public ResponseEntity<Void> unlinkTicket(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @PathVariable UUID ticketId,
        @RequestParam(required = false) String reason
    ) {
        incidentService.unlinkTicket(tenantId, id, ticketId, userId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/updates")
    public ResponseEntity<IncidentUpdateResponse> createUpdate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @Valid @RequestBody CreateUpdateRequest request
    ) {
        IncidentUpdateResponse res = incidentService.createUpdate(tenantId, id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/{id}/updates/{updateId}/approve")
    public ResponseEntity<IncidentUpdateResponse> approveUpdate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader(value = "X-Roles", defaultValue = "") String rolesHeader,
        @PathVariable UUID id,
        @PathVariable UUID updateId
    ) {
        Set<String> roles = new HashSet<>(Arrays.asList(rolesHeader.split(",")));
        return ResponseEntity.ok(incidentService.approveUpdate(tenantId, id, updateId, userId, roles));
    }

    @PostMapping("/{id}/updates/{updateId}/publish")
    public ResponseEntity<IncidentUpdateResponse> publishUpdate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @PathVariable UUID updateId
    ) {
        return ResponseEntity.ok(incidentService.publishUpdate(tenantId, id, updateId, userId));
    }

    @PostMapping("/detect")
    public ResponseEntity<DetectionRunResult> runDetect(
        @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        return ResponseEntity.ok(incidentService.runDetectionScan(tenantId));
    }
}
