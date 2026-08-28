package com.resolveiq.auth.application.dto;

import com.resolveiq.auth.domain.model.Role;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInMs,
    UUID userId,
    UUID tenantId,
    String email,
    String fullName,
    Set<Role> roles
) {}
