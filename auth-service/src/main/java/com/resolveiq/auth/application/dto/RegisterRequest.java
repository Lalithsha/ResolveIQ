package com.resolveiq.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be 12-128 characters")
    String password,

    @NotBlank(message = "Full name is required")
    String fullName
) {}
