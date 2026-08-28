-- V1__init_routing_schema.sql
-- Routing Service Schema matching Section 9.6 of RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md

CREATE SCHEMA IF NOT EXISTS routing_schema;

CREATE TABLE IF NOT EXISTS routing_schema.teams (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    max_active_tickets INT NOT NULL DEFAULT 50,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS routing_schema.team_skills (
    team_id UUID NOT NULL REFERENCES routing_schema.teams(id) ON DELETE CASCADE,
    skill VARCHAR(50) NOT NULL,
    PRIMARY KEY (team_id, skill)
);

CREATE TABLE IF NOT EXISTS routing_schema.agents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    team_id UUID NOT NULL REFERENCES routing_schema.teams(id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, BUSY, OFFLINE
    active_ticket_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS routing_schema.agent_skills (
    agent_id UUID NOT NULL REFERENCES routing_schema.agents(id) ON DELETE CASCADE,
    skill VARCHAR(50) NOT NULL,
    proficiency_level INT NOT NULL DEFAULT 1,
    PRIMARY KEY (agent_id, skill)
);

CREATE TABLE IF NOT EXISTS routing_schema.sla_policies (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    first_response_target_minutes INT NOT NULL,
    resolution_target_minutes INT NOT NULL,
    business_hours_only BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS routing_schema.routing_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL DEFAULT 'v1.0',
    conditions JSONB NOT NULL,
    target_team_id UUID NOT NULL REFERENCES routing_schema.teams(id),
    priority_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS routing_schema.routing_decisions (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    matched_rule_id UUID REFERENCES routing_schema.routing_rules(id),
    rule_version VARCHAR(50),
    target_team_id UUID NOT NULL REFERENCES routing_schema.teams(id),
    assigned_agent_id UUID REFERENCES routing_schema.agents(id),
    reason TEXT NOT NULL,
    input_facts JSONB NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
