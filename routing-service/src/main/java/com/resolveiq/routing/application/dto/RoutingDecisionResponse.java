package com.resolveiq.routing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record RoutingDecisionResponse(
    UUID decisionId,
    UUID ticketId,
    UUID targetTeamId,
    UUID assignedAgentId,
    UUID slaPolicyId,
    Instant firstResponseDueAt,
    Instant resolutionDueAt,
    String matchedRuleName,
    String reason
) {}
