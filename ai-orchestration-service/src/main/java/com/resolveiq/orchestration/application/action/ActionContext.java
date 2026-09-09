package com.resolveiq.orchestration.application.action;

import java.util.Set;
import java.util.UUID;

public record ActionContext(
        UUID tenantId,
        UUID ticketId,
        UUID proposalId,
        UUID actorId,
        String actorRole,
        Set<String> permissions
) {}
