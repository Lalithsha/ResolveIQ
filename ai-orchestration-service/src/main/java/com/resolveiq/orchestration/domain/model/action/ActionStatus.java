package com.resolveiq.orchestration.domain.model.action;

public enum ActionStatus {
    DRAFT,
    PROPOSED,
    POLICY_DENIED,
    AWAITING_APPROVAL,
    APPROVED,
    EXECUTING,
    SUCCEEDED,
    RECONCILED,
    REJECTED,
    EXPIRED,
    EXECUTION_UNKNOWN,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    MANUAL_REVIEW
}
