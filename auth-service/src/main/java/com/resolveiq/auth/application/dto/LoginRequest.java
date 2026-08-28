package com.resolveiq.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record LoginRequest(
    UUID tenantId,
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    String email,
    @NotBlank(message = "Password is required")
    String password
) {}
