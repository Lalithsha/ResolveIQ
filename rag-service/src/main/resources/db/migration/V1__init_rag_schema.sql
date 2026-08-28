-- V1__init_rag_schema.sql
-- Knowledge and RAG Service Schema matching Section 9.7 of RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md

CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS rag_schema;

CREATE TABLE IF NOT EXISTS rag_schema.knowledge_documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    category VARCHAR(100) NOT NULL,
    product VARCHAR(100),
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    active_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rag_schema.knowledge_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES rag_schema.knowledge_documents(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    published_by_user_id UUID,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rag_schema.knowledge_chunks (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    document_id UUID NOT NULL REFERENCES rag_schema.knowledge_documents(id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES rag_schema.knowledge_versions(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(255) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    embedding vector(1536),
    tsv_content tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_tsv ON rag_schema.knowledge_chunks USING GIN(tsv_content);

CREATE TABLE IF NOT EXISTS rag_schema.resolved_cases (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    original_ticket_id UUID NOT NULL,
    sanitized_subject VARCHAR(500) NOT NULL,
    sanitized_description TEXT NOT NULL,
    sanitized_resolution TEXT NOT NULL,
    category VARCHAR(100),
    tags TEXT[],
    approved_by_user_id UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rag_schema.resolved_case_chunks (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    resolved_case_id UUID NOT NULL REFERENCES rag_schema.resolved_cases(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(255) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    embedding vector(1536),
    tsv_content tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resolved_case_chunks_tsv ON rag_schema.resolved_case_chunks USING GIN(tsv_content);

CREATE TABLE IF NOT EXISTS rag_schema.retrieval_runs (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    query_text TEXT NOT NULL,
    strategy VARCHAR(50) NOT NULL DEFAULT 'HYBRID_RRF', -- VECTOR_ONLY, FTS_ONLY, HYBRID_RRF
    top_k INT NOT NULL DEFAULT 5,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rag_schema.citation_records (
    id UUID PRIMARY KEY,
    suggestion_id UUID NOT NULL,
    retrieval_run_id UUID REFERENCES rag_schema.retrieval_runs(id),
    source_type VARCHAR(50) NOT NULL, -- KNOWLEDGE_ARTICLE, RESOLVED_CASE
    source_id UUID NOT NULL,
    version_id UUID,
    chunk_id UUID NOT NULL,
    citation_text TEXT NOT NULL,
    confidence_score DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
