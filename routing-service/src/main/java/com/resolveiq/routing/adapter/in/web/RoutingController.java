package com.resolveiq.routing.adapter.in.web;

import com.resolveiq.routing.application.dto.*;
import com.resolveiq.routing.application.service.RoutingEngineService;
import com.resolveiq.routing.domain.model.RoutingRule;
import com.resolveiq.routing.domain.model.Team;
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

    public RoutingController(
        RoutingEngineService routingEngineService,
        TeamRepository teamRepository,
        RoutingRuleRepository ruleRepository
    ) {
        this.routingEngineService = routingEngineService;
        this.teamRepository = teamRepository;
        this.ruleRepository = ruleRepository;
    }

    @PostMapping("/decide")
    public ResponseEntity<RoutingDecisionResponse> decide(@Valid @RequestBody RoutingDecisionRequest request) {
        RoutingDecisionResponse response = routingEngineService.decideRouting(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Team>> listTeams(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<Team> teams = teamRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(teams);
    }

    @PostMapping("/teams")
    public ResponseEntity<Team> createTeam(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @Valid @RequestBody CreateTeamRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        Team team = new Team(UUID.randomUUID(), tenantId, request.name(), request.description(), request.maxActiveTickets());
        teamRepository.save(team);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    @PostMapping("/rules")
    public ResponseEntity<RoutingRule> createRule(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @Valid @RequestBody CreateRoutingRuleRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
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
}
