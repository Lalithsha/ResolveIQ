-- V7__verified_resolution_flywheel.sql
-- Resolution attempts, customer outcomes, repeat contact detection, and versioned scoring

CREATE TABLE IF NOT EXISTS ticket_schema.ticket_resolutions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL DEFAULT 1,
    resolver_id UUID NOT NULL,
    resolved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmation_window_expires_at TIMESTAMPTZ NOT NULL,
    scheduled_closure_at TIMESTAMPTZ,
    solution_fingerprint VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'AWAITING_CONFIRMATION', -- AWAITING_CONFIRMATION, CONFIRMED, PARTIAL, REJECTED, NO_RESPONSE, SUPERSEDED
    score INT NOT NULL DEFAULT 0,
    score_formula_version VARCHAR(50) NOT NULL DEFAULT 'v1',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ticket_resolutions_ticket ON ticket_schema.ticket_resolutions(tenant_id, ticket_id, attempt_number);
CREATE INDEX IF NOT EXISTS idx_ticket_resolutions_status ON ticket_schema.ticket_resolutions(tenant_id, status);

CREATE TABLE IF NOT EXISTS ticket_schema.resolution_outcomes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    resolution_id UUID NOT NULL REFERENCES ticket_schema.ticket_resolutions(id) ON DELETE CASCADE,
    source VARCHAR(50) NOT NULL, -- CUSTOMER, REOPEN, REPEAT_CONTACT, AGENT_CORRECTION
    rating VARCHAR(50) NOT NULL, -- YES, PARTLY, NO, NO_RESPONSE
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    weight INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resolution_outcomes_res ON ticket_schema.resolution_outcomes(tenant_id, resolution_id);

CREATE TABLE IF NOT EXISTS ticket_schema.repeat_contact_signals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    prior_ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    current_ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    similarity DOUBLE PRECISION NOT NULL,
    time_delta_seconds BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUGGESTED', -- SUGGESTED, CONFIRMED, DISMISSED
    reviewer_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_repeat_contact_customer ON ticket_schema.repeat_contact_signals(tenant_id, customer_id, created_at);
