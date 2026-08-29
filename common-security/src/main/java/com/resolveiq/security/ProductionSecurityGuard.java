package com.resolveiq.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionSecurityGuard {
    public ProductionSecurityGuard(@Value("${resolveiq.jwt.secret:}") String secret) {
        if (secret.isBlank() || secret.startsWith("fictional_jwt_")) {
            throw new IllegalStateException("Production requires a non-default JWT signing secret");
        }
    }
}
