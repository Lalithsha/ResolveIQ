# ADR 0003: PostgreSQL and pgvector for Vector Search

## Status
Accepted

## Context
ResolveIQ requires relational persistence for tickets, users, audit trails, and workflows, along with vector similarity search for knowledge articles and sanitized resolved cases. Introducing a separate specialized vector database (Pinecone, Qdrant, Milvus) increases operational complexity, requires cross-system consistency mechanisms, and adds maintenance overhead.

## Decision
Use PostgreSQL 16 with the `pgvector` extension. Store relational business entities and document vector embeddings within the same database engine (separated by service schemas). Use exact cosine similarity for small datasets and HNSW indexes as dataset size grows. Combine `tsvector` with vector search for hybrid retrieval.

## Consequences
- **Positive:** Single database infrastructure to operate, backup, and restore; transactional integrity; zero cross-system sync latency.
- **Negative:** Vector scaling is bound to PostgreSQL instance compute and memory.
- **Reversal Trigger:** Retrieval scale exceeds 500,000 active embeddings and measured p95 latency exceeds 800 ms despite HNSW indexing.
