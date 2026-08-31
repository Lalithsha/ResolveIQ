ALTER TABLE rag_schema.knowledge_versions
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS created_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS review_note TEXT,
    ADD COLUMN IF NOT EXISTS superseded_at TIMESTAMPTZ;

UPDATE rag_schema.knowledge_versions
SET status = CASE WHEN published_at IS NOT NULL THEN 'PUBLISHED' ELSE 'DRAFT' END,
    created_by_user_id = COALESCE(created_by_user_id, published_by_user_id),
    submitted_at = CASE WHEN published_at IS NOT NULL THEN COALESCE(submitted_at, published_at) ELSE submitted_at END,
    reviewed_by_user_id = COALESCE(reviewed_by_user_id, published_by_user_id),
    reviewed_at = CASE WHEN published_at IS NOT NULL THEN COALESCE(reviewed_at, published_at) ELSE reviewed_at END
WHERE created_by_user_id IS NULL OR status = 'DRAFT';

CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_version_number
    ON rag_schema.knowledge_versions(document_id, version_number);

CREATE INDEX IF NOT EXISTS idx_knowledge_versions_document_status
    ON rag_schema.knowledge_versions(document_id, status, version_number DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_active_lookup
    ON rag_schema.knowledge_chunks(tenant_id, document_id, version_id);
