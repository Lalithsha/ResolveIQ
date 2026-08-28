package com.resolveiq.ticket.application.dto;

import com.resolveiq.ticket.domain.model.TicketMessage;

import java.time.Instant;
import java.util.UUID;

public record TicketMessageResponse(
    UUID id,
    UUID ticketId,
    UUID senderId,
    String senderRole,
    String content,
    boolean isInternal,
    Instant createdAt
) {
    public static TicketMessageResponse fromEntity(TicketMessage msg) {
        return new TicketMessageResponse(
            msg.getId(),
            msg.getTicketId(),
            msg.getSenderId(),
            msg.getSenderRole(),
            msg.getContent(),
            msg.isInternal(),
            msg.getCreatedAt()
        );
    }
}
