package com.resolveiq.auth.application.dto;

import com.resolveiq.auth.domain.model.Role;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    @com.fasterxml.jackson.annotation.JsonIgnore
    String refreshToken,
    String tokenType,
    long expiresInMs,
    UUID userId,
    UUID tenantId,
    String email,
    String fullName,
    Set<Role> roles,
    Set<String> permissions
) {
    public AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UUID userId,
        UUID tenantId,
        String email,
        String fullName,
        Set<Role> roles
    ) {
        this(accessToken, refreshToken, tokenType, expiresInMs, userId, tenantId, email, fullName, roles, Set.of());
    }
}
