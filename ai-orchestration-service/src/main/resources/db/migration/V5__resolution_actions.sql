-- V5__resolution_actions.sql
-- Policy-Controlled Resolution Actions Schema for AI Orchestration Service

CREATE SCHEMA IF NOT EXISTS orchestration_schema;

CREATE TABLE IF NOT EXISTS orchestration_schema.resolution_action_proposals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PROPOSED',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    input_payload JSONB NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    canonical_digest VARCHAR(64) NOT NULL,
    canonical_bytes TEXT NOT NULL,
    ai_rationale TEXT,
    evidence_ids TEXT,
    policy_version VARCHAR(50) NOT NULL DEFAULT '1.0',
    current_state_version VARCHAR(50) NOT NULL DEFAULT '1',
    version BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_action_proposals_ticket ON orchestration_schema.resolution_action_proposals(tenant_id, ticket_id);
CREATE INDEX IF NOT EXISTS idx_action_proposals_status ON orchestration_schema.resolution_action_proposals(tenant_id, status);

CREATE TABLE IF NOT EXISTS orchestration_schema.action_policy_decisions (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES orchestration_schema.resolution_action_proposals(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    decision VARCHAR(50) NOT NULL,
    matched_rules JSONB,
    required_permissions JSONB,
    required_approval_count INT NOT NULL DEFAULT 1,
    financial_limit_cents BIGINT,
    reason_codes JSONB,
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_policy_decisions_proposal ON orchestration_schema.action_policy_decisions(proposal_id);

CREATE TABLE IF NOT EXISTS orchestration_schema.action_approvals (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES orchestration_schema.resolution_action_proposals(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    decision VARCHAR(50) NOT NULL,
    approved_digest VARCHAR(64) NOT NULL,
    authentication_time TIMESTAMPTZ,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_action_approval UNIQUE (proposal_id, actor_id, approved_digest)
);

CREATE INDEX IF NOT EXISTS idx_action_approvals_proposal ON orchestration_schema.action_approvals(proposal_id);

CREATE TABLE IF NOT EXISTS orchestration_schema.action_executions (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES orchestration_schema.resolution_action_proposals(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    provider VARCHAR(100) NOT NULL,
    provider_idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    provider_reference VARCHAR(128),
    status VARCHAR(50) NOT NULL,
    sanitized_response JSONB,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_action_executions_proposal ON orchestration_schema.action_executions(proposal_id);
CREATE INDEX IF NOT EXISTS idx_action_executions_idempotency ON orchestration_schema.action_executions(tenant_id, provider_idempotency_key);

CREATE TABLE IF NOT EXISTS orchestration_schema.action_reconciliations (
    id UUID PRIMARY KEY,
    proposal_id UUID NOT NULL REFERENCES orchestration_schema.resolution_action_proposals(id) ON DELETE CASCADE,
    execution_id UUID NOT NULL REFERENCES orchestration_schema.action_executions(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    expected_state JSONB NOT NULL,
    observed_state JSONB NOT NULL,
    notes TEXT,
    reconciled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_action_reconciliations_proposal ON orchestration_schema.action_reconciliations(proposal_id);

-- Simulated Payment Storage (Durable state for simulated payment provider)
CREATE TABLE IF NOT EXISTS orchestration_schema.simulated_payments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_account_id VARCHAR(100) NOT NULL,
    payment_reference VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    original_amount_cents BIGINT NOT NULL,
    refunded_amount_cents BIGINT NOT NULL DEFAULT 0,
    reserved_refund_cents BIGINT NOT NULL DEFAULT 0,
    is_settled BOOLEAN NOT NULL DEFAULT TRUE,
    duplicate_of_reference VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_simulated_payments_ref UNIQUE (tenant_id, payment_reference)
);

CREATE TABLE IF NOT EXISTS orchestration_schema.simulated_payment_operations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider_idempotency_key VARCHAR(128) NOT NULL,
    payment_reference VARCHAR(100) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_simulated_payment_ops UNIQUE (tenant_id, provider_idempotency_key)
);

-- Simulated Account Identity Storage (Durable state for simulated identity provider)
CREATE TABLE IF NOT EXISTS orchestration_schema.simulated_accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT TRUE,
    lock_reason VARCHAR(255),
    last_verified_identity_at TIMESTAMPTZ,
    sessions_revoked_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_simulated_accounts_user UNIQUE (tenant_id, user_id)
);

CREATE TABLE IF NOT EXISTS orchestration_schema.simulated_account_operations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider_idempotency_key VARCHAR(128) NOT NULL,
    user_id UUID NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_simulated_account_ops UNIQUE (tenant_id, provider_idempotency_key)
);
