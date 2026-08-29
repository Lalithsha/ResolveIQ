package com.resolveiq.routing.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.routing.application.dto.*;
import com.resolveiq.routing.domain.model.*;
import com.resolveiq.routing.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoutingEngineService {

    private final RoutingRuleRepository ruleRepository;
    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final RoutingDecisionRepository decisionRepository;
    private final SlaCalculationService slaService;
    private final ObjectMapper objectMapper;

    public RoutingEngineService(
        RoutingRuleRepository ruleRepository,
        TeamRepository teamRepository,
        AgentRepository agentRepository,
        RoutingDecisionRepository decisionRepository,
        SlaCalculationService slaService,
        ObjectMapper objectMapper
    ) {
        this.ruleRepository = ruleRepository;
        this.teamRepository = teamRepository;
        this.agentRepository = agentRepository;
        this.decisionRepository = decisionRepository;
        this.slaService = slaService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RoutingDecisionResponse decideRouting(RoutingDecisionRequest request) {
        UUID tenantId = java.util.Objects.requireNonNull(request.tenantId(), "Tenant ID is required");

        // 1. Evaluate routing rules in priority order
        List<RoutingRule> activeRules = ruleRepository.findByTenantIdAndActiveTrueOrderByPriorityOrderAsc(tenantId);
        RoutingRule matchedRule = null;
        UUID targetTeamId = null;

        for (RoutingRule rule : activeRules) {
            if (ruleMatches(rule, request)) {
                matchedRule = rule;
                targetTeamId = rule.getTargetTeamId();
                break;
            }
        }

        // Default team if no rule matched
        if (targetTeamId == null) {
            List<Team> teams = teamRepository.findByTenantId(tenantId);
            if (!teams.isEmpty()) {
                targetTeamId = teams.get(0).getId();
            } else {
                Team defaultTeam = new Team(UUID.randomUUID(), tenantId, "General Support", "Default triage queue", 50);
                teamRepository.save(defaultTeam);
                targetTeamId = defaultTeam.getId();
            }
        }

        // 2. Select available agent with lowest active ticket count
        UUID assignedAgentId = null;
        List<Agent> agents = agentRepository.findByTeamIdAndStatusOrderByActiveTicketCountAsc(targetTeamId, "ONLINE");
        if (!agents.isEmpty()) {
            Agent agent = agents.get(0);
            agent.incrementWorkload();
            agentRepository.save(agent);
            assignedAgentId = agent.getId();
        }

        // 3. Compute SLA targets
        SlaCalculationService.SlaTarget slaTarget = slaService.calculateSlaTarget(tenantId, request.priority());

        // 4. Save decision
        String inputFacts = String.format("{\"category\":\"%s\",\"intent\":\"%s\",\"urgency\":\"%s\",\"priority\":\"%s\"}",
            request.category(), request.intent(), request.urgency(), request.priority());

        RoutingDecision decision = new RoutingDecision(
            request.ticketId(),
            tenantId,
            matchedRule != null ? matchedRule.getId() : null,
            matchedRule != null ? matchedRule.getVersion() : "default",
            targetTeamId,
            assignedAgentId,
            matchedRule != null ? "Matched rule: " + matchedRule.getName() : "Default queue assignment",
            inputFacts
        );
        decisionRepository.save(decision);

        return new RoutingDecisionResponse(
            decision.getId(),
            decision.getTicketId(),
            targetTeamId,
            assignedAgentId,
            slaTarget.policyId(),
            slaTarget.firstResponseDueAt(),
            slaTarget.resolutionDueAt(),
            matchedRule != null ? matchedRule.getName() : "Default Routing",
            decision.getReason()
        );
    }

    private boolean ruleMatches(RoutingRule rule, RoutingDecisionRequest request) {
        try {
            JsonNode conditions = objectMapper.readTree(rule.getConditions());
            if (conditions.has("category") && request.category() != null) {
                if (!conditions.get("category").asText().equalsIgnoreCase(request.category())) return false;
            }
            if (conditions.has("urgency") && request.urgency() != null) {
                if (!conditions.get("urgency").asText().equalsIgnoreCase(request.urgency())) return false;
            }
            if (conditions.has("intent") && request.intent() != null) {
                if (!conditions.get("intent").asText().equalsIgnoreCase(request.intent())) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
