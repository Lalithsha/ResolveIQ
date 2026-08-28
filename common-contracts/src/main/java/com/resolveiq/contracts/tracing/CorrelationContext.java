package com.resolveiq.contracts.tracing;

import java.util.UUID;

/**
 * Thread-local context for managing correlation IDs across request and event lifecycles.
 */
public final class CorrelationContext {

    private static final ThreadLocal<UUID> CORRELATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    private CorrelationContext() {}

    public static void setCorrelationId(UUID correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static UUID getCorrelationId() {
        return CORRELATION_ID.get();
    }

    public static UUID getOrCreateCorrelationId() {
        UUID id = CORRELATION_ID.get();
        if (id == null) {
            id = UUID.randomUUID();
            CORRELATION_ID.set(id);
        }
        return id;
    }

    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        CORRELATION_ID.remove();
        TENANT_ID.remove();
    }
}
