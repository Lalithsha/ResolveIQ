package com.resolveiq.orchestration.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_instances", schema = "orchestration_schema")
public class WorkflowInstance {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_type", nullable = false, length = 100)
    private String workflowType;

    @Column(nullable = false, length = 50)
    private String status; // RUNNING, COMPLETED, FAILED, TIMED_OUT

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WorkflowInstance() {}

    public WorkflowInstance(UUID ticketId, UUID tenantId, String workflowType) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.workflowType = workflowType;
        this.status = "RUNNING";
        this.currentStep = "START";
        this.deadlineAt = Instant.now().plusSeconds(60); // 60-second execution deadline
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public String getWorkflowType() { return workflowType; }
    public String getStatus() { return status; }
    public String getCurrentStep() { return currentStep; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateProgress(String currentStep) {
        this.currentStep = currentStep;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.currentStep = "COMPLETED";
        this.updatedAt = Instant.now();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void retry() {
        this.status = "RUNNING";
        this.deadlineAt = Instant.now().plusSeconds(60);
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = "FAILED";
        this.currentStep = "FAILED: " + reason;
        this.updatedAt = Instant.now();
    }
}
