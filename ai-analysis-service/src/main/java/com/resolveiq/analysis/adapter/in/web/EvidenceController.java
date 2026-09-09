package com.resolveiq.analysis.adapter.in.web;

import com.resolveiq.analysis.application.dto.EvidenceDtos.*;
import com.resolveiq.analysis.application.service.evidence.EvidenceServicePort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class EvidenceController {

    private final EvidenceServicePort evidenceService;

    public EvidenceController(EvidenceServicePort evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping(value = "/api/v1/tickets/{ticketId}/evidence/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceJobResponse> uploadMultipart(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId,
        @PathVariable UUID ticketId,
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "consent", defaultValue = "true") boolean consent
    ) throws IOException {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment.bin";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        byte[] content = file.getBytes();

        EvidenceJobResponse response = evidenceService.createUploadSession(
            tenantId, ticketId, UUID.randomUUID(), fileName, contentType, content, consent
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/api/v1/tickets/{ticketId}/evidence/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvidenceJobResponse> uploadJson(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId,
        @PathVariable UUID ticketId,
        @RequestBody EvidenceUploadRequest request
    ) {
        byte[] content = request.content() != null ? request.content() : new byte[0];
        EvidenceJobResponse response = evidenceService.createUploadSession(
            tenantId, ticketId, UUID.randomUUID(), request.fileName(), request.mediaType(),
            content, request.consentGranted()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/tickets/{ticketId}/evidence/{attachmentId}/consent")
    public ResponseEntity<EvidenceJobResponse> updateConsent(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID ticketId,
        @PathVariable UUID attachmentId,
        @RequestBody EvidenceConsentRequest request
    ) {
        return ResponseEntity.ok(evidenceService.updateConsent(tenantId, ticketId, attachmentId, request.consent()));
    }

    @GetMapping("/api/v1/tickets/{ticketId}/evidence")
    public ResponseEntity<List<EvidenceJobResponse>> listEvidence(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID ticketId
    ) {
        return ResponseEntity.ok(evidenceService.listByTicket(tenantId, ticketId));
    }

    @GetMapping("/api/v1/evidence/{id}")
    public ResponseEntity<EvidenceJobResponse> getEvidence(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(evidenceService.getJob(tenantId, id));
    }

    @GetMapping("/api/v1/evidence/{id}/artifacts")
    public ResponseEntity<List<EvidenceArtifactResponse>> getArtifacts(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(evidenceService.getArtifacts(tenantId, id));
    }

    @GetMapping("/api/v1/evidence/{id}/observations")
    public ResponseEntity<List<EvidenceObservationResponse>> getObservations(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(evidenceService.getObservations(tenantId, id));
    }

    @GetMapping(value = "/api/v1/evidence/{id}/content", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getContent(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-Roles", defaultValue = "") String rolesHeader,
        @RequestHeader(value = "X-Permissions", defaultValue = "") String permissionsHeader,
        @PathVariable UUID id,
        @RequestParam(value = "variant", defaultValue = "redacted") String variant,
        @RequestParam(value = "reason", required = false) String reason
    ) {
        Set<String> roles = parseHeaderSet(rolesHeader);
        Set<String> permissions = parseHeaderSet(permissionsHeader);

        String content = evidenceService.getContent(tenantId, id, variant, roles, permissions, reason);
        return ResponseEntity.ok()
            .header("X-Content-Type-Options", "nosniff")
            .body(content);
    }

    @PostMapping("/api/v1/evidence/{id}/reprocess")
    public ResponseEntity<EvidenceJobResponse> reprocess(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(evidenceService.reprocess(tenantId, id));
    }

    @DeleteMapping("/api/v1/evidence/{id}")
    public ResponseEntity<Void> tombstoneEvidence(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        evidenceService.tombstoneEvidence(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    private Set<String> parseHeaderSet(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }
}
