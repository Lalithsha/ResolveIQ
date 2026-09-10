package com.resolveiq.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record StepUpRequest(
    @NotBlank(message = "Password is required for step-up authentication")
    String password
) {}
