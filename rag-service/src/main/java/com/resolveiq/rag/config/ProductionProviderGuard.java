package com.resolveiq.rag.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionProviderGuard {

    @Value("${resolveiq.ai.embedding-provider:deterministic}")
    private String provider;

    @PostConstruct
    void validate() {
        if ("deterministic".equalsIgnoreCase(provider) || "mock".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("Production profile requires a real embedding provider");
        }
    }
}
