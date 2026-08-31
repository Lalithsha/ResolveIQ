package com.resolveiq.routing.adapter.in.web;

import com.resolveiq.routing.application.dto.*;
import com.resolveiq.routing.application.service.RoutingEngineService;
import com.resolveiq.routing.domain.model.RoutingRule;
import com.resolveiq.routing.domain.model.Team;
import com.resolveiq.routing.domain.model.Agent;
import com.resolveiq.routing.domain.model.SlaPolicy;
import com.resolveiq.routing.domain.repository.AgentRepository;
import com.resolveiq.routing.domain.repository.SlaPolicyRepository;
import com.resolveiq.routing.domain.repository.RoutingRuleRepository;
import com.resolveiq.routing.domain.repository.TeamRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routing")
public class RoutingController {

    private final RoutingEngineService routingEngineService;
    private final TeamRepository teamRepository;
    private final RoutingRuleRepository ruleRepository;
    private final AgentRepository agentRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    public RoutingController(
        RoutingEngineService routingEngineService,
        TeamRepository teamRepository,
        RoutingRuleRepository ruleRepository,
        AgentRepository agentRepository,
        SlaPolicyRepository slaPolicyRepository
    ) {
        this.routingEngineService = routingEngineService;
        this.teamRepository = teamRepository;
        this.ruleRepository = ruleRepository;
        this.agentRepository = agentRepository;
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @PostMapping("/decide")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SYSTEM','ADMIN')")
    public ResponseEntity<RoutingDecisionResponse> decide(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @Valid @RequestBody RoutingDecisionRequest request) {
        if (request.tenantId() == null || !tenantId.equals(request.tenantId())) {
            throw new SecurityException("Request tenant does not match authenticated tenant");
        }
        RoutingDecisionResponse response = routingEngineService.decideRouting(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teams")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','KNOWLEDGE_MANAGER','ADMIN','AUDITOR')")
    public ResponseEntity<List<Team>> listTeams(@RequestHeader(value = "X-Tenant-Id") UUID tenantId) {
        List<Team> teams = teamRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/agents")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','KNOWLEDGE_MANAGER','ADMIN','AUDITOR')")
    public ResponseEntity<List<Agent>> listAgents(@RequestHeader(value = "X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(agentRepository.findByTenantId(tenantId));
    }

    @GetMapping("/rules")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEAM_LEAD','ADMIN','AUDITOR')")
    public ResponseEntity<List<RoutingRule>> listRules(@RequestHeader(value = "X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ruleRepository.findByTenantIdOrderByPriorityOrderAsc(tenantId));
    }

    @GetMapping("/sla-policies")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','ADMIN','AUDITOR')")
    public ResponseEntity<List<SlaPolicy>> listSlaPolicies(@RequestHeader(value = "X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(slaPolicyRepository.findByTenantId(tenantId));
    }

    @PostMapping("/teams")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Team> createTeam(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @Valid @RequestBody CreateTeamRequest request
    ) {
        Team team = new Team(UUID.randomUUID(), tenantId, request.name(), request.description(), request.maxActiveTickets());
        teamRepository.save(team);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    @PostMapping("/rules")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoutingRule> createRule(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @Valid @RequestBody CreateRoutingRuleRequest request
    ) {
        RoutingRule rule = new RoutingRule(
            UUID.randomUUID(),
            tenantId,
            request.name(),
            "v1.0",
            request.conditions(),
            request.targetTeamId(),
            request.priorityOrder()
        );
        ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PatchMapping("/rules/{id}/active")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoutingRule> setRuleActive(
        @RequestHeader(value = "X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id,
        @RequestBody java.util.Map<String, Boolean> body
    ) {
        RoutingRule rule = ruleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Routing rule not found"));
        if (!body.containsKey("active")) throw new IllegalArgumentException("active is required");
        rule.setActive(Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.ok(ruleRepository.save(rule));
    }
}
