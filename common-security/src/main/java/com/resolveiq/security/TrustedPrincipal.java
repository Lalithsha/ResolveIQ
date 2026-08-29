package com.resolveiq.security;

import java.util.Set;
import java.util.UUID;

public record TrustedPrincipal(UUID userId, UUID tenantId, Set<String> roles, String authenticationType) {
}
