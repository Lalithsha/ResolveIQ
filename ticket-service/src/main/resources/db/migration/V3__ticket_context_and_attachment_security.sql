ALTER TABLE ticket_schema.tickets
    ADD COLUMN IF NOT EXISTS intent VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sentiment VARCHAR(30),
    ADD COLUMN IF NOT EXISTS urgency VARCHAR(30),
    ADD COLUMN IF NOT EXISTS triage_confidence DOUBLE PRECISION;

ALTER TABLE ticket_schema.ticket_attachments
    ADD COLUMN IF NOT EXISTS sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS scan_engine VARCHAR(100),
    ADD COLUMN IF NOT EXISTS scan_details VARCHAR(500),
    ADD COLUMN IF NOT EXISTS scanned_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_tickets_tenant_sla
    ON ticket_schema.tickets(tenant_id, first_response_due_at, resolution_due_at);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_tenant_ticket_status
    ON ticket_schema.ticket_attachments(tenant_id, ticket_id, scan_status, created_at);
