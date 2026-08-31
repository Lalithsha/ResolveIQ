package com.resolveiq.orchestration.adapter.in.web;

import com.resolveiq.orchestration.application.service.TriageWorkflowOrchestrator;
import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowStep;
import com.resolveiq.orchestration.domain.repository.WorkflowInstanceRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowStepRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final TriageWorkflowOrchestrator orchestrator;
    private final WorkflowOutboxRepository outboxRepository;

    public WorkflowController(
        WorkflowInstanceRepository instanceRepository,
        WorkflowStepRepository stepRepository,
        TriageWorkflowOrchestrator orchestrator,
        WorkflowOutboxRepository outboxRepository
    ) {
        this.instanceRepository = instanceRepository;
        this.stepRepository = stepRepository;
        this.orchestrator = orchestrator;
        this.outboxRepository = outboxRepository;
    }

    @GetMapping("/operations/outbox-summary")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<Map<String, Long>> outboxSummary(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        Map<String, Long> result = new java.util.LinkedHashMap<>(Map.of("PENDING", 0L, "RETRY", 0L, "DEAD", 0L, "PUBLISHED", 0L));
        outboxRepository.countByStatusForTenant(tenantId)
            .forEach(row -> result.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<WorkflowInstance> getWorkflowByTicket(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable(value = "ticketId") UUID ticketId
    ) {
        return instanceRepository.findByTicketIdAndTenantId(ticketId, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<WorkflowStep>> getWorkflowSteps(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable(value = "id") UUID workflowId
    ) {
        instanceRepository.findByIdAndTenantId(workflowId, tenantId).orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
        return ResponseEntity.ok(steps);
    }

    @GetMapping("/failed")
    public ResponseEntity<List<WorkflowInstance>> listFailedWorkflows(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId
    ) {
        List<WorkflowInstance> failedWorkflows = instanceRepository.findByTenantIdAndStatus(tenantId, "FAILED");
        return ResponseEntity.ok(failedWorkflows);
    }

    @PostMapping("/{id}/retry")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryWorkflow(
        @PathVariable(value = "id") UUID workflowId,
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @RequestHeader(value = "X-User-Id") UUID operatorId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        if (instanceRepository.findByIdAndTenantId(workflowId, tenantId).isEmpty()) {
            throw new IllegalArgumentException("Workflow not found with id: " + workflowId);
        }

        String reason = body != null ? body.getOrDefault("reason", "Operator manual retry") : "Operator manual retry";
        log.info("Audited Workflow DLQ Replay triggered by [{}] for workflow [{}] reason: [{}]", operatorId, workflowId, reason);

        orchestrator.retryTriageWorkflow(workflowId);

        return ResponseEntity.ok(Map.of(
            "workflowId", workflowId,
            "status", "RETRY_EXECUTED",
            "operatorId", operatorId,
            "reason", reason
        ));
    }
}
