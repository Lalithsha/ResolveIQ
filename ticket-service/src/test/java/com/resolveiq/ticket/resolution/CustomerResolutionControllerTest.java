package com.resolveiq.ticket.resolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.ticket.adapter.in.web.resolution.CustomerResolutionController;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerResolutionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private ResolutionServicePort resolutionService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        resolutionService = mock(ResolutionServicePort.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerResolutionController(resolutionService)).build();
    }

    @Test
    @DisplayName("POST /api/v1/customer/tickets/{id}/resolution-outcome records customer rating")
    void recordCustomerOutcome() throws Exception {
        ResolutionOutcomeRequest request = new ResolutionOutcomeRequest("YES", "All resolved, thank you!");
        ResolutionAttemptResponse response = new ResolutionAttemptResponse(
            UUID.randomUUID(), ticketId, 1, UUID.randomUUID(),
            Instant.now(), Instant.now().plusSeconds(600), Instant.now().plusSeconds(86400),
            "CONFIRMED", 100, "v1",
            List.of(new OutcomeItemResponse(UUID.randomUUID(), "CUSTOMER", "YES", "All resolved, thank you!", Instant.now(), 100))
        );

        when(resolutionService.recordOutcome(eq(tenantId), eq(ticketId), eq(customerId), eq("YES"), eq("All resolved, thank you!")))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/customer/tickets/{ticketId}/resolution-outcome", ticketId)
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-User-Id", customerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.score").value(100));
    }

    @Test
    @DisplayName("POST /api/v1/customer/tickets/{id}/reopen reopens ticket")
    void customerReopenTicket() throws Exception {
        ResolutionReopenRequest request = new ResolutionReopenRequest("Issue recurring on logout");
        ResolutionAttemptResponse response = new ResolutionAttemptResponse(
            UUID.randomUUID(), ticketId, 1, UUID.randomUUID(),
            Instant.now(), Instant.now().plusSeconds(600), null,
            "REOPENED", -50, "v1",
            List.of()
        );

        when(resolutionService.reopenTicket(eq(tenantId), eq(ticketId), eq(customerId), eq("Issue recurring on logout")))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/customer/tickets/{ticketId}/reopen", ticketId)
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-User-Id", customerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REOPENED"))
            .andExpect(jsonPath("$.score").value(-50));
    }
}
