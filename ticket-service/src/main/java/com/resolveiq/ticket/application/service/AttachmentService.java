package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.application.dto.AttachmentResponse;
import com.resolveiq.ticket.application.dto.TicketResponse;
import com.resolveiq.ticket.application.port.AttachmentStoragePort;
import com.resolveiq.ticket.application.port.MalwareScannerPort;
import com.resolveiq.ticket.domain.exception.UnsafeAttachmentException;
import com.resolveiq.ticket.domain.model.TicketAttachment;
import com.resolveiq.ticket.domain.repository.TicketAttachmentRepository;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AttachmentService {
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf", "image/png", "image/jpeg", "text/plain", "application/json");
    private final TicketRepository tickets;
    private final TicketAttachmentRepository attachments;
    private final AttachmentStoragePort storage;
    private final MalwareScannerPort scanner;
    private final com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository memberships;
    private final boolean enabled;
    private final long maxBytes;
    private final int maxFiles;

    public AttachmentService(TicketRepository tickets, TicketAttachmentRepository attachments,
                             AttachmentStoragePort storage, MalwareScannerPort scanner,
                             com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository memberships,
                             @Value("${resolveiq.attachments.enabled:true}") boolean enabled,
                             @Value("${resolveiq.attachments.max-bytes:10485760}") long maxBytes,
                             @Value("${resolveiq.attachments.max-files-per-ticket:5}") int maxFiles) {
        this.tickets = tickets;
        this.attachments = attachments;
        this.storage = storage;
        this.scanner = scanner;
        this.memberships = memberships;
        this.enabled = enabled;
        this.maxBytes = maxBytes;
        this.maxFiles = maxFiles;
    }

    public AttachmentResponse uploadCustomer(UUID tenantId, UUID customerId, UUID ticketId, MultipartFile file) {
        tickets.findByIdAndTenantIdAndCustomerId(ticketId, tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        return upload(tenantId, customerId, ticketId, file);
    }

    public AttachmentResponse uploadStaff(UUID tenantId, UUID actorId, Set<String> roles, UUID ticketId, MultipartFile file) {
        ensureStaffAccess(tenantId, actorId, roles, ticketId);
        return upload(tenantId, actorId, ticketId, file);
    }

    public List<AttachmentResponse> listCustomer(UUID tenantId, UUID customerId, UUID ticketId) {
        tickets.findByIdAndTenantIdAndCustomerId(ticketId, tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        return list(tenantId, ticketId);
    }

    public List<AttachmentResponse> listStaff(UUID tenantId, UUID actorId, Set<String> roles, UUID ticketId) {
        ensureStaffAccess(tenantId, actorId, roles, ticketId);
        return list(tenantId, ticketId);
    }

    public AttachmentContent downloadCustomer(UUID tenantId, UUID customerId, UUID ticketId, UUID attachmentId) {
        tickets.findByIdAndTenantIdAndCustomerId(ticketId, tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        return download(tenantId, ticketId, attachmentId);
    }

    public AttachmentContent downloadStaff(UUID tenantId, UUID actorId, Set<String> roles, UUID ticketId, UUID attachmentId) {
        ensureStaffAccess(tenantId, actorId, roles, ticketId);
        return download(tenantId, ticketId, attachmentId);
    }

    private AttachmentResponse upload(UUID tenantId, UUID uploaderId, UUID ticketId, MultipartFile file) {
        if (!enabled) throw new IllegalStateException("Attachment upload is disabled");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Attachment file is required");
        if (file.getSize() > maxBytes) throw new AttachmentTooLargeException("Attachment exceeds the 10 MiB limit");
        if (attachments.countByTenantIdAndTicketId(tenantId, ticketId) >= maxFiles) {
            throw new AttachmentTooLargeException("Ticket attachment limit reached");
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream").toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) throw new UnsupportedAttachmentTypeException("Unsupported attachment type");
        try {
            byte[] content = file.getBytes();
            validateMagic(contentType, content);
            String fileName = normalizeName(file.getOriginalFilename());
            MalwareScannerPort.ScanResult scan = scanner.scan(fileName, content);
            if (!scan.clean()) throw new UnsafeAttachmentException("Attachment rejected by malware scanning");
            String key = tenantId + "/" + ticketId + "/" + UUID.randomUUID();
            storage.put(key, contentType, content);
            try {
                TicketAttachment entity = attachments.save(new TicketAttachment(
                    ticketId, tenantId, uploaderId, fileName, contentType, content.length, key,
                    sha256(content), scanner.engineName(), limit(scan.detail(), 500)));
                return AttachmentResponse.from(entity);
            } catch (RuntimeException persistenceFailure) {
                storage.delete(key);
                throw persistenceFailure;
            }
        } catch (UnsafeAttachmentException | UnsupportedAttachmentTypeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Unable to process attachment", error);
        }
    }

    private List<AttachmentResponse> list(UUID tenantId, UUID ticketId) {
        return attachments.findByTenantIdAndTicketIdOrderByCreatedAtAsc(tenantId, ticketId)
            .stream().map(AttachmentResponse::from).toList();
    }

    private AttachmentContent download(UUID tenantId, UUID ticketId, UUID attachmentId) {
        TicketAttachment attachment = attachments.findByIdAndTenantIdAndTicketId(attachmentId, tenantId, ticketId)
            .filter(value -> "CLEAN".equals(value.getScanStatus()))
            .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        return new AttachmentContent(attachment.getFileName(), attachment.getContentType(), storage.get(attachment.getStorageKey()));
    }

    private void ensureStaffAccess(UUID tenantId, UUID actorId, Set<String> roles, UUID ticketId) {
        TicketResponse ticket = tickets.findByIdAndTenantId(ticketId, tenantId).map(TicketResponse::fromEntity)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        if (roles.contains("ADMIN") || roles.contains("AUDITOR")) return;
        if ((roles.contains("AGENT") || roles.contains("TEAM_LEAD")) && (actorId.equals(ticket.assignedAgentId()) ||
            (ticket.teamId() != null && memberships.existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(tenantId, actorId, ticket.teamId())))) return;
        throw new SecurityException("Ticket is outside the authorized queue");
    }

    private void validateMagic(String contentType, byte[] content) {
        boolean valid = switch (contentType) {
            case "application/pdf" -> startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "image/png" -> startsWith(content, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            case "image/jpeg" -> content.length >= 3 && content[0] == (byte) 0xff && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff;
            case "text/plain", "application/json" -> !containsNul(content);
            default -> false;
        };
        if (!valid) throw new UnsupportedAttachmentTypeException("File content does not match its declared type");
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (content[i] != prefix[i]) return false;
        return true;
    }

    private boolean containsNul(byte[] content) {
        for (byte value : content) if (value == 0) return true;
        return false;
    }

    private String normalizeName(String original) {
        String value = original == null ? "attachment" : original.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (value.isBlank()) value = "attachment";
        return limit(value, 255);
    }

    private String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private String limit(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }

    public record AttachmentContent(String fileName, String contentType, byte[] content) {}
    public static class UnsupportedAttachmentTypeException extends RuntimeException { public UnsupportedAttachmentTypeException(String value) { super(value); } }
    public static class AttachmentTooLargeException extends RuntimeException { public AttachmentTooLargeException(String value) { super(value); } }
}
