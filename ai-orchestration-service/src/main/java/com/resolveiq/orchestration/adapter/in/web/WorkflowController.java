package com.resolveiq.orchestration.adapter.in.web;

import com.resolveiq.orchestration.application.service.TriageWorkflowOrchestrator;
import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowStep;
import com.resolveiq.orchestration.domain.repository.WorkflowInstanceRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final TriageWorkflowOrchestrator orchestrator;

    public WorkflowController(
        WorkflowInstanceRepository instanceRepository,
        WorkflowStepRepository stepRepository,
        TriageWorkflowOrchestrator orchestrator
    ) {
        this.instanceRepository = instanceRepository;
        this.stepRepository = stepRepository;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<WorkflowInstance> getWorkflowByTicket(@PathVariable("ticketId") UUID ticketId) {
        return instanceRepository.findByTicketId(ticketId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<WorkflowStep>> getWorkflowSteps(@PathVariable("id") UUID workflowId) {
        List<WorkflowStep> steps = stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
        return ResponseEntity.ok(steps);
    }

    @GetMapping("/failed")
    public ResponseEntity<List<WorkflowInstance>> listFailedWorkflows(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<WorkflowInstance> failedWorkflows = instanceRepository.findByTenantIdAndStatus(tenantId, "FAILED");
        return ResponseEntity.ok(failedWorkflows);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, Object>> retryWorkflow(
        @PathVariable("id") UUID workflowId,
        @RequestHeader(value = "X-Operator-Id", defaultValue = "system-ops") String operatorId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        WorkflowInstance instance = instanceRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found with id: " + workflowId));

        String reason = body != null ? body.getOrDefault("reason", "Operator manual retry") : "Operator manual retry";
        log.info("Audited Workflow DLQ Replay triggered by [{}] for workflow [{}] reason: [{}]", operatorId, workflowId, reason);

        instance.setStatus("RUNNING");
        instanceRepository.save(instance);

        return ResponseEntity.ok(Map.of(
            "workflowId", workflowId,
            "status", "RETRY_QUEUED",
            "operatorId", operatorId,
            "reason", reason
        ));
    }
}
