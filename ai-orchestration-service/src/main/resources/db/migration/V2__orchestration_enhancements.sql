-- V2__orchestration_enhancements.sql
-- Outbox claim fields and attempt diagnostics for resilient workflow saga

ALTER TABLE orchestration_schema.outbox_events ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE orchestration_schema.outbox_events ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE orchestration_schema.outbox_events ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(100);
ALTER TABLE orchestration_schema.outbox_events ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMPTZ;
ALTER TABLE orchestration_schema.outbox_events ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_orchestration_outbox_claim 
    ON orchestration_schema.outbox_events(status, next_attempt_at) 
    WHERE status IN ('PENDING', 'RETRY');
