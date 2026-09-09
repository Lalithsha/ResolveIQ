package com.resolveiq.ticket.application.service.resolution;

import com.resolveiq.ticket.application.dto.resolution.ResolutionDtos.*;

import java.util.List;
import java.util.UUID;

public interface ResolutionServicePort {
    ResolutionAttemptResponse resolveTicket(UUID tenantId, UUID ticketId, UUID resolverId, String solutionFingerprint);
    ResolutionAttemptResponse recordOutcome(UUID tenantId, UUID ticketId, UUID customerId, String rating, String reason);
    ResolutionAttemptResponse reopenTicket(UUID tenantId, UUID ticketId, UUID customerId, String reason);
    List<ResolutionAttemptResponse> getResolutionHistory(UUID tenantId, UUID ticketId);
    ResolutionMetricsResponse getResolutionMetrics(UUID tenantId);
}
