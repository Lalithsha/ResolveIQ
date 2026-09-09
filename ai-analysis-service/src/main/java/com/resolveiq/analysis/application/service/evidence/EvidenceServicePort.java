package com.resolveiq.analysis.application.service.evidence;

import com.resolveiq.analysis.application.dto.EvidenceDtos.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EvidenceServicePort {

    EvidenceJobResponse createUploadSession(UUID tenantId, UUID ticketId, UUID attachmentId,
                                           String fileName, String mediaType, byte[] content,
                                           boolean consentGranted);

    EvidenceJobResponse updateConsent(UUID tenantId, UUID ticketId, UUID attachmentId, boolean consent);

    EvidenceJobResponse reprocess(UUID tenantId, UUID jobId);

    void tombstoneEvidence(UUID tenantId, UUID jobId);

    EvidenceJobResponse getJob(UUID tenantId, UUID jobId);

    List<EvidenceJobResponse> listByTicket(UUID tenantId, UUID ticketId);

    List<EvidenceArtifactResponse> getArtifacts(UUID tenantId, UUID jobId);

    List<EvidenceObservationResponse> getObservations(UUID tenantId, UUID jobId);

    String getContent(UUID tenantId, UUID jobId, String variant, Set<String> roles,
                     Set<String> permissions, String reason);
}
