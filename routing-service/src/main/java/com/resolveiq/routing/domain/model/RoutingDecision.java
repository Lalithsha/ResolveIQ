package com.resolveiq.routing.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routing_decisions", schema = "routing_schema")
public class RoutingDecision {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "matched_rule_id")
    private UUID matchedRuleId;

    @Column(name = "rule_version", length = 50)
    private String ruleVersion;

    @Column(name = "target_team_id", nullable = false)
    private UUID targetTeamId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "input_facts", columnDefinition = "JSONB")
    private String inputFacts;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    public RoutingDecision() {}

    public RoutingDecision(
        UUID ticketId,
        UUID tenantId,
        UUID matchedRuleId,
        String ruleVersion,
        UUID targetTeamId,
        UUID assignedAgentId,
        String reason,
        String inputFacts
    ) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.matchedRuleId = matchedRuleId;
        this.ruleVersion = ruleVersion;
        this.targetTeamId = targetTeamId;
        this.assignedAgentId = assignedAgentId;
        this.reason = reason;
        this.inputFacts = inputFacts;
        this.decidedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMatchedRuleId() { return matchedRuleId; }
    public String getRuleVersion() { return ruleVersion; }
    public UUID getTargetTeamId() { return targetTeamId; }
    public UUID getAssignedAgentId() { return assignedAgentId; }
    public String getReason() { return reason; }
    public String getInputFacts() { return inputFacts; }
    public Instant getDecidedAt() { return decidedAt; }
}
