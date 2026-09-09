package com.resolveiq.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RolePermissions {

    private RolePermissions() {}

    private static final Map<String, Set<Permission>> DEFAULT_TEMPLATES = Map.of(
        "CUSTOMER", Collections.emptySet(),
        "AGENT", EnumSet.of(Permission.ACTION_APPROVE_LOW_RISK),
        "TEAM_LEAD", EnumSet.of(
            Permission.INCIDENT_APPROVE,
            Permission.INCIDENT_PUBLISH,
            Permission.ACTION_APPROVE_LOW_RISK,
            Permission.ACTION_APPROVE_FINANCIAL
        ),
        "KNOWLEDGE_MANAGER", EnumSet.of(Permission.KNOWLEDGE_RELEASE_APPROVE),
        "ADMIN", EnumSet.of(
            Permission.INCIDENT_APPROVE,
            Permission.INCIDENT_PUBLISH,
            Permission.ACTION_APPROVE_LOW_RISK,
            Permission.ACTION_APPROVE_FINANCIAL,
            Permission.KNOWLEDGE_RELEASE_APPROVE
        ),
        "AUDITOR", Collections.emptySet()
    );

    public static Set<String> defaultPermissionsForRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }
        return roles.stream()
            .flatMap(role -> DEFAULT_TEMPLATES.getOrDefault(role, Collections.emptySet()).stream())
            .map(Permission::name)
            .collect(Collectors.toSet());
    }

    public static Set<Permission> getTemplateForRole(String role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return DEFAULT_TEMPLATES.getOrDefault(role, Collections.emptySet());
    }
}
