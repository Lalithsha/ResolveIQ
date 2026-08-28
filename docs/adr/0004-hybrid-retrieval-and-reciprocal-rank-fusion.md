# ADR 0004: Hybrid Retrieval and Reciprocal Rank Fusion

## Status
Accepted

## Context
Pure vector similarity search struggles with exact keyword matching (error codes, product SKUs, specific acronyms), while pure lexical search (BM25 / full-text search) fails to capture semantic synonyms and user intent variations.

## Decision
Implement a two-stage hybrid retrieval pipeline:
1. Lexical retrieval using PostgreSQL `tsvector` with English dictionary GIN indexing.
2. Semantic vector retrieval using `pgvector` cosine similarity (`<=>`).
3. Combine and rank candidate chunks using Reciprocal Rank Fusion (RRF with `k=60`).
4. Apply pre-filtering on `tenant_id`, active version, product, and visibility before ranking.

## Consequences
- **Positive:** High recall and precision for technical error codes and natural language queries alike.
- **Negative:** Slightly higher compute overhead to execute both lexical and vector queries.
- **Reversal Trigger:** If offline evaluation demonstrates that pure vector or fine-tuned dense embeddings outperform hybrid retrieval by >15% MRR across all query categories.
