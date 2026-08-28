package com.resolveiq.orchestration.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps", schema = "orchestration_schema")
public class WorkflowStep {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "input_payload", columnDefinition = "JSONB")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "JSONB")
    private String outputPayload;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WorkflowStep() {}

    public WorkflowStep(UUID workflowId, String stepName, int stepOrder, String inputPayload) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.stepName = stepName;
        this.stepOrder = stepOrder;
        this.status = "RUNNING";
        this.inputPayload = inputPayload;
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getStepName() { return stepName; }
    public int getStepOrder() { return stepOrder; }
    public String getStatus() { return status; }
    public String getInputPayload() { return inputPayload; }
    public String getOutputPayload() { return outputPayload; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void complete(String outputPayload) {
        this.status = "COMPLETED";
        this.outputPayload = outputPayload;
        this.completedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = "FAILED";
        this.outputPayload = String.format("{\"error\": \"%s\"}", error != null ? error.replace("\"", "'") : "Unknown");
        this.completedAt = Instant.now();
    }
}
