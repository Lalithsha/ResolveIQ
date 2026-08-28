package com.resolveiq.orchestration.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.EventEnvelope;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.contracts.tracing.CorrelationContext;
import com.resolveiq.orchestration.domain.model.*;
import com.resolveiq.orchestration.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class TriageWorkflowOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TriageWorkflowOrchestrator.class);

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowAttemptRepository attemptRepository;
    private final WorkflowOutboxRepository outboxRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${resolveiq.services.analysis-url:http://localhost:8084}")
    private String analysisUrl;

    @Value("${resolveiq.services.routing-url:http://localhost:8085}")
    private String routingUrl;

    @Value("${resolveiq.services.rag-url:http://localhost:8086}")
    private String ragUrl;

    public TriageWorkflowOrchestrator(
        WorkflowInstanceRepository instanceRepository,
        WorkflowStepRepository stepRepository,
        WorkflowAttemptRepository attemptRepository,
        WorkflowOutboxRepository outboxRepository,
        RestTemplateBuilder restTemplateBuilder,
        ObjectMapper objectMapper
    ) {
        this.instanceRepository = instanceRepository;
        this.stepRepository = stepRepository;
        this.attemptRepository = attemptRepository;
        this.outboxRepository = outboxRepository;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void executeTriageWorkflow(
        UUID ticketId,
        UUID tenantId,
        String subject,
        String description,
        String priority,
        String channel
    ) {
        log.info("Starting Triage Workflow for ticket {} in tenant {}", ticketId, tenantId);

        WorkflowInstance instance = new WorkflowInstance(ticketId, tenantId, "TICKET_TRIAGE");
        instanceRepository.save(instance);

        try {
            // STEP 1: AI Analysis
            instance.updateProgress("AI_ANALYSIS");
            WorkflowStep step1 = new WorkflowStep(instance.getId(), "AI_ANALYSIS", 1, "{\"subject\":\"" + subject + "\"}");
            stepRepository.save(step1);

            AnalysisResultDto analysisResult = callAnalysisService(ticketId, tenantId, subject, description, channel);
            step1.complete(objectMapper.writeValueAsString(analysisResult));
            stepRepository.save(step1);

            // STEP 2: Routing & SLA Decision
            instance.updateProgress("ROUTING_AND_SLA");
            WorkflowStep step2 = new WorkflowStep(instance.getId(), "ROUTING_AND_SLA", 2, "{}");
            stepRepository.save(step2);

            RoutingResultDto routingResult = callRoutingService(
                ticketId,
                tenantId,
                analysisResult.category(),
                analysisResult.intent(),
                analysisResult.urgency(),
                priority
            );
            step2.complete(objectMapper.writeValueAsString(routingResult));
            stepRepository.save(step2);

            // STEP 3: RAG Retrieval (Knowledge + Similar Cases)
            instance.updateProgress("RAG_RETRIEVAL");
            WorkflowStep step3 = new WorkflowStep(instance.getId(), "RAG_RETRIEVAL", 3, "{}");
            stepRepository.save(step3);

            RetrievalResultDto retrievalResult = callRagService(tenantId, ticketId, subject + " " + description);
            step3.complete(objectMapper.writeValueAsString(retrievalResult));
            stepRepository.save(step3);

            // STEP 4: Formulate Grounded Draft Response
            instance.updateProgress("DRAFT_GENERATION");
            WorkflowStep step4 = new WorkflowStep(instance.getId(), "DRAFT_GENERATION", 4, "{}");
            stepRepository.save(step4);

            DraftResponseDto draft = formulateDraft(subject, description, retrievalResult);
            step4.complete(objectMapper.writeValueAsString(draft));
            stepRepository.save(step4);

            // Mark Workflow Completed
            instance.complete();
            instanceRepository.save(instance);

            // Emit TicketTriageCompleted.v1
            TicketEvents.TicketTriageCompletedPayload payload = new TicketEvents.TicketTriageCompletedPayload(
                ticketId,
                analysisResult.intent(),
                analysisResult.category(),
                analysisResult.sentiment(),
                analysisResult.urgency(),
                draft.confidence(),
                routingResult.targetTeamId(),
                UUID.randomUUID(), // Generated suggestion ID
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
            outboxRepository.save(outboxEvent);

            log.info("Triage workflow successfully completed for ticket {}", ticketId);

        } catch (Exception e) {
            log.error("Triage workflow failed for ticket {}: {}", ticketId, e.getMessage(), e);
            instance.fail(e.getMessage());
            instanceRepository.save(instance);

            // Emit TicketTriageFailed.v1
            emitFailureEvent(ticketId, tenantId, e.getMessage());
        }
    }

    private AnalysisResultDto callAnalysisService(UUID ticketId, UUID tenantId, String subject, String description, String channel) {
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
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(analysisUrl + "/api/v1/analysis/classify", entity, JsonNode.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode b = resp.getBody();
                return new AnalysisResultDto(
                    b.get("category").asText(),
                    b.get("intent").asText(),
                    b.get("sentiment").asText(),
                    b.get("urgency").asText()
                );
            }
        } catch (Exception e) {
            log.warn("Analysis service call fallback: {}", e.getMessage());
        }
        return new AnalysisResultDto("TECHNICAL", "general_inquiry", "NEUTRAL", "MEDIUM");
    }

    private RoutingResultDto callRoutingService(UUID ticketId, UUID tenantId, String category, String intent, String urgency, String priority) {
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
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(routingUrl + "/api/v1/routing/decide", entity, JsonNode.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode b = resp.getBody();
                UUID teamId = b.has("targetTeamId") && !b.get("targetTeamId").isNull() ? UUID.fromString(b.get("targetTeamId").asText()) : null;
                UUID agentId = b.has("assignedAgentId") && !b.get("assignedAgentId").isNull() ? UUID.fromString(b.get("assignedAgentId").asText()) : null;
                return new RoutingResultDto(teamId, agentId);
            }
        } catch (Exception e) {
            log.warn("Routing service call fallback: {}", e.getMessage());
        }
        return new RoutingResultDto(null, null);
    }

    private RetrievalResultDto callRagService(UUID tenantId, UUID ticketId, String query) {
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
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> resp = restTemplate.postForEntity(ragUrl + "/api/v1/retrieval/search", entity, JsonNode.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode b = resp.getBody();
                List<String> citationTexts = new ArrayList<>();
                if (b.has("citations") && b.get("citations").isArray()) {
                    for (JsonNode c : b.get("citations")) {
                        citationTexts.add(c.get("citationText").asText());
                    }
                }
                return new RetrievalResultDto(citationTexts, 0.85);
            }
        } catch (Exception e) {
            log.warn("RAG service call fallback: {}", e.getMessage());
        }
        return new RetrievalResultDto(List.of(), 0.0);
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

    private void emitFailureEvent(UUID ticketId, UUID tenantId, String error) {
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

            WorkflowOutboxEvent outboxEvent = new WorkflowOutboxEvent(
                "ticket",
                ticketId,
                TicketEvents.TICKET_TRIAGE_FAILED,
                objectMapper.writeValueAsString(envelope)
            );
            outboxRepository.save(outboxEvent);
        } catch (Exception ignored) {}
    }

    private record AnalysisResultDto(String category, String intent, String sentiment, String urgency) {}
    private record RoutingResultDto(UUID targetTeamId, UUID assignedAgentId) {}
    private record RetrievalResultDto(List<String> citations, double confidence) {}
    private record DraftResponseDto(String suggestedText, double confidence, String citationsJson) {}
}
