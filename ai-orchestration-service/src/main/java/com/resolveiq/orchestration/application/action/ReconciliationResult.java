package com.resolveiq.orchestration.application.action;

import java.util.Map;

public record ReconciliationResult(
        boolean isMatch,
        String status, // RECONCILED, MISMATCH_MANUAL_REVIEW
        Map<String, Object> expectedState,
        Map<String, Object> observedState,
        String notes
) {
    public static ReconciliationResult match(Map<String, Object> expectedState, Map<String, Object> observedState, String notes) {
        return new ReconciliationResult(true, "RECONCILED", expectedState, observedState, notes);
    }

    public static ReconciliationResult mismatch(Map<String, Object> expectedState, Map<String, Object> observedState, String notes) {
        return new ReconciliationResult(false, "MISMATCH_MANUAL_REVIEW", expectedState, observedState, notes);
    }
}
