ALTER TABLE analysis_schema.analysis_results
    ADD COLUMN IF NOT EXISTS input_tokens INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS output_tokens INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS estimated_cost_micros BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS guardrail_outcome VARCHAR(50) NOT NULL DEFAULT 'LEGACY_UNKNOWN',
    ADD COLUMN IF NOT EXISTS guardrail_findings JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS provider_request_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_analysis_tenant_guardrail_time
    ON analysis_schema.analysis_results(tenant_id, guardrail_outcome, created_at DESC);
