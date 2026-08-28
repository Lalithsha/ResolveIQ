package com.resolveiq.orchestration.domain.repository;

import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowOutboxRepository extends JpaRepository<WorkflowOutboxEvent, UUID> {
    List<WorkflowOutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
