package com.resolveiq.orchestration.application.action;

public record CompensationResult(
        boolean isSupported,
        String status, // NON_COMPENSATABLE, COMPENSATED, FAILED
        String reason
) {
    public static CompensationResult nonCompensatable(String reason) {
        return new CompensationResult(false, "NON_COMPENSATABLE", reason);
    }
}
