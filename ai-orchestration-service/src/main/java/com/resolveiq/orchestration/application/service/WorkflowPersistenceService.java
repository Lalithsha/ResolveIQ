package com.resolveiq.orchestration.application.service;

import com.resolveiq.orchestration.domain.model.WorkflowAttempt;
import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.orchestration.domain.model.WorkflowStep;
import com.resolveiq.orchestration.domain.repository.WorkflowAttemptRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowInstanceRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowOutboxRepository;
import com.resolveiq.orchestration.domain.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkflowPersistenceService {
    private final WorkflowInstanceRepository instances;
    private final WorkflowStepRepository steps;
    private final WorkflowAttemptRepository attempts;
    private final WorkflowOutboxRepository outbox;

    public WorkflowPersistenceService(WorkflowInstanceRepository instances,
                                      WorkflowStepRepository steps,
                                      WorkflowAttemptRepository attempts,
                                      WorkflowOutboxRepository outbox) {
        this.instances = instances;
        this.steps = steps;
        this.attempts = attempts;
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkflowInstance start(UUID ticketId, UUID tenantId, String subject, String description, String priority, String channel) {
        return instances.save(new WorkflowInstance(ticketId, tenantId, "TICKET_TRIAGE", subject, description, priority, channel));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkflowInstance restart(UUID workflowId) {
        WorkflowInstance instance = instances.findById(workflowId).orElseThrow();
        instance.retry();
        return instances.save(instance);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StepHandle startStep(UUID workflowId, String name, int order, String input) {
        WorkflowInstance instance = instances.findById(workflowId).orElseThrow();
        instance.updateProgress(name);
        instances.save(instance);
        WorkflowStep step = steps.save(new WorkflowStep(workflowId, name, order, input));
        WorkflowAttempt attempt = attempts.save(new WorkflowAttempt(step.getId(), 1));
        return new StepHandle(step.getId(), attempt.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeStep(StepHandle handle, String output) {
        WorkflowStep step = steps.findById(handle.stepId()).orElseThrow();
        WorkflowAttempt attempt = attempts.findById(handle.attemptId()).orElseThrow();
        step.complete(output);
        attempt.succeed();
        steps.save(step);
        attempts.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failStep(StepHandle handle, String error) {
        WorkflowStep step = steps.findById(handle.stepId()).orElseThrow();
        WorkflowAttempt attempt = attempts.findById(handle.attemptId()).orElseThrow();
        step.fail(error);
        attempt.fail(error);
        steps.save(step);
        attempts.save(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID workflowId, WorkflowOutboxEvent event) {
        WorkflowInstance instance = instances.findById(workflowId).orElseThrow();
        instance.complete();
        instances.save(instance);
        outbox.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID workflowId, WorkflowOutboxEvent event, String error) {
        WorkflowInstance instance = instances.findById(workflowId).orElseThrow();
        instance.fail(error);
        instances.save(instance);
        outbox.save(event);
    }

    public record StepHandle(UUID stepId, UUID attemptId) {}
}
