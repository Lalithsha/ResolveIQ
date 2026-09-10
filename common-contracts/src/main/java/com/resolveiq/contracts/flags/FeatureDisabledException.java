package com.resolveiq.contracts.flags;

public class FeatureDisabledException extends RuntimeException {
    private final FeatureFlag flag;

    public FeatureDisabledException(FeatureFlag flag) {
        super("Feature is disabled for this tenant: " + (flag != null ? flag.flagKey() : "unknown"));
        this.flag = flag;
    }

    public FeatureFlag getFlag() {
        return flag;
    }
}
