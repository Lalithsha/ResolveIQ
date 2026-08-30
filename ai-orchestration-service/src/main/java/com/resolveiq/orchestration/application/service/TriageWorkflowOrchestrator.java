package com.resolveiq.orchestration.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.EventEnvelope;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.contracts.tracing.CorrelationContext;
import com.resolveiq.orchestration.domain.model.WorkflowInstance;
import com.resolveiq.orchestration.domain.model.WorkflowOutboxEvent;
import com.resolveiq.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class TriageWorkflowOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TriageWorkflowOrchestrator.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WorkflowPersistenceService persistence;
    private final JwtService jwtService;

    @Value("${resolveiq.services.analysis-url:http://localhost:8084}")
    private String analysisUrl = "http://localhost:8084";

    @Value("${resolveiq.services.routing-url:http://localhost:8085}")
    private String routingUrl = "http://localhost:8085";

    @Value("${resolveiq.services.rag-url:http://localhost:8086}")
    private String ragUrl = "http://localhost:8086";

    public TriageWorkflowOrchestrator(
        RestTemplateBuilder restTemplateBuilder,
        ObjectMapper objectMapper,
        WorkflowPersistenceService persistence,
        JwtService jwtService
    ) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = objectMapper;
        this.persistence = persistence;
        this.jwtService = jwtService;
    }

    public void executeTriageWorkflow(
        UUID ticketId,
        UUID tenantId,
        String subject,
        String description,
        String priority,
        String channel
    ) {
        log.info("Starting Triage Workflow for ticket {} in tenant {}", ticketId, tenantId);

        WorkflowInstance instance = persistence.start(ticketId, tenantId, subject, description, priority, channel);
        runWorkflow(instance, subject, description, priority, channel);
    }

    public void retryTriageWorkflow(UUID workflowId) {
        WorkflowInstance instance = persistence.restart(workflowId);
        runWorkflow(instance, instance.getSubject(), instance.getDescription(), instance.getPriority(), instance.getChannel());
    }

    private void runWorkflow(WorkflowInstance instance, String subject, String description, String priority, String channel) {
        UUID ticketId = instance.getTicketId();
        UUID tenantId = instance.getTenantId();
        WorkflowPersistenceService.StepHandle activeStep = null;

        try {
            // STEP 1: AI Analysis
            activeStep = persistence.startStep(instance.getId(), "AI_ANALYSIS", 1,
                objectMapper.writeValueAsString(Map.of("subject", subject)));

            AnalysisResultDto analysisResult = callAnalysisService(ticketId, tenantId, subject, description, channel);
            persistence.completeStep(activeStep, objectMapper.writeValueAsString(analysisResult));
            activeStep = null;

            // STEP 2: Routing & SLA Decision
            activeStep = persistence.startStep(instance.getId(), "ROUTING_AND_SLA", 2, "{}");

            RoutingResultDto routingResult = callRoutingService(
                ticketId,
                tenantId,
                analysisResult.category(),
                analysisResult.intent(),
                analysisResult.urgency(),
                priority
            );
            persistence.completeStep(activeStep, objectMapper.writeValueAsString(routingResult));
            activeStep = null;

            // STEP 3: RAG Retrieval (Knowledge + Similar Cases)
            activeStep = persistence.startStep(instance.getId(), "RAG_RETRIEVAL", 3, "{}");

            RetrievalResultDto retrievalResult = callRagService(tenantId, ticketId, subject + " " + description);
            persistence.completeStep(activeStep, objectMapper.writeValueAsString(retrievalResult));
            activeStep = null;

            // STEP 4: Formulate Grounded Draft Response
            activeStep = persistence.startStep(instance.getId(), "DRAFT_GENERATION", 4, "{}");

            DraftResponseDto draft = formulateDraft(subject, description, retrievalResult);
            persistence.completeStep(activeStep, objectMapper.writeValueAsString(draft));
            activeStep = null;

            // Emit TicketTriageCompleted.v1
            TicketEvents.TicketTriageCompletedPayload payload = new TicketEvents.TicketTriageCompletedPayload(
                ticketId,
                analysisResult.intent(),
                analysisResult.category(),
                analysisResult.sentiment(),
                analysisResult.urgency(),
                draft.confidence(),
                routingResult.targetTeamId(),
                routingResult.assignedAgentId(),
                routingResult.slaPolicyId(),
                routingResult.firstResponseDueAt(),
                routingResult.resolutionDueAt(),
                UUID.randomUUID(),
                draft.suggestedText(),
                "resolveiq-grounded-draft",
                "triage-draft-v1",
                draft.citationsJson(),
                java.time.Instant.now()
            );

            EventEnvelope<TicketEvents.TicketTriageCompletedPayload> envelope = EventEnvelope.create(
                TicketEvents.TICKET_TRIAGE_COMPLETED,
                1,
                "ai-orchestration-service",
                tenantId,
                "ticket",
                ticketId,
                CorrelationContext.getCorrelationId(),
                null,
                null,
                payload
            );

            WorkflowOutboxEvent outboxEvent = new WorkflowOutboxEvent(
                "ticket",
                ticketId,
                TicketEvents.TICKET_TRIAGE_COMPLETED,
                objectMapper.writeValueAsString(envelope)
            );
            persistence.complete(instance.getId(), outboxEvent);

            log.info("Triage workflow successfully completed for ticket {}", ticketId);

        } catch (Exception e) {
            log.error("Triage workflow failed for ticket {}: {}", ticketId, e.getMessage(), e);
            if (activeStep != null) {
                try { persistence.failStep(activeStep, safeError(e)); } catch (Exception persistenceError) {
                    log.error("Could not persist failed workflow step {}", activeStep.stepId(), persistenceError);
                }
            }
            persistence.fail(instance.getId(), failureEvent(ticketId, tenantId, safeError(e)), safeError(e));
        }
    }

    protected AnalysisResultDto callAnalysisService(UUID ticketId, UUID tenantId, String subject, String description, String channel) {
        try {
            Map<String, Object> body = Map.of(
                "ticketId", ticketId,
                "tenantId", tenantId,
                "subject", subject,
                "description", description,
                "channel", channel != null ? channel : "WEB"
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtService.serviceToken("ai-orchestration-service", tenantId));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(analysisUrl + "/api/v1/analysis/classify", entity, JsonNode.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode b = resp.getBody();
                String category = b.hasNonNull("category") ? b.get("category").asText() : "GENERAL";
                String intent = b.hasNonNull("intent") ? b.get("intent").asText() : "general_inquiry";
                String sentiment = b.hasNonNull("sentiment") ? b.get("sentiment").asText() : "NEUTRAL";
                String urgency = b.hasNonNull("urgency") ? b.get("urgency").asText() : "MEDIUM";
                return new AnalysisResultDto(category, intent, sentiment, urgency);
            }
            throw new IllegalStateException("Analysis service returned an empty response");
        } catch (Exception e) {
            throw new IllegalStateException("Analysis dependency failed", e);
        }
    }

    protected RoutingResultDto callRoutingService(UUID ticketId, UUID tenantId, String category, String intent, String urgency, String priority) {
        try {
            Map<String, Object> body = Map.of(
                "ticketId", ticketId,
                "tenantId", tenantId,
                "category", category,
                "intent", intent,
                "urgency", urgency,
                "priority", priority != null ? priority : "MEDIUM"
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtService.serviceToken("ai-orchestration-service", tenantId));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(routingUrl + "/api/v1/routing/decide", entity, JsonNode.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode b = resp.getBody();
                UUID teamId = b.hasNonNull("targetTeamId") ? UUID.fromString(b.get("targetTeamId").asText()) : null;
                UUID agentId = b.hasNonNull("assignedAgentId") ? UUID.fromString(b.get("assignedAgentId").asText()) : null;
                UUID slaPolicyId = b.hasNonNull("slaPolicyId") ? UUID.fromString(b.get("slaPolicyId").asText()) : null;
                java.time.Instant firstDue = b.hasNonNull("firstResponseDueAt") ? java.time.Instant.parse(b.get("firstResponseDueAt").asText()) : null;
                java.time.Instant resolutionDue = b.hasNonNull("resolutionDueAt") ? java.time.Instant.parse(b.get("resolutionDueAt").asText()) : null;
                return new RoutingResultDto(teamId, agentId, slaPolicyId, firstDue, resolutionDue);
            }
            throw new IllegalStateException("Routing service returned an empty response");
        } catch (Exception e) {
            throw new IllegalStateException("Routing dependency failed", e);
        }
    }

    protected RetrievalResultDto callRagService(UUID tenantId, UUID ticketId, String query) {
        try {
            Map<String, Object> body = Map.of(
                "ticketId", ticketId,
                "queryText", query,
                "strategy", "HYBRID_RRF",
                "topK", 3
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());
            headers.setBearerAuth(jwtService.serviceToken("ai-orchestration-service", tenantId));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(ragUrl + "/api/v1/retrieval/search", entity, JsonNode.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode b = resp.getBody();
                List<String> citationTexts = new ArrayList<>();
                if (b.hasNonNull("citations") && b.get("citations").isArray()) {
                    for (JsonNode c : b.get("citations")) {
                        if (c.hasNonNull("citationText")) {
                            citationTexts.add(c.get("citationText").asText());
                        }
                    }
                }
                return new RetrievalResultDto(citationTexts, 0.85);
            }
            throw new IllegalStateException("RAG service returned an empty response");
        } catch (Exception e) {
            throw new IllegalStateException("RAG dependency failed", e);
        }
    }

    private DraftResponseDto formulateDraft(String subject, String description, RetrievalResultDto retrieval) {
        if (retrieval.citations().isEmpty() || retrieval.confidence() < 0.65) {
            return new DraftResponseDto(
                "Thank you for contacting ResolveIQ Support. I have received your request regarding: '" + subject + "'. An agent is currently reviewing your account details and will respond shortly.",
                0.60,
                "[]"
            );
        }

        String topCitation = retrieval.citations().get(0);
        String draftText = String.format(
            "Hello, thank you for reaching out to ResolveIQ Support regarding %s.\n\nBased on our documented resolution process:\n%s\n\nPlease let us know if this resolves your issue or if further assistance is needed.",
            subject,
            topCitation
        );

        String citationsJson = "[{\"source\": \"Knowledge Base Article [1]\", \"snippet\": \"" + topCitation.replace("\"", "'") + "\"}]";

        return new DraftResponseDto(draftText, 0.90, citationsJson);
    }

    private WorkflowOutboxEvent failureEvent(UUID ticketId, UUID tenantId, String error) {
        try {
            TicketEvents.TicketTriageFailedPayload payload = new TicketEvents.TicketTriageFailedPayload(
                ticketId,
                error != null ? error : "Unknown error",
                "PROCESSING_ERROR",
                false,
                java.time.Instant.now()
            );

            EventEnvelope<TicketEvents.TicketTriageFailedPayload> envelope = EventEnvelope.create(
                TicketEvents.TICKET_TRIAGE_FAILED,
                1,
                "ai-orchestration-service",
                tenantId,
                "ticket",
                ticketId,
                CorrelationContext.getCorrelationId(),
                null,
                null,
                payload
            );

            return new WorkflowOutboxEvent(
                "ticket",
                ticketId,
                TicketEvents.TICKET_TRIAGE_FAILED,
                objectMapper.writeValueAsString(envelope)
            );
        } catch (Exception serializationError) {
            throw new IllegalStateException("Unable to serialize workflow failure event", serializationError);
        }
    }

    private String safeError(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message.substring(0, Math.min(message.length(), 70));
    }

    protected record AnalysisResultDto(String category, String intent, String sentiment, String urgency) {}
    protected record RoutingResultDto(UUID targetTeamId, UUID assignedAgentId, UUID slaPolicyId,
                                      java.time.Instant firstResponseDueAt, java.time.Instant resolutionDueAt) {}
    protected record RetrievalResultDto(List<String> citations, double confidence) {}
    protected record DraftResponseDto(String suggestedText, double confidence, String citationsJson) {}
}
