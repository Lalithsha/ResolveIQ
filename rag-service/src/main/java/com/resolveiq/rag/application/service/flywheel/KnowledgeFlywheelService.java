package com.resolveiq.rag.application.service.flywheel;

import com.resolveiq.rag.application.dto.flywheel.FlywheelDtos.*;
import com.resolveiq.rag.domain.model.flywheel.*;
import com.resolveiq.rag.domain.repository.flywheel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class KnowledgeFlywheelService implements KnowledgeFlywheelServicePort {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFlywheelService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?:Bearer\\s+[a-zA-Z0-9_\\-\\.]+|sk-live-[a-zA-Z0-9]+)");

    private final KnowledgeCandidateRepository candidateRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final KnowledgeReleaseRepository releaseRepository;
    private final RollbackRecordRepository rollbackRecordRepository;
    private final com.resolveiq.rag.domain.repository.KnowledgeDocumentRepository documentRepository;
    private final com.resolveiq.rag.domain.repository.KnowledgeVersionRepository versionRepository;
    private final com.resolveiq.rag.application.port.KnowledgePublicationPort publicationService;
    private final com.resolveiq.rag.application.port.KnowledgeIndexingPort indexingService;
    private final KnowledgeEvaluatorPort evaluatorPort;

    public KnowledgeFlywheelService(KnowledgeCandidateRepository candidateRepository,
                                  EvaluationRunRepository evaluationRunRepository,
                                  KnowledgeReleaseRepository releaseRepository,
                                  RollbackRecordRepository rollbackRecordRepository,
                                  com.resolveiq.rag.domain.repository.KnowledgeDocumentRepository documentRepository,
                                  com.resolveiq.rag.domain.repository.KnowledgeVersionRepository versionRepository,
                                  com.resolveiq.rag.application.port.KnowledgePublicationPort publicationService,
                                  com.resolveiq.rag.application.port.KnowledgeIndexingPort indexingService,
                                  KnowledgeEvaluatorPort evaluatorPort) {
        this.candidateRepository = Objects.requireNonNull(candidateRepository, "candidateRepository cannot be null");
        this.evaluationRunRepository = Objects.requireNonNull(evaluationRunRepository, "evaluationRunRepository cannot be null");
        this.releaseRepository = Objects.requireNonNull(releaseRepository, "releaseRepository cannot be null");
        this.rollbackRecordRepository = Objects.requireNonNull(rollbackRecordRepository, "rollbackRecordRepository cannot be null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
        this.versionRepository = Objects.requireNonNull(versionRepository, "versionRepository cannot be null");
        this.publicationService = Objects.requireNonNull(publicationService, "publicationService cannot be null");
        this.indexingService = Objects.requireNonNull(indexingService, "indexingService cannot be null");
        this.evaluatorPort = Objects.requireNonNull(evaluatorPort, "evaluatorPort cannot be null");
    }

    public CandidateResponse createCandidate(UUID tenantId, CreateCandidateRequest request) {
        KnowledgeCandidate candidate = new KnowledgeCandidate(
            tenantId, request.title(), request.contentDraft(), request.category(),
            request.verifiedOutcomeScore(), request.distinctSourceCustomers()
        );
        candidate = candidateRepository.save(candidate);
        log.info("Knowledge candidate {} created with eligibility {}", candidate.getId(), candidate.getEligibilityStatus());
        return toCandidateResponse(candidate);
    }

    public CandidateResponse sanitizeCandidate(UUID tenantId, UUID candidateId) {
        KnowledgeCandidate candidate = candidateRepository.findByIdAndTenantId(candidateId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + candidateId));

        String sanitized = candidate.getContentDraft();
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[REDACTED_EMAIL]");
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("[REDACTED_TOKEN]");

        candidate.setSanitizedContent(sanitized);
        candidate.setSanitizationStatus(CandidateSanitizationStatus.SANITIZED);
        candidate.setContentHash(sha256(sanitized.getBytes(StandardCharsets.UTF_8)));
        candidate = candidateRepository.save(candidate);

        log.info("Knowledge candidate {} sanitized successfully. Hash: {}", candidateId, candidate.getContentHash());
        return toCandidateResponse(candidate);
    }

    public EvaluationRunResponse runEvaluation(UUID tenantId, UUID candidateId) {
        KnowledgeCandidate candidate = candidateRepository.findByIdAndTenantId(candidateId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + candidateId));

        if (candidate.getSanitizationStatus() != CandidateSanitizationStatus.SANITIZED) {
            throw new IllegalStateException("Candidate must be sanitized before running evaluation");
        }

        if (candidate.getEligibilityStatus() != CandidateEligibilityStatus.ELIGIBLE &&
            candidate.getEligibilityStatus() != CandidateEligibilityStatus.APPROVED) {
            throw new IllegalStateException("Candidate does not meet eligibility thresholds (requires score >= 80 and >= 5 distinct customers)");
        }

        String contentToEvaluate = candidate.getSanitizedContent() != null
            ? candidate.getSanitizedContent()
            : candidate.getContentDraft();

        // Run evaluator across frozen benchmark holdout cases (independent of candidate title keywords)
        KnowledgeEvaluatorPort.EvaluationResult evalResult = evaluatorPort.evaluateCandidate(
            tenantId, candidateId, candidate.getTitle(), contentToEvaluate, candidate.getCategory()
        );

        EvaluationRun run = new EvaluationRun(
            tenantId, candidateId, evalResult.datasetVersion(),
            evalResult.baselineRecallAt5(), evalResult.proposedRecallAt5(),
            evalResult.baselineMrr(), evalResult.proposedMrr(),
            evalResult.safetyCasesPassed(), evalResult.latencyP95Ratio()
        );
        run = evaluationRunRepository.save(run);

        if (run.isGatePassed()) {
            candidate.setEligibilityStatus(CandidateEligibilityStatus.APPROVED);
            candidateRepository.save(candidate);
            log.info("Evaluation run {} PASSED for candidate {}. Candidate marked APPROVED.", run.getId(), candidateId);
        } else {
            candidate.setEligibilityStatus(CandidateEligibilityStatus.REJECTED);
            candidateRepository.save(candidate);
            log.warn("Evaluation run {} FAILED for candidate {}.", run.getId(), candidateId);
        }

        return toEvaluationResponse(run);
    }

    public KnowledgeReleaseResponse releaseCandidate(UUID tenantId, UUID candidateId, UUID approverId,
                                                    Set<String> roles, Set<String> permissions) {
        boolean hasPermission = permissions.contains("KNOWLEDGE_RELEASE_APPROVE") ||
            roles.contains("ADMIN") || roles.contains("ROLE_ADMIN");
        if (!hasPermission) {
            throw new AccessDeniedException("Missing required permission: KNOWLEDGE_RELEASE_APPROVE");
        }

        KnowledgeCandidate candidate = candidateRepository.findByIdAndTenantId(candidateId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + candidateId));

        List<EvaluationRun> runs = evaluationRunRepository.findByTenantIdAndCandidateIdOrderByCreatedAtDesc(tenantId, candidateId);
        if (runs.isEmpty() || !runs.get(0).isGatePassed()) {
            throw new IllegalStateException("Cannot release candidate: no passing evaluation run found");
        }

        // Mark existing active release as SUPERSEDED
        releaseRepository.findByTenantIdAndStatus(tenantId, KnowledgeReleaseStatus.ACTIVE)
            .ifPresent(active -> {
                active.setStatus(KnowledgeReleaseStatus.SUPERSEDED);
                releaseRepository.save(active);
            });

        // Route activation through exact existing document reference if title matches, or create new
        String docTitle = candidate.getTitle() != null ? candidate.getTitle() : "Solution Article";
        com.resolveiq.rag.domain.model.KnowledgeDocument doc = documentRepository.findByTenantId(tenantId).stream()
            .filter(d -> docTitle.equalsIgnoreCase(d.getTitle()))
            .findFirst()
            .orElseGet(() -> {
                com.resolveiq.rag.domain.model.KnowledgeDocument newDoc = new com.resolveiq.rag.domain.model.KnowledgeDocument(
                    UUID.randomUUID(), tenantId, docTitle, candidate.getCategory(), "DEFAULT", "en"
                );
                return documentRepository.save(newDoc);
            });

        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(doc.getId())
            .map(v -> v.getVersionNumber() + 1)
            .orElse(1);

        String content = candidate.getSanitizedContent() != null ? candidate.getSanitizedContent() : candidate.getContentDraft();
        com.resolveiq.rag.domain.model.KnowledgeVersion ver = new com.resolveiq.rag.domain.model.KnowledgeVersion(
            doc.getId(), nextVersion, content, "Flywheel verified release", approverId
        );
        ver = versionRepository.save(ver);

        // Atomically index and activate
        indexingService.index(tenantId, doc.getId(), ver.getId());
        publicationService.activate(tenantId, doc.getId(), ver.getId(), approverId, "Flywheel publication");

        KnowledgeRelease release = new KnowledgeRelease(
            tenantId, candidateId, doc.getId(), nextVersion, runs.get(0).getId(),
            approverId, "Released verified solution for: " + candidate.getTitle()
        );
        release = releaseRepository.save(release);

        candidate.setEligibilityStatus(CandidateEligibilityStatus.RELEASED);
        candidateRepository.save(candidate);
        log.info("Knowledge release {} created for candidate {} by approver {}", release.getId(), candidateId, approverId);

        return toReleaseResponse(release);
    }

    public RollbackResponse rollbackRelease(UUID tenantId, UUID releaseId, UUID targetReleaseId,
                                           UUID performedBy, String reason,
                                           Set<String> roles, Set<String> permissions) {
        boolean hasPermission = permissions.contains("KNOWLEDGE_RELEASE_APPROVE") ||
            roles.contains("ADMIN") || roles.contains("ROLE_ADMIN");
        if (!hasPermission) {
            throw new AccessDeniedException("Missing required permission: KNOWLEDGE_RELEASE_APPROVE");
        }

        KnowledgeRelease current = releaseRepository.findByIdAndTenantId(releaseId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Release not found: " + releaseId));

        if (current.getStatus() != KnowledgeReleaseStatus.ACTIVE) {
            throw new IllegalStateException("Cannot rollback a release that is not currently ACTIVE");
        }

        current.setStatus(KnowledgeReleaseStatus.ROLLED_BACK);
        releaseRepository.save(current);

        if (targetReleaseId != null) {
            KnowledgeRelease target = releaseRepository.findByIdAndTenantId(targetReleaseId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Target release not found: " + targetReleaseId));
            target.setStatus(KnowledgeReleaseStatus.ACTIVE);
            releaseRepository.save(target);

            // Revert document and search index to target release version
            com.resolveiq.rag.domain.model.KnowledgeVersion targetVersion = versionRepository
                .findByDocumentIdOrderByVersionNumberDesc(target.getDocumentId()).stream()
                .filter(v -> v.getVersionNumber() == target.getVersionNumber())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Version not found for target release: " + target.getVersionNumber()));

            publicationService.rollback(tenantId, target.getDocumentId(), targetVersion.getId(), performedBy, "Flywheel rollback: " + reason);
            indexingService.index(tenantId, target.getDocumentId(), targetVersion.getId());
        }

        RollbackRecord record = new RollbackRecord(tenantId, releaseId, targetReleaseId, reason, performedBy);
        record = rollbackRecordRepository.save(record);
        log.info("Knowledge release {} rolled back to target {} by {}", releaseId, targetReleaseId, performedBy);

        return new RollbackResponse(
            record.getId(), record.getReleaseId(), record.getTargetReleaseId(),
            record.getReason(), record.getPerformedBy(), record.getOccurredAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> listCandidates(UUID tenantId) {
        return candidateRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .map(this::toCandidateResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidate(UUID tenantId, UUID candidateId) {
        KnowledgeCandidate candidate = candidateRepository.findByIdAndTenantId(candidateId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + candidateId));
        return toCandidateResponse(candidate);
    }

    @Transactional(readOnly = true)
    public EvaluationRunResponse getEvaluationRun(UUID tenantId, UUID runId) {
        EvaluationRun run = evaluationRunRepository.findByIdAndTenantId(runId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Evaluation run not found: " + runId));
        return toEvaluationResponse(run);
    }

    private CandidateResponse toCandidateResponse(KnowledgeCandidate c) {
        return new CandidateResponse(
            c.getId(), c.getTitle(), c.getContentDraft(), c.getSanitizedContent(),
            c.getCategory(), c.getEligibilityStatus().name(), c.getVerifiedOutcomeScore(),
            c.getDistinctSourceCustomers(), c.getSanitizationStatus().name(),
            c.getContentHash(), c.getCreatedAt()
        );
    }

    private EvaluationRunResponse toEvaluationResponse(EvaluationRun r) {
        return new EvaluationRunResponse(
            r.getId(), r.getCandidateId(), r.getDatasetVersion(),
            r.getBaselineRecallAt5(), r.getProposedRecallAt5(),
            r.getBaselineMrr(), r.getProposedMrr(),
            r.isSafetyCasesPassed(), r.getLatencyP95Ratio(),
            r.isGatePassed(), r.getCreatedAt()
        );
    }

    private KnowledgeReleaseResponse toReleaseResponse(KnowledgeRelease r) {
        return new KnowledgeReleaseResponse(
            r.getId(), r.getCandidateId(), r.getDocumentId(), r.getVersionNumber(),
            r.getStatus().name(), r.getEvaluationRunId(), r.getApproverId(),
            r.getReleaseNotes(), r.getReleasedAt()
        );
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "sha256-placeholder";
        }
    }
}
