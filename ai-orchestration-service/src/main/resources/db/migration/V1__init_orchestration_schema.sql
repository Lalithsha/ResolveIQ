-- V1__init_orchestration_schema.sql
-- Orchestration Service Schema matching Section 9.4 of RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md

CREATE SCHEMA IF NOT EXISTS orchestration_schema;

CREATE TABLE IF NOT EXISTS orchestration_schema.workflow_instances (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    workflow_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    current_step VARCHAR(100),
    deadline_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_instances_ticket ON orchestration_schema.workflow_instances(ticket_id);
CREATE INDEX IF NOT EXISTS idx_workflow_instances_status ON orchestration_schema.workflow_instances(tenant_id, status);

CREATE TABLE IF NOT EXISTS orchestration_schema.workflow_steps (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES orchestration_schema.workflow_instances(id) ON DELETE CASCADE,
    step_name VARCHAR(100) NOT NULL,
    step_order INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    input_payload JSONB,
    output_payload JSONB,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS orchestration_schema.workflow_attempts (
    id UUID PRIMARY KEY,
    step_id UUID NOT NULL REFERENCES orchestration_schema.workflow_steps(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS orchestration_schema.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS orchestration_schema.processed_events (
    event_id UUID PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
