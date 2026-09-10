package com.resolveiq.contracts.flags;

import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagStore {
    Optional<Boolean> getFlag(UUID tenantId, String flagKey);
    void setFlag(UUID tenantId, String flagKey, boolean enabled);
}
