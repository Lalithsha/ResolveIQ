package com.resolveiq.rag.flywheel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.rag.adapter.in.web.flywheel.KnowledgeFlywheelController;
import com.resolveiq.rag.application.dto.flywheel.FlywheelDtos.*;
import com.resolveiq.rag.application.service.flywheel.KnowledgeFlywheelServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeFlywheelControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private KnowledgeFlywheelServicePort flywheelService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        flywheelService = mock(KnowledgeFlywheelServicePort.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeFlywheelController(flywheelService)).build();
    }

    @Test
    @DisplayName("GET /api/v1/knowledge/candidates lists all candidates")
    void listCandidatesEndpoint() throws Exception {
        CandidateResponse candidate = new CandidateResponse(
            candidateId, "SAML Resolution Guide", "Draft content", "Sanitized content",
            "AUTHENTICATION", "ELIGIBLE", 95, 6, "COMPLETED", "hash123", Instant.now()
        );
        when(flywheelService.listCandidates(tenantId)).thenReturn(List.of(candidate));

        mockMvc.perform(get("/api/v1/knowledge/candidates")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("SAML Resolution Guide"))
            .andExpect(jsonPath("$[0].eligibilityStatus").value("ELIGIBLE"));
    }

    @Test
    @DisplayName("POST /api/v1/knowledge/candidates/{id}/sanitize performs PII/secret scrubbing")
    void sanitizeCandidateEndpoint() throws Exception {
        CandidateResponse candidate = new CandidateResponse(
            candidateId, "SAML Resolution Guide", "Draft with key", "Sanitized draft",
            "AUTHENTICATION", "ELIGIBLE", 95, 6, "COMPLETED", "hash123", Instant.now()
        );
        when(flywheelService.sanitizeCandidate(tenantId, candidateId)).thenReturn(candidate);

        mockMvc.perform(post("/api/v1/knowledge/candidates/{id}/sanitize", candidateId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sanitizationStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/knowledge/candidates/{id}/evaluation-runs evaluates gate metrics")
    void runEvaluationEndpoint() throws Exception {
        UUID evalId = UUID.randomUUID();
        EvaluationRunResponse response = new EvaluationRunResponse(
            evalId, candidateId, "v1.2-frozen-holdout", 0.82, 0.88, 0.72, 0.81,
            true, 1.05, true, Instant.now()
        );
        when(flywheelService.runEvaluation(tenantId, candidateId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/knowledge/candidates/{id}/evaluation-runs", candidateId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gatePassed").value(true))
            .andExpect(jsonPath("$.safetyCasesPassed").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/knowledge/candidates/{id}/release publishes candidate")
    void releaseCandidateEndpoint() throws Exception {
        UUID releaseId = UUID.randomUUID();
        KnowledgeReleaseResponse response = new KnowledgeReleaseResponse(
            releaseId, candidateId, UUID.randomUUID(), 1, "ACTIVE",
            UUID.randomUUID(), userId, "Initial verified release", Instant.now()
        );
        when(flywheelService.releaseCandidate(eq(tenantId), eq(candidateId), eq(userId), anySet(), anySet()))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/knowledge/candidates/{id}/release", candidateId)
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-User-Id", userId.toString())
                .header("X-Roles", "LEAD_RESOLVER,ADMIN")
                .header("X-Permissions", "KNOWLEDGE_RELEASE_APPROVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.versionNumber").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/knowledge/releases/{id}/rollback rolls back release")
    void rollbackReleaseEndpoint() throws Exception {
        UUID releaseId = UUID.randomUUID();
        UUID prevReleaseId = UUID.randomUUID();
        RollbackResponse response = new RollbackResponse(
            UUID.randomUUID(), releaseId, prevReleaseId, "Regression in live testing", userId, Instant.now()
        );
        when(flywheelService.rollbackRelease(eq(tenantId), eq(releaseId), eq(prevReleaseId), eq(userId), eq("Regression in live testing"), anySet(), anySet()))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/knowledge/releases/{id}/rollback", releaseId)
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-User-Id", userId.toString())
                .header("X-Roles", "LEAD_RESOLVER,ADMIN")
                .header("X-Permissions", "KNOWLEDGE_RELEASE_ROLLBACK")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RollbackRequest(prevReleaseId, "Regression in live testing"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reason").value("Regression in live testing"));
    }
}
