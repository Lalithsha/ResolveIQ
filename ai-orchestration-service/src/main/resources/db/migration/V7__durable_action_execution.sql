ALTER TABLE orchestration_schema.action_executions
    ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS client_idempotency_key VARCHAR(128),
    ADD COLUMN IF NOT EXISTS actor_id UUID;

UPDATE orchestration_schema.action_executions
SET client_idempotency_key = provider_idempotency_key
WHERE client_idempotency_key IS NULL;

ALTER TABLE orchestration_schema.action_executions
    ALTER COLUMN client_idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_action_execution_provider_key
    ON orchestration_schema.action_executions(tenant_id, provider_idempotency_key);

CREATE UNIQUE INDEX IF NOT EXISTS uq_action_execution_client_key
    ON orchestration_schema.action_executions(tenant_id, client_idempotency_key);

CREATE INDEX IF NOT EXISTS idx_action_execution_recovery
    ON orchestration_schema.action_executions(status, lease_expires_at, started_at);
