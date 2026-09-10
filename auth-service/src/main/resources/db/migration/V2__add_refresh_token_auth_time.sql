-- V2__add_refresh_token_auth_time.sql
-- SEC-03: Carry immutable original authentication time through refresh-token rotation

ALTER TABLE auth_schema.refresh_tokens ADD COLUMN IF NOT EXISTS auth_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
