package com.resolveiq.ticket.application.dto;

import java.util.List;

public record AgentTicketContextResponse(
    TicketResponse ticket,
    List<TicketMessageResponse> messages,
    List<AiSuggestionResponse> suggestions
) {}
