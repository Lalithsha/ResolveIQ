package com.resolveiq.rag.flywheel;

import com.resolveiq.rag.application.dto.flywheel.FlywheelDtos.*;
import com.resolveiq.rag.application.service.flywheel.KnowledgeFlywheelService;
import com.resolveiq.rag.domain.model.flywheel.*;
import com.resolveiq.rag.domain.repository.flywheel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeFlywheelServiceTest {

    @Mock private KnowledgeCandidateRepository candidateRepository;
    @Mock private EvaluationRunRepository evaluationRunRepository;
    @Mock private KnowledgeReleaseRepository releaseRepository;
    @Mock private RollbackRecordRepository rollbackRecordRepository;

    private KnowledgeFlywheelService flywheelService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID approverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        flywheelService = new KnowledgeFlywheelService(
            candidateRepository, evaluationRunRepository, releaseRepository, rollbackRecordRepository
        );
    }

    @Test
    @DisplayName("Candidate meeting eligibility threshold (>=5 customers, score >=80) is marked ELIGIBLE")
    void candidateEligibilityThresholds() {
        when(candidateRepository.save(any(KnowledgeCandidate.class))).thenAnswer(i -> i.getArgument(0));

        CreateCandidateRequest eligibleReq = new CreateCandidateRequest(
            "SSO Fix", "Fix SSO cert", "AUTH", 85, 6
        );
        CandidateResponse eligibleRes = flywheelService.createCandidate(tenantId, eligibleReq);
        assertThat(eligibleRes.eligibilityStatus()).isEqualTo("ELIGIBLE");

        CreateCandidateRequest ineligibleReq = new CreateCandidateRequest(
            "Draft Fix", "Fix SSO cert", "AUTH", 70, 2
        );
        CandidateResponse ineligibleRes = flywheelService.createCandidate(tenantId, ineligibleReq);
        assertThat(ineligibleRes.eligibilityStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Sanitization removes email and Bearer secrets, sets SANITIZED and computes hash")
    void sanitizationRedactsPiiAndComputesHash() {
        KnowledgeCandidate candidate = new KnowledgeCandidate(
            tenantId, "SSO Guide", "Contact alice@example.com with Bearer eyJtoken123", "AUTH", 90, 5
        );
        when(candidateRepository.findByIdAndTenantId(candidateId, tenantId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(KnowledgeCandidate.class))).thenAnswer(i -> i.getArgument(0));

        CandidateResponse res = flywheelService.sanitizeCandidate(tenantId, candidateId);

        assertThat(res.sanitizationStatus()).isEqualTo("SANITIZED");
        assertThat(res.sanitizedContent()).doesNotContain("alice@example.com");
        assertThat(res.sanitizedContent()).doesNotContain("eyJtoken123");
        assertThat(res.sanitizedContent()).contains("[REDACTED_EMAIL]");
        assertThat(res.sanitizedContent()).contains("[REDACTED_TOKEN]");
        assertThat(res.contentHash()).isNotBlank();
    }

    @Test
    @DisplayName("Evaluation run tests Recall@5 >=0.85, MRR >=0.75 and marks candidate APPROVED on pass")
    void evaluationRunPassesThresholds() {
        KnowledgeCandidate candidate = new KnowledgeCandidate(
            tenantId, "SSO Guide", "Sanitized SSO Guide", "AUTH", 90, 5
        );
        candidate.setSanitizationStatus(CandidateSanitizationStatus.SANITIZED);
        candidate.setEligibilityStatus(CandidateEligibilityStatus.ELIGIBLE);
        when(candidateRepository.findByIdAndTenantId(candidateId, tenantId)).thenReturn(Optional.of(candidate));
        when(evaluationRunRepository.save(any(EvaluationRun.class))).thenAnswer(i -> i.getArgument(0));

        EvaluationRunResponse runRes = flywheelService.runEvaluation(tenantId, candidateId);

        assertThat(runRes.gatePassed()).isTrue();
        assertThat(runRes.proposedRecallAt5()).isGreaterThanOrEqualTo(0.85);
        assertThat(runRes.proposedMrr()).isGreaterThanOrEqualTo(0.75);
        assertThat(candidate.getEligibilityStatus()).isEqualTo(CandidateEligibilityStatus.APPROVED);
    }

    @Test
    @DisplayName("Release candidate requires KNOWLEDGE_RELEASE_APPROVE permission")
    void releaseRequiresApprovalPermission() {
        // Without permission -> AccessDeniedException
        assertThatThrownBy(() ->
            flywheelService.releaseCandidate(tenantId, candidateId, approverId, Set.of("AGENT"), Set.of())
        ).isInstanceOf(AccessDeniedException.class)
         .hasMessageContaining("KNOWLEDGE_RELEASE_APPROVE");

        // With permission
        KnowledgeCandidate candidate = new KnowledgeCandidate(tenantId, "SSO", "Content", "AUTH", 90, 5);
        when(candidateRepository.findByIdAndTenantId(candidateId, tenantId)).thenReturn(Optional.of(candidate));

        EvaluationRun run = new EvaluationRun(tenantId, candidateId, "v1", 0.82, 0.91, 0.72, 0.84, true, 1.05);
        when(evaluationRunRepository.findByTenantIdAndCandidateIdOrderByCreatedAtDesc(tenantId, candidateId))
            .thenReturn(List.of(run));
        when(releaseRepository.save(any(KnowledgeRelease.class))).thenAnswer(i -> i.getArgument(0));

        KnowledgeReleaseResponse release = flywheelService.releaseCandidate(
            tenantId, candidateId, approverId, Set.of("AGENT"), Set.of("KNOWLEDGE_RELEASE_APPROVE")
        );

        assertThat(release.status()).isEqualTo("ACTIVE");
        assertThat(candidate.getEligibilityStatus()).isEqualTo(CandidateEligibilityStatus.RELEASED);
    }

    @Test
    @DisplayName("Rollback reverts active release to ROLLED_BACK and creates audit record")
    void rollbackReleaseRevertsStatus() {
        KnowledgeRelease release = new KnowledgeRelease(
            tenantId, candidateId, UUID.randomUUID(), 1, UUID.randomUUID(), approverId, "Notes"
        );
        when(releaseRepository.findByIdAndTenantId(release.getId(), tenantId)).thenReturn(Optional.of(release));
        when(rollbackRecordRepository.save(any(RollbackRecord.class))).thenAnswer(i -> i.getArgument(0));

        RollbackResponse rollback = flywheelService.rollbackRelease(
            tenantId, release.getId(), null, approverId, "Regression in authentication queries",
            Set.of("ADMIN"), Set.of()
        );

        assertThat(release.getStatus()).isEqualTo(KnowledgeReleaseStatus.ROLLED_BACK);
        assertThat(rollback.reason()).contains("Regression");
    }
}
