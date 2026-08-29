package com.resolveiq.orchestration.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_attempts", schema = "orchestration_schema")
public class WorkflowAttempt {

    @Id
    private UUID id;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 50)
    private String status; // RUNNING, SUCCESS, FAILURE

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant endedAt;

    public WorkflowAttempt() {}

    public WorkflowAttempt(UUID stepId, int attemptNumber) {
        this.id = UUID.randomUUID();
        this.stepId = stepId;
        this.attemptNumber = attemptNumber;
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStepId() { return stepId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }

    public void succeed() {
        this.status = "SUCCESS";
        this.endedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = "FAILURE";
        this.errorMessage = error;
        this.endedAt = Instant.now();
    }
}
