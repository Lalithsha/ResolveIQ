package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.WorkflowAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkflowAttemptRepository extends JpaRepository<WorkflowAttempt, UUID> {
    List<WorkflowAttempt> findByStepIdOrderByAttemptNumberAsc(UUID stepId);
}
