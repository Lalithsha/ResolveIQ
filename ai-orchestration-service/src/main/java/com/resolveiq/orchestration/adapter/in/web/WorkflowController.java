package com.resolveiq.orchestration.adapter.in.web;

import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowStep;
import com.resolveiq.orchestration.domain.repository.WorkflowInstanceRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowStepRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;

    public WorkflowController(WorkflowInstanceRepository instanceRepository, WorkflowStepRepository stepRepository) {
        this.instanceRepository = instanceRepository;
        this.stepRepository = stepRepository;
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
}
