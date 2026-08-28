package com.resolveiq.routing.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateTeamRequest(
    @NotBlank(message = "Team name is required")
    String name,
    String description,
    int maxActiveTickets
) {}
