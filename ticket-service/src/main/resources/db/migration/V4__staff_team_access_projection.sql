CREATE TABLE IF NOT EXISTS ticket_schema.staff_team_memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    team_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_staff_team_membership UNIQUE (tenant_id, user_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_team_access
    ON ticket_schema.staff_team_memberships(tenant_id, user_id, team_id)
    WHERE active = TRUE;
