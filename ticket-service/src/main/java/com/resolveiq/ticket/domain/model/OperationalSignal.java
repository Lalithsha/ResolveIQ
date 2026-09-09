package com.resolveiq.ticket.domain.model;

import java.time.Instant;
import java.util.Map;

public record OperationalSignal(
    String source, // e.g. "DEPLOYMENT_EVENT", "DATADOG_MONITOR", "KUBERNETES_EVENT"
    String externalId,
    String component,
    String environment,
    String description,
    Instant observedAt,
    Map<String, String> attributes,
    double correlationScore
) {}
