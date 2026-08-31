package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.ticket.application.dto.AttachmentResponse;
import com.resolveiq.ticket.application.service.AttachmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class AttachmentController {
    private final AttachmentService attachments;

    public AttachmentController(AttachmentService attachments) {
        this.attachments = attachments;
    }

    @PostMapping(value = "/api/v1/customer/tickets/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadCustomer(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId,
        @PathVariable UUID ticketId,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(201).body(attachments.uploadCustomer(tenantId, customerId, ticketId, file));
    }

    @GetMapping("/api/v1/customer/tickets/{ticketId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listCustomer(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId,
        @PathVariable UUID ticketId
    ) {
        return ResponseEntity.ok(attachments.listCustomer(tenantId, customerId, ticketId));
    }

    @GetMapping("/api/v1/customer/tickets/{ticketId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> downloadCustomer(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID customerId,
        @PathVariable UUID ticketId,
        @PathVariable UUID attachmentId
    ) {
        return content(attachments.downloadCustomer(tenantId, customerId, ticketId, attachmentId));
    }

    @PostMapping(value = "/api/v1/agent/tickets/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','ADMIN')")
    public ResponseEntity<AttachmentResponse> uploadStaff(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-Roles") String roleHeader,
        @PathVariable UUID ticketId,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(201).body(attachments.uploadStaff(tenantId, actorId, roles(roleHeader), ticketId, file));
    }

    @GetMapping("/api/v1/agent/tickets/{ticketId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listStaff(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-Roles") String roleHeader,
        @PathVariable UUID ticketId
    ) {
        return ResponseEntity.ok(attachments.listStaff(tenantId, actorId, roles(roleHeader), ticketId));
    }

    @GetMapping("/api/v1/agent/tickets/{ticketId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> downloadStaff(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-Roles") String roleHeader,
        @PathVariable UUID ticketId,
        @PathVariable UUID attachmentId
    ) {
        return content(attachments.downloadStaff(tenantId, actorId, roles(roleHeader), ticketId, attachmentId));
    }

    private ResponseEntity<byte[]> content(AttachmentService.AttachmentContent value) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(value.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(value.fileName(), StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(value.content());
    }

    private Set<String> roles(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(",")).map(String::trim).filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }
}
