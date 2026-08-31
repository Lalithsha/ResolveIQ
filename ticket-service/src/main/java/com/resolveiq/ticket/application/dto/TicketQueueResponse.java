package com.resolveiq.ticket.application.dto;

import java.util.List;

public record TicketQueueResponse(
    List<TicketResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
