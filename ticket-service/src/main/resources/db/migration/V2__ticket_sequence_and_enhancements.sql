-- V2__ticket_sequence_and_enhancements.sql
-- Adds sequence-backed ticket numbers, robust multi-attribute idempotency, attachments, and outbox fields

CREATE SEQUENCE IF NOT EXISTS ticket_schema.ticket_number_seq START WITH 100001 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS ticket_schema.idempotent_commands (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    operation VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL, -- IN_PROGRESS, COMPLETED, FAILED_RETRYABLE
    response_code INT,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_idempotent_command UNIQUE (tenant_id, actor_id, operation, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotent_commands_lookup 
    ON ticket_schema.idempotent_commands(tenant_id, actor_id, operation, idempotency_key);

CREATE TABLE IF NOT EXISTS ticket_schema.ticket_attachments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    uploader_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    scan_status VARCHAR(50) NOT NULL DEFAULT 'CLEAN', -- PENDING, CLEAN, INFECTED
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket 
    ON ticket_schema.ticket_attachments(ticket_id);

ALTER TABLE ticket_schema.outbox_events ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE ticket_schema.outbox_events ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ticket_schema.outbox_events ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(100);
ALTER TABLE ticket_schema.outbox_events ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMPTZ;
ALTER TABLE ticket_schema.outbox_events ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(100);
