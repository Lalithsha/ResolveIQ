package com.resolveiq.routing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRoutingRuleRequest(
    @NotBlank(message = "Rule name is required")
    String name,
    @NotBlank(message = "Conditions are required")
    String conditions,
    @NotNull(message = "Target team ID is required")
    UUID targetTeamId,
    int priorityOrder
) {}
