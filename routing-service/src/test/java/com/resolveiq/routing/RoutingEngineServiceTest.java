package com.resolveiq.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.routing.application.dto.RoutingDecisionRequest;
import com.resolveiq.routing.application.dto.RoutingDecisionResponse;
import com.resolveiq.routing.application.service.RoutingEngineService;
import com.resolveiq.routing.application.service.SlaCalculationService;
import com.resolveiq.routing.domain.model.Agent;
import com.resolveiq.routing.domain.model.RoutingDecision;
import com.resolveiq.routing.domain.model.RoutingRule;
import com.resolveiq.routing.domain.model.Team;
import com.resolveiq.routing.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class RoutingEngineServiceTest {

    @Mock
    private RoutingRuleRepository ruleRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private RoutingDecisionRepository decisionRepository;
    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    private ObjectMapper objectMapper;
    private SlaCalculationService slaService;
    private RoutingEngineService routingEngineService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        slaService = new SlaCalculationService(slaPolicyRepository);
        routingEngineService = new RoutingEngineService(
            ruleRepository,
            teamRepository,
            agentRepository,
            decisionRepository,
            slaService,
            objectMapper
        );
    }

    @Test
    @DisplayName("Should match routing rule by category and assign available agent with lowest workload")
    void testRuleMatchingAndWorkloadDistribution() {
        UUID tenantId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID billingTeamId = UUID.randomUUID();
        UUID agent1Id = UUID.randomUUID();

        RoutingRule billingRule = new RoutingRule(
            UUID.randomUUID(),
            tenantId,
            "Billing Rule",
            "v1.0",
            "{\"category\": \"BILLING\"}",
            billingTeamId,
            1
        );

        Agent agent1 = new Agent(agent1Id, tenantId, billingTeamId, "Alice Smith", "alice@test.com");
        // activeTicketCount = 0

        when(ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityOrderAsc(tenantId))
            .thenReturn(List.of(billingRule));
        when(agentRepository.findByTeamIdAndStatusOrderByActiveTicketCountAsc(billingTeamId, "ONLINE"))
            .thenReturn(List.of(agent1));

        RoutingDecisionRequest request = new RoutingDecisionRequest(
            ticketId,
            tenantId,
            "BILLING",
            "billing_dispute",
            "HIGH",
            "HIGH",
            "en"
        );

        RoutingDecisionResponse response = routingEngineService.decideRouting(request);

        assertThat(response).isNotNull();
        assertThat(response.targetTeamId()).isEqualTo(billingTeamId);
        assertThat(response.assignedAgentId()).isEqualTo(agent1Id);
        assertThat(response.firstResponseDueAt()).isNotNull();
        assertThat(response.resolutionDueAt()).isNotNull();

        verify(agentRepository, times(1)).save(agent1);
        verify(decisionRepository, times(1)).save(any(RoutingDecision.class));
    }
}
