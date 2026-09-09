package com.resolveiq.rag.adapter.in.web.flywheel;

import com.resolveiq.rag.application.dto.flywheel.FlywheelDtos.*;
import com.resolveiq.rag.application.service.flywheel.KnowledgeFlywheelServicePort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class KnowledgeFlywheelController {

    private final KnowledgeFlywheelServicePort flywheelService;

    public KnowledgeFlywheelController(KnowledgeFlywheelServicePort flywheelService) {
        this.flywheelService = flywheelService;
    }

    @GetMapping("/api/v1/knowledge/candidates")
    public ResponseEntity<List<CandidateResponse>> listCandidates(
        @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        return ResponseEntity.ok(flywheelService.listCandidates(tenantId));
    }

    @GetMapping("/api/v1/knowledge/candidates/{id}")
    public ResponseEntity<CandidateResponse> getCandidate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(flywheelService.getCandidate(tenantId, id));
    }

    @PostMapping("/api/v1/knowledge/candidates")
    public ResponseEntity<CandidateResponse> createCandidate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestBody CreateCandidateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flywheelService.createCandidate(tenantId, request));
    }

    @PostMapping("/api/v1/knowledge/candidates/{id}/sanitize")
    public ResponseEntity<CandidateResponse> sanitizeCandidate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(flywheelService.sanitizeCandidate(tenantId, id));
    }

    @PostMapping("/api/v1/knowledge/candidates/{id}/evaluation-runs")
    public ResponseEntity<EvaluationRunResponse> runEvaluation(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(flywheelService.runEvaluation(tenantId, id));
    }

    @GetMapping("/api/v1/knowledge/evaluation-runs/{id}")
    public ResponseEntity<EvaluationRunResponse> getEvaluationRun(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(flywheelService.getEvaluationRun(tenantId, id));
    }

    @PostMapping("/api/v1/knowledge/candidates/{id}/release")
    public ResponseEntity<KnowledgeReleaseResponse> releaseCandidate(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID approverId,
        @RequestHeader(value = "X-Roles", defaultValue = "") String rolesHeader,
        @RequestHeader(value = "X-Permissions", defaultValue = "") String permissionsHeader,
        @PathVariable UUID id
    ) {
        Set<String> roles = parseHeaderSet(rolesHeader);
        Set<String> permissions = parseHeaderSet(permissionsHeader);
        return ResponseEntity.ok(flywheelService.releaseCandidate(tenantId, id, approverId, roles, permissions));
    }

    @PostMapping("/api/v1/knowledge/releases/{id}/rollback")
    public ResponseEntity<RollbackResponse> rollbackRelease(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID performedBy,
        @RequestHeader(value = "X-Roles", defaultValue = "") String rolesHeader,
        @RequestHeader(value = "X-Permissions", defaultValue = "") String permissionsHeader,
        @PathVariable UUID id,
        @RequestBody RollbackRequest request
    ) {
        Set<String> roles = parseHeaderSet(rolesHeader);
        Set<String> permissions = parseHeaderSet(permissionsHeader);
        return ResponseEntity.ok(flywheelService.rollbackRelease(
            tenantId, id, request.targetReleaseId(), performedBy, request.reason(), roles, permissions
        ));
    }

    private Set<String> parseHeaderSet(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }
}
