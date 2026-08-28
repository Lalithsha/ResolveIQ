-- V1__init_ticket_schema.sql
-- Ticket Service Schema matching Section 9.3 of RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md

CREATE SCHEMA IF NOT EXISTS ticket_schema;

CREATE TABLE IF NOT EXISTS ticket_schema.tickets (
    id UUID PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    team_id UUID,
    assigned_agent_id UUID,
    subject VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    category VARCHAR(100),
    channel VARCHAR(50) NOT NULL DEFAULT 'WEB',
    sla_policy_id UUID,
    first_response_due_at TIMESTAMPTZ,
    resolution_due_at TIMESTAMPTZ,
    ai_triage_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    latest_suggestion_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tickets_tenant_status ON ticket_schema.tickets(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_tenant_customer ON ticket_schema.tickets(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_tenant_team ON ticket_schema.tickets(tenant_id, team_id);
CREATE INDEX IF NOT EXISTS idx_tickets_tenant_assigned ON ticket_schema.tickets(tenant_id, assigned_agent_id);

CREATE TABLE IF NOT EXISTS ticket_schema.ticket_messages (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_id ON ticket_schema.ticket_messages(ticket_id, created_at ASC);

CREATE TABLE IF NOT EXISTS ticket_schema.ticket_status_history (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    changed_by_user_id UUID,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket_schema.ai_suggestions (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    suggested_response TEXT NOT NULL,
    confidence_score DOUBLE PRECISION,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    citations JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW', -- PENDING_REVIEW, ACCEPTED, EDITED, REJECTED, INVALIDATED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    reviewed_by_agent_id UUID
);

CREATE TABLE IF NOT EXISTS ticket_schema.suggestion_feedback (
    id UUID PRIMARY KEY,
    suggestion_id UUID NOT NULL REFERENCES ticket_schema.ai_suggestions(id) ON DELETE CASCADE,
    ticket_id UUID NOT NULL,
    agent_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- ACCEPTED, EDITED, REJECTED, REGENERATED
    rejection_reason TEXT,
    edited_content TEXT,
    rating INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket_schema.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PUBLISHED, RETRY, DEAD
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON ticket_schema.outbox_events(status, created_at ASC);

CREATE TABLE IF NOT EXISTS ticket_schema.processed_events (
    event_id UUID PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket_schema.idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    tenant_id UUID NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    response_body JSONB,
    status_code INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);
