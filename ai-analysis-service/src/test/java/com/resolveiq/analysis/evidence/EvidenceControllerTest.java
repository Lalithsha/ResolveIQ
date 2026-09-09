package com.resolveiq.analysis.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.analysis.adapter.in.web.EvidenceController;
import com.resolveiq.analysis.application.dto.EvidenceDtos.*;
import com.resolveiq.analysis.application.service.evidence.EvidenceServicePort;
import com.resolveiq.analysis.domain.model.evidence.PipelineStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EvidenceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private EvidenceServicePort evidenceService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID attachmentId = UUID.randomUUID();
    private final UUID evidenceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evidenceService = mock(EvidenceServicePort.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EvidenceController(evidenceService)).build();
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{id}/evidence/uploads via JSON creates evidence job")
    void uploadJsonCreatesEvidenceJob() throws Exception {
        EvidenceUploadRequest request = new EvidenceUploadRequest("saml_screenshot.png", "image/png", "dummy".getBytes(), true);
        EvidenceJobResponse response = new EvidenceJobResponse(
            evidenceId, tenantId, ticketId, attachmentId, "saml_screenshot.png", "image/png",
            100L, PipelineStatus.READY, true, "STANDARD_30D", 1, null, Instant.now(), Instant.now()
        );

        when(evidenceService.createUploadSession(eq(tenantId), eq(ticketId), any(), eq("saml_screenshot.png"), eq("image/png"), any(), eq(true)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/evidence/uploads", ticketId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(evidenceId.toString()))
            .andExpect(jsonPath("$.pipelineStatus").value("READY"));
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{id}/evidence/{attachmentId}/consent updates consent")
    void updateConsentEndpoint() throws Exception {
        EvidenceConsentRequest request = new EvidenceConsentRequest(true);
        EvidenceJobResponse response = new EvidenceJobResponse(
            evidenceId, tenantId, ticketId, attachmentId, "invoice.pdf", "application/pdf",
            200L, PipelineStatus.READY, true, "STANDARD_30D", 1, null, Instant.now(), Instant.now()
        );

        when(evidenceService.updateConsent(tenantId, ticketId, attachmentId, true)).thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/evidence/{attachmentId}/consent", ticketId, attachmentId)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentGranted").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/tickets/{id}/evidence lists ticket evidence")
    void listTicketEvidence() throws Exception {
        EvidenceJobResponse response = new EvidenceJobResponse(
            evidenceId, tenantId, ticketId, attachmentId, "recording.mp4", "video/mp4",
            5000L, PipelineStatus.READY, true, "STANDARD_30D", 1, null, Instant.now(), Instant.now()
        );
        when(evidenceService.listByTicket(tenantId, ticketId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/evidence", ticketId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fileName").value("recording.mp4"));
    }

    @Test
    @DisplayName("GET /api/v1/evidence/{id}/artifacts returns artifact list")
    void getArtifactsEndpoint() throws Exception {
        EvidenceArtifactResponse artifact = new EvidenceArtifactResponse(
            UUID.randomUUID(), evidenceId, ticketId, "VIDEO_FRAME", "redacted/frame.png",
            42, 42.0, "sha256hash", "INTERNAL", "[REDACTED_FRAME_42]", Instant.now()
        );
        when(evidenceService.getArtifacts(tenantId, evidenceId)).thenReturn(List.of(artifact));

        mockMvc.perform(get("/api/v1/evidence/{id}/artifacts", evidenceId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].artifactType").value("VIDEO_FRAME"))
            .andExpect(jsonPath("$[0].timestampSeconds").value(42.0));
    }

    @Test
    @DisplayName("GET /api/v1/evidence/{id}/observations returns observations including failure timestamp")
    void getObservationsEndpoint() throws Exception {
        EvidenceObservationResponse observation = new EvidenceObservationResponse(
            UUID.randomUUID(), evidenceId, ticketId, "FAILURE_TIMESTAMP", "00:42",
            "Failure chapter detected at 00:42", 0.99, "{\"timestamp\": 42.0}", Instant.now()
        );
        when(evidenceService.getObservations(tenantId, evidenceId)).thenReturn(List.of(observation));

        mockMvc.perform(get("/api/v1/evidence/{id}/observations", evidenceId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].observationType").value("FAILURE_TIMESTAMP"))
            .andExpect(jsonPath("$[0].codeOrKey").value("00:42"));
    }

    @Test
    @DisplayName("GET /api/v1/evidence/{id}/content?variant=original with EVIDENCE_VIEW_ORIGINAL returns 200 OK")
    void getOriginalContentWithPermissionReturns200() throws Exception {
        when(evidenceService.getContent(eq(tenantId), eq(evidenceId), eq("original"), anySet(), anySet(), eq("Audited request")))
            .thenReturn("raw confidential content");

        mockMvc.perform(get("/api/v1/evidence/{id}/content", evidenceId)
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-Roles", "AGENT")
                .header("X-Permissions", "EVIDENCE_VIEW_ORIGINAL")
                .param("variant", "original")
                .param("reason", "Audited request"))
            .andExpect(status().isOk())
            .andExpect(content().string("raw confidential content"));
    }

    @Test
    @DisplayName("DELETE /api/v1/evidence/{id} tombstones evidence and returns 204 No Content")
    void deleteEvidenceTombstonesJob() throws Exception {
        doNothing().when(evidenceService).tombstoneEvidence(tenantId, evidenceId);

        mockMvc.perform(delete("/api/v1/evidence/{id}", evidenceId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isNoContent());
    }
}
