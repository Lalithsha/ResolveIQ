package com.resolveiq.contracts.flags;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureFlagService {

    private final Map<String, Boolean> tenantFlagOverrides = new ConcurrentHashMap<>();
    private final boolean defaultEnabled;
    private final FeatureFlagStore store;

    public FeatureFlagService() {
        this(null, false);
    }

    public FeatureFlagService(boolean defaultEnabled) {
        this(null, defaultEnabled);
    }

    public FeatureFlagService(FeatureFlagStore store) {
        this(store, false);
    }

    public FeatureFlagService(FeatureFlagStore store, boolean defaultEnabled) {
        this.store = store;
        this.defaultEnabled = defaultEnabled;
    }

    public boolean isEnabled(UUID tenantId, FeatureFlag flag) {
        if (tenantId == null || flag == null) {
            return false;
        }
        String key = tenantId + ":" + flag.flagKey();
        Boolean cached = tenantFlagOverrides.get(key);
        if (cached != null) {
            return cached;
        }
        if (store != null) {
            java.util.Optional<Boolean> stored = store.getFlag(tenantId, flag.flagKey());
            if (stored.isPresent()) {
                tenantFlagOverrides.put(key, stored.get());
                return stored.get();
            }
        }
        return defaultEnabled && flag.defaultValue();
    }

    public void setEnabled(UUID tenantId, FeatureFlag flag, boolean enabled) {
        String key = tenantId + ":" + flag.flagKey();
        tenantFlagOverrides.put(key, enabled);
        if (store != null) {
            store.setFlag(tenantId, flag.flagKey(), enabled);
        }
    }

    public void requireEnabled(UUID tenantId, FeatureFlag flag) {
        if (!isEnabled(tenantId, flag)) {
            throw new FeatureDisabledException(flag);
        }
    }
}
