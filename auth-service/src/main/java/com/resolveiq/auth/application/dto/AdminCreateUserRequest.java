package com.resolveiq.auth.application.dto;

import com.resolveiq.auth.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record AdminCreateUserRequest(
    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Full name is required")
    String fullName,

    @NotEmpty(message = "At least one role is required")
    Set<Role> roles
) {}
