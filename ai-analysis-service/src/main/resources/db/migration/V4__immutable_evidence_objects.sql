ALTER TABLE analysis_schema.evidence_jobs
    ADD COLUMN IF NOT EXISTS original_checksum_sha256 VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_evidence_job_attachment
    ON analysis_schema.evidence_jobs (tenant_id, ticket_id, attachment_id);
