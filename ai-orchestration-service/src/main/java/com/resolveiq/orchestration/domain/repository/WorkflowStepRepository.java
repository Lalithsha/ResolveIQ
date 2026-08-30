package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {
    List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(UUID workflowId);
}
