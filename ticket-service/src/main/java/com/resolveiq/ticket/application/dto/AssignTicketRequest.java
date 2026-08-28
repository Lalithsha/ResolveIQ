package com.resolveiq.ticket.application.dto;

import java.util.UUID;

public record AssignTicketRequest(
    UUID teamId,
    UUID agentId,
    String reason
) {}
