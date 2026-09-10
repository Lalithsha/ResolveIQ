package com.resolveiq.security;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TrustedPrincipal(
    UUID userId,
    UUID tenantId,
    Set<String> roles,
    String authenticationType,
    Set<String> permissions,
    Instant authTime
) {
    public TrustedPrincipal(UUID userId, UUID tenantId, Set<String> roles, String authenticationType) {
        this(userId, tenantId, roles, authenticationType, RolePermissions.defaultPermissionsForRoles(roles), Instant.now());
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permission != null && hasPermission(permission.name());
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isRecentAuthentication(long maxAgeSeconds) {
        if (authTime == null) {
            return false;
        }
        Instant now = Instant.now();
        return !authTime.isBefore(now.minusSeconds(maxAgeSeconds)) && !authTime.isAfter(now.plusSeconds(30));
    }
}
