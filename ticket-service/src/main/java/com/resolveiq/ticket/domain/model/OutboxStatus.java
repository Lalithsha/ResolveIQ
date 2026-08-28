package com.resolveiq.ticket.domain.model;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    RETRY,
    DEAD
}
