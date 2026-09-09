package com.resolveiq.rag.application.service.flywheel;

import com.resolveiq.rag.application.dto.flywheel.FlywheelDtos.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface KnowledgeFlywheelServicePort {
    CandidateResponse createCandidate(UUID tenantId, CreateCandidateRequest request);
    CandidateResponse sanitizeCandidate(UUID tenantId, UUID candidateId);
    EvaluationRunResponse runEvaluation(UUID tenantId, UUID candidateId);
    KnowledgeReleaseResponse releaseCandidate(UUID tenantId, UUID candidateId, UUID approverId, Set<String> roles, Set<String> permissions);
    RollbackResponse rollbackRelease(UUID tenantId, UUID releaseId, UUID targetReleaseId, UUID performedBy, String reason, Set<String> roles, Set<String> permissions);
    List<CandidateResponse> listCandidates(UUID tenantId);
    CandidateResponse getCandidate(UUID tenantId, UUID candidateId);
    EvaluationRunResponse getEvaluationRun(UUID tenantId, UUID runId);
}
