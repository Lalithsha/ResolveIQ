package com.resolveiq.contracts.flags;

public enum FeatureFlag {
    INCIDENT_RADAR_ENABLED("incident_radar_enabled", true),
    RESOLUTION_ACTIONS_ENABLED("resolution_actions_enabled", true),
    EMAIL_CHANNEL_ENABLED("email_channel_enabled", true),
    MULTIMODAL_PROCESSING_ENABLED("multimodal_processing_enabled", true),
    RESOLUTION_FLYWHEEL_ENABLED("resolution_flywheel_enabled", true);

    private final String flagKey;
    private final boolean defaultValue;

    FeatureFlag(String flagKey, boolean defaultValue) {
        this.flagKey = flagKey;
        this.defaultValue = defaultValue;
    }

    public String flagKey() {
        return flagKey;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public static FeatureFlag fromKey(String key) {
        for (FeatureFlag flag : values()) {
            if (flag.flagKey.equalsIgnoreCase(key)) {
                return flag;
            }
        }
        throw new IllegalArgumentException("Unknown feature flag key: " + key);
    }
}
