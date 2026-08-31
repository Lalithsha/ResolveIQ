package com.resolveiq.auth.application.dto;

import com.resolveiq.auth.domain.model.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(
    @NotEmpty(message = "At least one role is required") Set<Role> roles
) {}
