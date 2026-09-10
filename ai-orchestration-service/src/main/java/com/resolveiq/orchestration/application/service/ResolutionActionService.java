package com.resolveiq.orchestration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.ActionEvents;
import com.resolveiq.orchestration.application.action.*;
import com.resolveiq.orchestration.application.action.digest.ActionDigestService;
import com.resolveiq.orchestration.application.action.policy.ActionPolicyEngine;
import com.resolveiq.orchestration.application.dto.ResolutionActionDtos.*;
import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.orchestration.domain.model.action.*;
import com.resolveiq.orchestration.domain.repository.*;
import com.resolveiq.security.TrustedPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResolutionActionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionActionService.class);

    private final ResolutionActionProposalRepository proposalRepository;
    private final ActionPolicyDecisionRepository policyDecisionRepository;
    private final ActionApprovalRepository approvalRepository;
    private final ActionExecutionRepository executionRepository;
    private final ActionReconciliationRepository reconciliationRepository;
    private final WorkflowOutboxRepository outboxRepository;
    private final ActionRegistry actionRegistry;
    private final ActionDigestService digestService;
    private final ActionPolicyEngine policyEngine;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactions;

    public ResolutionActionService(
            ResolutionActionProposalRepository proposalRepository,
            ActionPolicyDecisionRepository policyDecisionRepository,
            ActionApprovalRepository approvalRepository,
            ActionExecutionRepository executionRepository,
            ActionReconciliationRepository reconciliationRepository,
            WorkflowOutboxRepository outboxRepository,
            ActionRegistry actionRegistry,
            ActionDigestService digestService,
            ActionPolicyEngine policyEngine,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.proposalRepository = proposalRepository;
        this.policyDecisionRepository = policyDecisionRepository;
        this.approvalRepository = approvalRepository;
        this.executionRepository = executionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.outboxRepository = outboxRepository;
        this.actionRegistry = actionRegistry;
        this.digestService = digestService;
        this.policyEngine = policyEngine;
        this.objectMapper = objectMapper;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public ActionProposalResponse proposeAction(UUID tenantId, UUID ticketId, ProposeActionRequest request, TrustedPrincipal principal) {
        if (principal == null || principal.userId() == null || principal.tenantId() == null || !tenantId.equals(principal.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated tenant actor is required");
        }
        ActionType type = request.actionType();
        ResolutionAction action = actionRegistry.getAction(type);

        // 1. Parse typed input
        Object typedInput = objectMapper.convertValue(request.input(), action.inputType());
        Map<String, Object> normalizedMap = objectMapper.convertValue(typedInput, Map.class);
        String inputPayloadJson;
        try {
            inputPayloadJson = objectMapper.writeValueAsString(normalizedMap);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize normalized action input", e);
        }

        String inputHash = digestService.computeInputHash(inputPayloadJson);

        // 2. Check for active proposal deduplication
        Optional<ResolutionActionProposal> existingProposal = proposalRepository.findByTenantIdAndInputHash(tenantId, inputHash);
        if (existingProposal.isPresent()) {
            ResolutionActionProposal existing = existingProposal.get();
            if (!existing.isExpired() && (
                    existing.getStatus() == ActionStatus.PROPOSED ||
                    existing.getStatus() == ActionStatus.AWAITING_APPROVAL ||
                    existing.getStatus() == ActionStatus.APPROVED ||
                    existing.getStatus() == ActionStatus.EXECUTING)) {
                log.info("Returning existing active action proposal {} for input hash {}", existing.getId(), inputHash);
                return mapToProposalResponse(existing);
            }
        }

        // 3. Fetch authoritative target current state
        ActionContext context = new ActionContext(tenantId, ticketId, null, principal.userId(),
                !principal.roles().isEmpty() ? principal.roles().iterator().next() : "UNKNOWN",
                principal.permissions());

        CurrentState currentState = action.fetchCurrentState(context, typedInput);

        // 4. Precondition validation
        ValidationResult validation = action.validate(context, typedInput, currentState);

        // 5. Policy Engine evaluation
        ActionPolicyEngine.PolicyEvaluation policyEval = policyEngine.evaluate(
                type,
                validation.riskLevel(),
                normalizedMap,
                validation.isValid(),
                validation.reasonCodes()
        );

        // 6. Compute versioned canonical digest v1
        UUID proposalId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        Map<String, Object> approvalReqs = Map.of(
                "requiredApprovalCount", policyEval.requiredApprovalCount(),
                "requiredPermissions", policyEval.requiredPermissions()
        );

        ActionDigestService.CanonicalDigestResult digestResult = digestService.generateDigestV1(
                tenantId,
                ticketId,
                proposalId,
                type.name(),
                action.extractTarget(typedInput),
                normalizedMap,
                currentState.stateVersion(),
                ActionPolicyEngine.POLICY_VERSION,
                approvalReqs,
                expiresAt
        );

        // 7. Persist proposal
        ResolutionActionProposal proposal = new ResolutionActionProposal(
                tenantId,
                ticketId,
                type,
                inputPayloadJson,
                inputHash,
                digestResult.digestHex(),
                digestResult.canonicalBytes(),
                request.aiRationale(),
                request.evidenceIds(),
                expiresAt
        );
        // Reflect pre-generated ID
        try {
            var idField = ResolutionActionProposal.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(proposal, proposalId);
        } catch (Exception ignored) {}

        proposal.setRiskLevel(validation.riskLevel());
        proposal.setCurrentStateVersion(currentState.stateVersion());
        proposal.setPolicyVersion(ActionPolicyEngine.POLICY_VERSION);
        if (principal != null) {
            proposal.setProposerId(principal.userId());
        }

        if (policyEval.decision() == PolicyDecision.DENIED) {
            proposal.setStatus(ActionStatus.POLICY_DENIED);
        } else if (policyEval.decision() == PolicyDecision.REQUIRES_APPROVAL) {
            proposal.setStatus(ActionStatus.AWAITING_APPROVAL);
        } else {
            proposal.setStatus(ActionStatus.APPROVED);
        }

        proposal = proposalRepository.save(proposal);

        // 8. Persist policy decision
        try {
            String matchedRulesJson = objectMapper.writeValueAsString(policyEval.matchedRules());
            String reqPermissionsJson = objectMapper.writeValueAsString(policyEval.requiredPermissions());
            String reasonCodesJson = objectMapper.writeValueAsString(policyEval.reasonCodes());

            ActionPolicyDecision decision = new ActionPolicyDecision(
                    proposal.getId(),
                    tenantId,
                    policyEval.decision(),
                    matchedRulesJson,
                    reqPermissionsJson,
                    policyEval.requiredApprovalCount(),
                    policyEval.financialLimitCents(),
                    reasonCodesJson
            );
            policyDecisionRepository.save(decision);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist policy decision", e);
        }

        // 9. Publish ActionProposed event via outbox
        publishOutboxEvent(tenantId, proposal.getId(), ActionEvents.ACTION_PROPOSED,
                new ActionEvents.ActionProposedPayload(
                        proposal.getId(),
                        ticketId,
                        tenantId,
                        type.name(),
                        proposal.getRiskLevel().name(),
                        proposal.getCanonicalDigest(),
                        proposal.getExpiresAt()
                ));

        PolicyDecisionDto policyDto = new PolicyDecisionDto(
                policyEval.decision().name(),
                policyEval.matchedRules(),
                policyEval.requiredPermissions(),
                policyEval.requiredApprovalCount(),
                policyEval.financialLimitCents(),
                policyEval.reasonCodes(),
                Instant.now()
        );

        log.info("Action proposed: id={}, type={}, status={}, digest={}",
                proposal.getId(), proposal.getActionType(), proposal.getStatus(), proposal.getCanonicalDigest());

        return mapToProposalResponse(proposal, policyDto);
    }

    @Transactional(readOnly = true)
    public List<ActionProposalResponse> getProposalsForTicket(UUID tenantId, UUID ticketId) {
        return proposalRepository.findByTenantIdAndTicketIdOrderByCreatedAtDesc(tenantId, ticketId)
                .stream()
                .map(this::mapToProposalResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActionProposalResponse getProposal(UUID tenantId, UUID proposalId) {
        ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Action proposal not found: " + proposalId));
        return mapToProposalResponse(proposal);
    }

    @Transactional(readOnly = true)
    public List<ActionExecutionResponse> getExecutionsForProposal(UUID tenantId, UUID proposalId) {
        return executionRepository.findByProposalIdAndTenantIdOrderByAttemptNumberAsc(proposalId, tenantId)
                .stream()
                .map(this::mapToExecutionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActionProposalResponse approveAction(UUID tenantId, UUID proposalId, ApproveActionRequest request, TrustedPrincipal principal) {
        if (principal == null || principal.userId() == null || principal.tenantId() == null || !tenantId.equals(principal.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated tenant actor is required");
        }
        ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Action proposal not found: " + proposalId));

        if (proposal.isExpired()) {
            proposal.setStatus(ActionStatus.EXPIRED);
            proposalRepository.save(proposal);
            throw new IllegalStateException("Action proposal has expired and cannot be approved");
        }

        if (proposal.getStatus() != ActionStatus.PROPOSED && proposal.getStatus() != ActionStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("Proposal cannot be approved in current status: " + proposal.getStatus());
        }

        // Digest check
        if (request.approvedDigest() == null || !proposal.getCanonicalDigest().equalsIgnoreCase(request.approvedDigest().trim())) {
            throw new IllegalArgumentException("Approved digest mismatch. Proposal has been altered or updated.");
        }

        // Permission check
        ActionPolicyDecision decision = policyDecisionRepository.findByProposalIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new IllegalStateException("Policy decision missing for proposal: " + proposalId));

        List<String> requiredPermissions = parseStringList(decision.getRequiredPermissions());
        if (principal != null && !requiredPermissions.isEmpty()) {
            boolean hasPermission = requiredPermissions.stream().anyMatch(p -> principal.permissions().contains(p));
            if (!hasPermission) {
                throw new SecurityException("User lacks required permission for action approval: " + requiredPermissions);
            }
        }

        // Proposer exclusion & recent authentication check for high-risk / financial actions
        if (proposal.getRiskLevel() == RiskLevel.HIGH || proposal.getRiskLevel() == RiskLevel.CRITICAL) {
            UUID actorIdCheck = principal != null ? principal.userId() : null;
            if (actorIdCheck != null && actorIdCheck.equals(proposal.getProposerId())) {
                throw new IllegalStateException("Two-person rule violation: proposer cannot approve own high-risk action");
            }
            if (principal == null || principal.authTime() == null) {
                throw new SecurityException("Missing authentication time is a denial for high-risk action approval");
            }
            if (!principal.isRecentAuthentication(300L)) {
                throw new SecurityException("High-risk action approval requires recent re-authentication (within 5 minutes)");
            }
        }

        // Check duplicate approval by same actor
        UUID actorId = principal != null ? principal.userId() : UUID.randomUUID();
        String actorRole = principal != null && !principal.roles().isEmpty() ? principal.roles().iterator().next() : "AGENT";
        Optional<ActionApproval> existingApproval = approvalRepository.findByProposalIdAndActorIdAndApprovedDigest(
                proposalId, actorId, request.approvedDigest().trim());
        if (existingApproval.isPresent()) {
            throw new IllegalStateException("Actor has already approved this proposal digest");
        }

        // Save approval
        ActionApproval approval = new ActionApproval(
                proposalId,
                tenantId,
                actorId,
                actorRole,
                "APPROVED",
                request.approvedDigest().trim(),
                principal != null ? principal.authTime() : Instant.now(),
                request.comment()
        );
        approvalRepository.save(approval);

        // Count approvals
        List<ActionApproval> allApprovals = approvalRepository.findByProposalIdAndTenantIdOrderByCreatedAtDesc(proposalId, tenantId);
        long matchingApprovals = allApprovals.stream()
                .filter(a -> "APPROVED".equals(a.getDecision()) && a.getApprovedDigest().equalsIgnoreCase(proposal.getCanonicalDigest()))
                .map(ActionApproval::getActorId)
                .distinct()
                .count();

        if (matchingApprovals >= decision.getRequiredApprovalCount()) {
            proposal.setStatus(ActionStatus.APPROVED);
            proposalRepository.save(proposal);

            publishOutboxEvent(tenantId, proposal.getId(), ActionEvents.ACTION_APPROVED,
                    new ActionEvents.ActionApprovedPayload(
                            proposal.getId(),
                            proposal.getTicketId(),
                            tenantId,
                            actorId,
                            proposal.getCanonicalDigest(),
                            Instant.now()
                    ));
            log.info("Action proposal {} is now APPROVED ({} approvals recorded)", proposalId, matchingApprovals);
        }

        return mapToProposalResponse(proposal);
    }

    @Transactional
    public ActionProposalResponse rejectAction(UUID tenantId, UUID proposalId, RejectActionRequest request, TrustedPrincipal principal) {
        if (principal == null || principal.userId() == null || principal.tenantId() == null || !tenantId.equals(principal.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated tenant actor is required");
        }
        ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Action proposal not found: " + proposalId));

        proposal.setStatus(ActionStatus.REJECTED);
        proposalRepository.save(proposal);

        UUID actorId = principal != null ? principal.userId() : UUID.randomUUID();
        String actorRole = principal != null && !principal.roles().isEmpty() ? principal.roles().iterator().next() : "AGENT";
        ActionApproval rejection = new ActionApproval(
                proposalId,
                tenantId,
                actorId,
                actorRole,
                "REJECTED",
                proposal.getCanonicalDigest(),
                principal != null ? principal.authTime() : Instant.now(),
                request.reason()
        );
        approvalRepository.save(rejection);

        log.info("Action proposal {} was REJECTED by actor {}", proposalId, actorId);
        return mapToProposalResponse(proposal);
    }

    @Transactional
    public ActionExecutionResponse executeAction(UUID tenantId, UUID proposalId, ExecuteActionRequest request, TrustedPrincipal principal) {
        if (principal == null || principal.userId() == null || principal.tenantId() == null || !tenantId.equals(principal.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Authenticated tenant actor is required");
        }
        if (request.expectedVersion() == null) {
            throw new IllegalArgumentException("expectedVersion is mandatory for action execution");
        }

        // Fetch proposal first (must exist before checking cached results)
        ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Action proposal not found: " + proposalId));

        // Two-person rule enforcement: Proposer cannot execute their own high-risk proposal
        if (principal != null && proposal.getProposerId() != null && proposal.getProposerId().equals(principal.userId())) {
            throw new org.springframework.security.access.AccessDeniedException("Two-person rule violation: Proposer cannot execute own high-risk proposal");
        }

        // 5-minute step-up authentication check
        if (!principal.isRecentAuthentication(300L)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Step-up authentication required: auth_time must be present and within 5 minutes");
        }

        String idempotencyKey = request.idempotencyKey() != null && !request.idempotencyKey().isBlank()
                ? request.idempotencyKey().trim()
                : "exec_" + proposalId + "_" + request.expectedVersion();

        // 1. Check if execution already exists with this idempotency key
        Optional<ActionExecution> existingExec = executionRepository.findByTenantIdAndClientIdempotencyKey(tenantId, idempotencyKey);
        if (existingExec.isPresent()) {
            ActionExecution exec = existingExec.get();
            if (!exec.getProposalId().equals(proposalId)) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Idempotency conflict: key was previously used with a different proposal: " + exec.getProposalId()
                );
            }
            if (exec.getActorId() == null || !exec.getActorId().equals(principal.userId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Idempotency conflict: key belongs to another actor"
                );
            }
            if (exec.getProvider() != null && !exec.getProvider().equalsIgnoreCase(proposal.getActionType().name())) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Idempotency conflict: key was previously used with a different action type"
                );
            }
            String reqHash = digestService.computeInputHash(proposal.getInputPayload());
            if (!exec.getRequestHash().equals(reqHash)) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Idempotency conflict: key was previously used with different request parameters"
                );
            }
            log.info("Returning existing execution {} for idempotency key {}", exec.getId(), idempotencyKey);
            return mapToExecutionResponse(exec);
        }

        String requestHash = digestService.computeInputHash(proposal.getInputPayload());

        if (proposal.isExpired()) {
            proposal.setStatus(ActionStatus.EXPIRED);
            proposalRepository.save(proposal);
            throw new IllegalStateException("Action proposal has expired");
        }

        if (proposal.getStatus() != ActionStatus.APPROVED) {
            throw new IllegalStateException("Proposal cannot be executed. Required status: APPROVED, current: " + proposal.getStatus());
        }

        if (!proposal.getVersion().equals(request.expectedVersion())) {
            throw new IllegalStateException("Optimistic concurrency conflict: proposal version changed concurrently");
        }

        if (request.approvedDigest() == null || !proposal.getCanonicalDigest().equalsIgnoreCase(request.approvedDigest().trim())) {
            throw new IllegalArgumentException("Approved digest mismatch on execution submission");
        }

        // Re-validate current authoritative target state before durable admission
        ResolutionAction action = actionRegistry.getAction(proposal.getActionType());
        Object typedInput;
        try {
            typedInput = objectMapper.readValue(proposal.getInputPayload(), action.inputType());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize proposal input", e);
        }

        ActionContext context = new ActionContext(tenantId, proposal.getTicketId(), proposalId,
                principal != null ? principal.userId() : null,
                principal != null && !principal.roles().isEmpty() ? principal.roles().iterator().next() : "AGENT",
                principal != null ? principal.permissions() : Set.of());

        CurrentState freshState = action.fetchCurrentState(context, typedInput);
        if (!freshState.stateVersion().equals(proposal.getCurrentStateVersion())) {
            proposal.setStatus(ActionStatus.POLICY_DENIED);
            proposalRepository.save(proposal);
            throw new IllegalStateException("Authoritative target state version changed since proposal was approved. Re-evaluation required.");
        }

        // Mark proposal as EXECUTING and persist a durable queued intent. Provider I/O is performed by the worker.
        proposal.setStatus(ActionStatus.EXECUTING);
        proposalRepository.save(proposal);

        // 5. Create Execution entity
        ActionExecution execution = new ActionExecution(
                proposalId,
                tenantId,
                1,
                proposal.getActionType().name(),
                idempotencyKey,
                digestService.computeInputHash(proposal.getInputPayload())
        );
        execution.setActorId(principal.userId());
        execution.setProviderIdempotencyKey("riq_" + digestService.computeInputHash(
            tenantId + ":" + proposalId + ":" + requestHash).substring(0, 48));
        execution = executionRepository.save(execution);

        return mapToExecutionResponse(execution);
    }

    @Scheduled(fixedDelayString = "${resolveiq.actions.worker-delay-ms:1000}")
    public void processPendingExecutions() {
        for (UUID executionId : claimExecutions()) {
            try {
                processClaimedExecution(executionId);
            } catch (Exception error) {
                log.error("Action execution worker failed for {} and will leave a recoverable lease", executionId, error);
            }
        }
    }

    public List<UUID> claimExecutions() {
        return transactions.execute(status -> {
            Instant now = Instant.now();
            List<ActionExecution> claimed = executionRepository.lockRecoverable(now, PageRequest.of(0, 20));
            claimed.forEach(execution -> {
                if ("FAILED_RETRYABLE".equals(execution.getStatus())) {
                    execution.setAttemptNumber(execution.getAttemptNumber() + 1);
                }
                execution.setStatus("CLAIMED");
                execution.setLeaseExpiresAt(now.plusSeconds(30));
            });
            executionRepository.saveAll(claimed);
            return claimed.stream().map(ActionExecution::getId).toList();
        });
    }

    public void processClaimedExecution(UUID executionId) {
        ActionExecution execution = transactions.execute(status -> executionRepository.findById(executionId)
            .filter(e -> "CLAIMED".equals(e.getStatus()))
            .orElse(null));
        if (execution == null) return;

        ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(execution.getProposalId(), execution.getTenantId())
            .orElseThrow(() -> new IllegalStateException("Action proposal disappeared: " + execution.getProposalId()));
        ResolutionAction action = actionRegistry.getAction(proposal.getActionType());
        Object typedInput;
        try {
            typedInput = objectMapper.readValue(proposal.getInputPayload(), action.inputType());
        } catch (Exception error) {
            completeExecution(executionId, null, null, "FAILED_FINAL", error.getMessage());
            return;
        }

        ActionContext context = new ActionContext(proposal.getTenantId(), proposal.getTicketId(), proposal.getId(),
            proposal.getProposerId(), "SYSTEM_WORKER", Set.of());
        ExecutionResult result;
        ReconciliationResult reconciliation = null;
        try {
            result = action.execute(context, typedInput, execution.getProviderIdempotencyKey());
            if ("SUCCEEDED".equals(result.status())) reconciliation = action.reconcile(context, typedInput, result);
        } catch (Exception error) {
            result = new ExecutionResult("EXECUTION_UNKNOWN", null, Map.of(), error.getMessage());
        }
        completeExecution(executionId, result, reconciliation, result.status(), result.errorMessage());
    }

    private void completeExecution(UUID executionId, ExecutionResult result, ReconciliationResult reconciliation,
                                   String resultStatus, String errorMessage) {
        transactions.executeWithoutResult(status -> {
            ActionExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution disappeared: " + executionId));
            ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(execution.getProposalId(), execution.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Proposal disappeared: " + execution.getProposalId()));
            execution.setStatus(resultStatus);
            execution.setLeaseExpiresAt(null);
            execution.setErrorMessage(errorMessage);
            if (result != null) {
                execution.setProviderReference(result.providerReference());
                try { execution.setSanitizedResponse(objectMapper.writeValueAsString(result.responsePayload())); }
                catch (Exception error) { throw new IllegalStateException("Cannot persist sanitized provider response", error); }
            }
            execution.setCompletedAt(Instant.now());
            executionRepository.save(execution);

            ActionReconciliation saved = null;
            if (reconciliation != null) {
                try {
                    saved = reconciliationRepository.save(new ActionReconciliation(proposal.getId(), execution.getId(),
                        proposal.getTenantId(), reconciliation.status(), objectMapper.writeValueAsString(reconciliation.expectedState()),
                        objectMapper.writeValueAsString(reconciliation.observedState()), reconciliation.notes()));
                } catch (Exception error) {
                    throw new IllegalStateException("Reconciliation persistence failed", error);
                }
            }
            if (reconciliation != null && reconciliation.isMatch()) proposal.setStatus(ActionStatus.RECONCILED);
            else if ("EXECUTION_UNKNOWN".equals(resultStatus)) proposal.setStatus(ActionStatus.EXECUTION_UNKNOWN);
            else if ("FAILED_RETRYABLE".equals(resultStatus)) proposal.setStatus(ActionStatus.FAILED_RETRYABLE);
            else if (!"SUCCEEDED".equals(resultStatus)) proposal.setStatus(ActionStatus.FAILED_FINAL);
            else proposal.setStatus(ActionStatus.MANUAL_REVIEW);
            proposalRepository.save(proposal);
            publishOutboxEvent(proposal.getTenantId(), proposal.getId(), ActionEvents.ACTION_EXECUTION_COMPLETED,
                new ActionEvents.ActionExecutionCompletedPayload(execution.getId(), proposal.getId(), proposal.getTicketId(),
                    proposal.getTenantId(), proposal.getActionType().name(), execution.getStatus(), execution.getProviderReference(),
                    saved != null ? saved.getStatus() : "NOT_RECONCILED", execution.getErrorMessage(), Instant.now()));
        });
    }

    @Transactional
    public CompensationResponse compensateAction(UUID tenantId, UUID proposalId) {
        ResolutionActionProposal proposal = proposalRepository.findByIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Action proposal not found: " + proposalId));

        ResolutionAction action = actionRegistry.getAction(proposal.getActionType());
        Object typedInput;
        try {
            typedInput = objectMapper.readValue(proposal.getInputPayload(), action.inputType());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize proposal input", e);
        }

        ActionContext context = new ActionContext(tenantId, proposal.getTicketId(), proposalId, null, "SYSTEM", Set.of());
        CompensationResult compResult = action.compensate(context, typedInput, null);

        return new CompensationResponse(compResult.isSupported(), compResult.status(), compResult.reason());
    }

    private void publishOutboxEvent(UUID tenantId, UUID aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            WorkflowOutboxEvent event = new WorkflowOutboxEvent("RESOLUTION_ACTION", aggregateId, eventType, payloadJson);
            outboxRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to enqueue outbox event for action aggregate {}", aggregateId, e);
        }
    }

    private ActionProposalResponse mapToProposalResponse(ResolutionActionProposal proposal) {
        return mapToProposalResponse(proposal, null);
    }

    private ActionProposalResponse mapToProposalResponse(ResolutionActionProposal proposal, PolicyDecisionDto policyDto) {
        Map<String, Object> inputMap = Map.of();
        try {
            inputMap = objectMapper.readValue(proposal.getInputPayload(), Map.class);
        } catch (Exception ignored) {}

        if (policyDto == null) {
            Optional<ActionPolicyDecision> decisionOpt = policyDecisionRepository.findFirstByProposalIdOrderByEvaluatedAtDesc(proposal.getId());
            if (decisionOpt.isPresent()) {
                ActionPolicyDecision d = decisionOpt.get();
                policyDto = new PolicyDecisionDto(
                        d.getDecision().name(),
                        parseStringList(d.getMatchedRules()),
                        parseStringList(d.getRequiredPermissions()),
                        d.getRequiredApprovalCount(),
                        d.getFinancialLimitCents(),
                        parseStringList(d.getReasonCodes()),
                        d.getEvaluatedAt()
                );
            }
        }

        List<ActionApprovalDto> approvalDtos = approvalRepository.findByProposalIdAndTenantIdOrderByCreatedAtDesc(proposal.getId(), proposal.getTenantId())
                .stream()
                .map(a -> new ActionApprovalDto(
                        a.getId(),
                        a.getActorId(),
                        a.getActorRole(),
                        a.getDecision(),
                        a.getApprovedDigest(),
                        a.getAuthenticationTime(),
                        a.getComment(),
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new ActionProposalResponse(
                proposal.getId(),
                proposal.getTenantId(),
                proposal.getTicketId(),
                proposal.getActionType(),
                proposal.getStatus(),
                proposal.getRiskLevel(),
                inputMap,
                proposal.getInputHash(),
                proposal.getCanonicalDigest(),
                proposal.getCanonicalBytes(),
                proposal.getAiRationale(),
                proposal.getEvidenceIds(),
                policyDto,
                approvalDtos,
                proposal.getVersion(),
                proposal.getExpiresAt(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }

    private ActionExecutionResponse mapToExecutionResponse(ActionExecution execution) {
        Map<String, Object> respMap = Map.of();
        if (execution.getSanitizedResponse() != null) {
            try {
                respMap = objectMapper.readValue(execution.getSanitizedResponse(), Map.class);
            } catch (Exception ignored) {}
        }

        ActionReconciliationDto reconDto = null;
        Optional<ActionReconciliation> reconOpt = reconciliationRepository.findByExecutionIdAndTenantId(execution.getId(), execution.getTenantId());
        if (reconOpt.isPresent()) {
            ActionReconciliation r = reconOpt.get();
            Map<String, Object> exp = Map.of();
            Map<String, Object> obs = Map.of();
            try {
                exp = objectMapper.readValue(r.getExpectedState(), Map.class);
                obs = objectMapper.readValue(r.getObservedState(), Map.class);
            } catch (Exception ignored) {}
            reconDto = new ActionReconciliationDto(r.getId(), r.getStatus(), exp, obs, r.getNotes(), r.getReconciledAt());
        }

        return new ActionExecutionResponse(
                execution.getId(),
                execution.getProposalId(),
                execution.getStatus(),
                execution.getProvider(),
                execution.getProviderReference(),
                respMap,
                execution.getErrorMessage(),
                reconDto,
                execution.getStartedAt(),
                execution.getCompletedAt()
        );
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
