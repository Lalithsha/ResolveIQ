package com.resolveiq.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.orchestration.application.service.TriageWorkflowOrchestrator;
import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.orchestration.domain.model.WorkflowStep;
import com.resolveiq.orchestration.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TriageWorkflowOrchestratorTest {

    @Mock
    private WorkflowInstanceRepository instanceRepository;
    @Mock
    private WorkflowStepRepository stepRepository;
    @Mock
    private WorkflowAttemptRepository attemptRepository;
    @Mock
    private WorkflowOutboxRepository outboxRepository;

    private ObjectMapper objectMapper;
    private TriageWorkflowOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        orchestrator = new TriageWorkflowOrchestrator(
            instanceRepository,
            stepRepository,
            attemptRepository,
            outboxRepository,
            new RestTemplateBuilder(),
            objectMapper
        );
    }

    @Test
    @DisplayName("Should execute all 4 workflow steps and produce durable outbox completion event")
    void testWorkflowExecution() {
        UUID ticketId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        orchestrator.executeTriageWorkflow(
            ticketId,
            tenantId,
            "Refund Request for Duplicate Subscription Charge",
            "I was billed twice on August 1st for the Pro Plan.",
            "HIGH",
            "WEB"
        );

        // Verify instance was saved and completed
        verify(instanceRepository, atLeast(2)).save(any(WorkflowInstance.class));
        // Verify 4 steps were recorded (Analysis, Routing, RAG, Draft)
        verify(stepRepository, atLeast(4)).save(any(WorkflowStep.class));
        // Verify completion outbox event was generated
        verify(outboxRepository, times(1)).save(any(WorkflowOutboxEvent.class));
    }
}
