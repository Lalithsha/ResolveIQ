package com.resolveiq.ticket.domain.model;

public enum IncidentStatus {
    PROPOSED,
    INVESTIGATING,
    IDENTIFIED,
    MONITORING,
    RESOLVED,
    DISMISSED;

    public boolean canTransitionTo(IncidentStatus next) {
        if (this == next) return true;
        return switch (this) {
            case PROPOSED -> next == INVESTIGATING || next == DISMISSED;
            case INVESTIGATING -> next == IDENTIFIED || next == MONITORING || next == RESOLVED || next == DISMISSED;
            case IDENTIFIED -> next == MONITORING || next == RESOLVED;
            case MONITORING -> next == RESOLVED || next == IDENTIFIED;
            case RESOLVED -> false;
            case DISMISSED -> false;
        };
    }
}
