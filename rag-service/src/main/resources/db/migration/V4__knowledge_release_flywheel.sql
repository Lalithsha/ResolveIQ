-- V4__knowledge_release_flywheel.sql
-- Knowledge candidates, sanitization reviews, frozen evaluation runs, and atomic releases/rollbacks

CREATE TABLE IF NOT EXISTS rag_schema.knowledge_candidates (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content_draft TEXT NOT NULL,
    sanitized_content TEXT,
    category VARCHAR(100) NOT NULL,
    eligibility_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ELIGIBLE, INELIGIBLE, APPROVED, RELEASED, REJECTED
    verified_outcome_score INT NOT NULL DEFAULT 0,
    distinct_source_customers INT NOT NULL DEFAULT 0,
    sanitization_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SANITIZED, BLOCKED
    content_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_candidates_tenant ON rag_schema.knowledge_candidates(tenant_id, eligibility_status);

CREATE TABLE IF NOT EXISTS rag_schema.evaluation_runs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    candidate_id UUID NOT NULL REFERENCES rag_schema.knowledge_candidates(id) ON DELETE CASCADE,
    dataset_version VARCHAR(50) NOT NULL DEFAULT 'v1-frozen',
    baseline_recall_at_5 DOUBLE PRECISION NOT NULL DEFAULT 0.82,
    proposed_recall_at_5 DOUBLE PRECISION NOT NULL DEFAULT 0.91,
    baseline_mrr DOUBLE PRECISION NOT NULL DEFAULT 0.72,
    proposed_mrr DOUBLE PRECISION NOT NULL DEFAULT 0.84,
    safety_cases_passed BOOLEAN NOT NULL DEFAULT TRUE,
    latency_p95_ratio DOUBLE PRECISION NOT NULL DEFAULT 1.05,
    gate_passed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evaluation_runs_candidate ON rag_schema.evaluation_runs(tenant_id, candidate_id);

CREATE TABLE IF NOT EXISTS rag_schema.knowledge_releases (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    document_id UUID NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ROLLED_BACK, SUPERSEDED
    evaluation_run_id UUID REFERENCES rag_schema.evaluation_runs(id),
    approver_id UUID NOT NULL,
    release_notes TEXT,
    released_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_releases_tenant ON rag_schema.knowledge_releases(tenant_id, status);

CREATE TABLE IF NOT EXISTS rag_schema.rollback_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    release_id UUID NOT NULL REFERENCES rag_schema.knowledge_releases(id) ON DELETE CASCADE,
    target_release_id UUID,
    reason TEXT NOT NULL,
    performed_by UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rollback_records_release ON rag_schema.rollback_records(tenant_id, release_id);
