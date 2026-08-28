-- V1__init_analysis_schema.sql
-- Analysis Service Schema matching Section 9.5 of RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md

CREATE SCHEMA IF NOT EXISTS analysis_schema;

CREATE TABLE IF NOT EXISTS analysis_schema.prompt_versions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    template_body TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prompt_name_version UNIQUE (name, version)
);

CREATE TABLE IF NOT EXISTS analysis_schema.analysis_results (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    intent VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    sentiment VARCHAR(50) NOT NULL,
    sentiment_confidence DOUBLE PRECISION,
    urgency VARCHAR(50) NOT NULL,
    urgency_confidence DOUBLE PRECISION,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    redacted_entities JSONB,
    policy_flags JSONB,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    raw_output_hash VARCHAR(255) NOT NULL,
    validation_outcome VARCHAR(50) NOT NULL DEFAULT 'VALID',
    latency_ms BIGINT NOT NULL,
    tokens_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_ticket ON analysis_schema.analysis_results(ticket_id);
CREATE INDEX IF NOT EXISTS idx_analysis_tenant_time ON analysis_schema.analysis_results(tenant_id, created_at DESC);
