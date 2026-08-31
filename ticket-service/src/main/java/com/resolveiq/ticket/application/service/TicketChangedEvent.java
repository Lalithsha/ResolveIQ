package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.application.dto.TicketResponse;

public record TicketChangedEvent(String eventType, TicketResponse ticket) {}
