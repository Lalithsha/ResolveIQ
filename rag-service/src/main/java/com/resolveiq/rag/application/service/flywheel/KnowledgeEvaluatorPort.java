package com.resolveiq.rag.application.service.flywheel;

import java.util.UUID;

public interface KnowledgeEvaluatorPort {

    record EvaluationResult(
        String datasetVersion,
        double baselineRecallAt5,
        double proposedRecallAt5,
        double baselineMrr,
        double proposedMrr,
        boolean safetyCasesPassed,
        double latencyP95Ratio
    ) {}

    EvaluationResult evaluateCandidate(UUID tenantId, UUID candidateId, String title, String content, String category);
}
