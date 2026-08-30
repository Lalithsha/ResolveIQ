package com.resolveiq.orchestration.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowStep;
import com.resolveiq.orchestration.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import com.resolveiq.security.JwtService;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private WorkflowPersistenceService persistence;

    private ObjectMapper objectMapper;
    private TriageWorkflowOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        Map<UUID, WorkflowInstance> instances = new HashMap<>();
        Map<UUID, WorkflowStep> steps = new HashMap<>();
        Map<UUID, com.resolveiq.orchestration.domain.model.WorkflowAttempt> attempts = new HashMap<>();
        when(instanceRepository.save(any())).thenAnswer(call -> {
            WorkflowInstance value = call.getArgument(0); instances.put(value.getId(), value); return value;
        });
        when(instanceRepository.findById(any())).thenAnswer(call -> Optional.ofNullable(instances.get(call.getArgument(0))));
        when(stepRepository.save(any())).thenAnswer(call -> {
            WorkflowStep value = call.getArgument(0); steps.put(value.getId(), value); return value;
        });
        when(stepRepository.findById(any())).thenAnswer(call -> Optional.ofNullable(steps.get(call.getArgument(0))));
        when(attemptRepository.save(any())).thenAnswer(call -> {
            var value = (com.resolveiq.orchestration.domain.model.WorkflowAttempt) call.getArgument(0);
            attempts.put(value.getId(), value); return value;
        });
        when(attemptRepository.findById(any())).thenAnswer(call -> Optional.ofNullable(attempts.get(call.getArgument(0))));
        persistence = new WorkflowPersistenceService(instanceRepository, stepRepository, attemptRepository, outboxRepository);
        orchestrator = new TriageWorkflowOrchestrator(
            new RestTemplateBuilder(),
            objectMapper,
            persistence,
            new JwtService("fictional_jwt_hmac_secret_key_minimum_256_bits_for_local_development_only_12345", "resolveiq-auth", "resolveiq-api")
        ) {
            @Override protected AnalysisResultDto callAnalysisService(UUID ticketId, UUID tenantId, String subject, String description, String channel) {
                return new AnalysisResultDto("BILLING", "refund_request", "NEGATIVE", "HIGH");
            }
            @Override protected RoutingResultDto callRoutingService(UUID ticketId, UUID tenantId, String category, String intent, String urgency, String priority) {
                return new RoutingResultDto(UUID.randomUUID(), null, null, null, null);
            }
            @Override protected RetrievalResultDto callRagService(UUID tenantId, UUID ticketId, String query) {
                return new RetrievalResultDto(java.util.List.of("Verify the duplicate charge before refunding."), 0.9);
            }
        };
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

        verify(stepRepository, atLeast(8)).save(any(WorkflowStep.class));
        verify(outboxRepository, times(1)).save(any());
    }
}
