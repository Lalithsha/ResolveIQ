package com.resolveiq.ticket.domain.model;

public enum TicketStatus {
    NEW,
    TRIAGE_PENDING,
    TRIAGE_IN_PROGRESS,
    READY_FOR_AGENT,
    IN_PROGRESS,
    WAITING_ON_CUSTOMER,
    RESOLVED,
    CLOSED,
    TRIAGE_FAILED;

    public boolean canTransitionTo(TicketStatus target) {
        if (this == target) return true;
        return switch (this) {
            case NEW -> target == TRIAGE_PENDING || target == READY_FOR_AGENT || target == TRIAGE_FAILED;
            case TRIAGE_PENDING -> target == TRIAGE_IN_PROGRESS || target == READY_FOR_AGENT || target == TRIAGE_FAILED;
            case TRIAGE_IN_PROGRESS -> target == READY_FOR_AGENT || target == TRIAGE_FAILED;
            case TRIAGE_FAILED -> target == READY_FOR_AGENT || target == TRIAGE_PENDING;
            case READY_FOR_AGENT -> target == IN_PROGRESS || target == WAITING_ON_CUSTOMER || target == RESOLVED;
            case IN_PROGRESS -> target == WAITING_ON_CUSTOMER || target == RESOLVED || target == READY_FOR_AGENT;
            case WAITING_ON_CUSTOMER -> target == IN_PROGRESS || target == RESOLVED;
            case RESOLVED -> target == CLOSED || target == IN_PROGRESS; // Re-open allowed
            case CLOSED -> false; // Terminal state
        };
    }
}
