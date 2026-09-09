package com.resolveiq.ticket.resolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.ticket.adapter.in.web.resolution.TicketResolutionController;
import com.resolveiq.ticket.application.dto.resolution.ResolutionDtos.*;
import com.resolveiq.ticket.application.service.resolution.ResolutionServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketResolutionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private ResolutionServicePort resolutionService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID resolverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        resolutionService = mock(ResolutionServicePort.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TicketResolutionController(resolutionService)).build();
    }

    @Test
    @DisplayName("POST /api/v1/tickets/{id}/resolve resolves ticket")
    void resolveTicketEndpoint() throws Exception {
        ResolutionAttemptResponse response = new ResolutionAttemptResponse(
            UUID.randomUUID(), ticketId, 1, resolverId,
            Instant.now(), Instant.now().plusSeconds(604800), null,
            "PENDING_CONFIRMATION", 0, "v1",
            List.of()
        );

        when(resolutionService.resolveTicket(eq(tenantId), eq(ticketId), eq(resolverId), eq("fp-123")))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/resolve", ticketId)
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-User-Id", resolverId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("solutionFingerprint", "fp-123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
            .andExpect(jsonPath("$.score").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/tickets/{id}/resolution returns resolution history")
    void getResolutionHistoryEndpoint() throws Exception {
        ResolutionAttemptResponse response = new ResolutionAttemptResponse(
            UUID.randomUUID(), ticketId, 1, resolverId,
            Instant.now(), Instant.now().plusSeconds(600), Instant.now().plusSeconds(86400),
            "CONFIRMED", 100, "v1",
            List.of()
        );

        when(resolutionService.getResolutionHistory(tenantId, ticketId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}/resolution", ticketId)
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/governance/resolution-metrics returns resolution flywheel metrics")
    void getResolutionMetricsEndpoint() throws Exception {
        ResolutionMetricsResponse response = new ResolutionMetricsResponse(
            100L, 50L, 0.50, 0.90, 0.85, 82
        );

        when(resolutionService.getResolutionMetrics(tenantId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/governance/resolution-metrics")
                .header("X-Tenant-Id", tenantId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eligibleAttempts").value(100))
            .andExpect(jsonPath("$.verifiedSuccessRate").value(0.90));
    }
}
