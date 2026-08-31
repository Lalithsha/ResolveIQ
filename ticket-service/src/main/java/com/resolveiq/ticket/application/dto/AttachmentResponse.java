package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.TicketAttachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    UUID ticketId,
    UUID uploaderId,
    String fileName,
    String contentType,
    long sizeBytes,
    String scanStatus,
    String sha256,
    String scanEngine,
    Instant createdAt
) {
    public static AttachmentResponse from(TicketAttachment value) {
        return new AttachmentResponse(value.getId(), value.getTicketId(), value.getUploaderId(), value.getFileName(),
            value.getContentType(), value.getSizeBytes(), value.getScanStatus(), value.getSha256(),
            value.getScanEngine(), value.getCreatedAt());
    }
}
