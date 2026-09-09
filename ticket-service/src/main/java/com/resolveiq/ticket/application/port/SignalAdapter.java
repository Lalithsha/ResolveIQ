package com.resolveiq.ticket.application.port;

import com.resolveiq.ticket.domain.model.OperationalSignal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SignalAdapter {
    List<OperationalSignal> fetchSignalsForWindow(UUID tenantId, String component, Instant windowStart, Instant windowEnd);
}
