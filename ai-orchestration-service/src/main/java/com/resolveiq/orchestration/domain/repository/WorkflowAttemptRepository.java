package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.WorkflowAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowAttemptRepository extends JpaRepository<WorkflowAttempt, UUID> {
    List<WorkflowAttempt> findByStepIdOrderByAttemptNumberAsc(UUID stepId);
}
