-- V3__multimodal_evidence.sql
-- Evidence jobs, artifacts, observations, and redactions for Multimodal Evidence Lab

CREATE TABLE IF NOT EXISTS analysis_schema.evidence_jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    attachment_id UUID NOT NULL,
    file_name VARCHAR(255),
    media_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    pipeline_status VARCHAR(50) NOT NULL DEFAULT 'QUEUED', -- QUEUED, EXTRACTING, REDACTING, ANALYZING, READY, PARTIAL, FAILED, BLOCKED_REDACTION, TOMBSTONED
    consent_granted BOOLEAN NOT NULL DEFAULT FALSE,
    retention_class VARCHAR(50) NOT NULL DEFAULT 'STANDARD_30D',
    attempt INT NOT NULL DEFAULT 0,
    lease_expires_at TIMESTAMPTZ,
    tool_versions JSONB,
    original_object_key VARCHAR(500),
    raw_content TEXT,
    error_details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_jobs_tenant_ticket ON analysis_schema.evidence_jobs(tenant_id, ticket_id);
CREATE INDEX IF NOT EXISTS idx_evidence_jobs_tenant_status ON analysis_schema.evidence_jobs(tenant_id, pipeline_status);

CREATE TABLE IF NOT EXISTS analysis_schema.evidence_artifacts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL REFERENCES analysis_schema.evidence_jobs(id) ON DELETE CASCADE,
    ticket_id UUID NOT NULL,
    artifact_type VARCHAR(50) NOT NULL, -- SCREENSHOT_REDACTED, PDF_REDACTED, LOG_REDACTED, CSV_SANITIZED, AUDIO_TRANSCRIPT, VIDEO_FRAME
    redacted_object_key VARCHAR(500) NOT NULL,
    page_or_frame INT,
    timestamp_seconds DOUBLE PRECISION,
    checksum_sha256 VARCHAR(64) NOT NULL,
    sensitivity_class VARCHAR(50) NOT NULL DEFAULT 'INTERNAL',
    redacted_content TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_artifacts_job ON analysis_schema.evidence_artifacts(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_evidence_artifacts_ticket ON analysis_schema.evidence_artifacts(tenant_id, ticket_id);

CREATE TABLE IF NOT EXISTS analysis_schema.evidence_observations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL REFERENCES analysis_schema.evidence_jobs(id) ON DELETE CASCADE,
    ticket_id UUID NOT NULL,
    observation_type VARCHAR(50) NOT NULL, -- ERROR_CODE, STACK_FINGERPRINT, INVOICE_FACT, UI_LABEL, REPRODUCTION_STEP, TRANSCRIPT_SEGMENT, FAILURE_TIMESTAMP
    code_or_key VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    source_coordinates JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_obs_job ON analysis_schema.evidence_observations(tenant_id, job_id);
CREATE INDEX IF NOT EXISTS idx_evidence_obs_ticket ON analysis_schema.evidence_observations(tenant_id, ticket_id);

CREATE TABLE IF NOT EXISTS analysis_schema.evidence_redactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL REFERENCES analysis_schema.evidence_jobs(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- PII_EMAIL, PII_NAME, SECRET_TOKEN, FINANCIAL
    source_location VARCHAR(255) NOT NULL,
    mask_method VARCHAR(50) NOT NULL DEFAULT 'MASK_HASH',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_redactions_job ON analysis_schema.evidence_redactions(tenant_id, job_id);
