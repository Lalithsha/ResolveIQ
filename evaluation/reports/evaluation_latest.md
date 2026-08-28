# ResolveIQ AI Evaluation Report

**Generated At:** 2026-08-28T20:07:23.724741Z  
**Dataset:** `evaluation/datasets/eval_ground_truth.json` (5 benchmark queries)  
**Status:** ✅ ALL GATES PASSED

---

## 1. Summary of Key Quality Metrics

| Metric | Measured Value | Target Gate | Status |
| :--- | :--- | :--- | :--- |
| **Retrieval Recall@5** | **100.00%** | >= 85.0% | ✅ Passed |
| **Mean Reciprocal Rank (MRR)** | **1.0000** | >= 0.7500 | ✅ Passed |
| **Auto-Send Rate** | **0.00%** | 0.00% (Strict) | ✅ Invariant Enforced |
| **PII Leakage Rate** | **0.00%** | < 0.01% | ✅ Invariant Enforced |

---

## 2. Benchmark Query Breakdown

1. **Query:** "Duplicate payment charge on credit card for invoice" → **Expected:** `['KB-104']` (Rank: 1) ✅
2. **Query:** "Okta SSO SAML 401 signature validation error" → **Expected:** `['KB-105']` (Rank: 1) ✅
3. **Query:** "Change VAT tax ID number on company invoice" → **Expected:** `['KB-108']` (Rank: 1) ✅
4. **Query:** "Delivery status says delivered but package is missing" → **Expected:** `['KB-112']` (Rank: 1) ✅
5. **Query:** "Webhook receiver getting 429 rate limit errors" → **Expected:** `['KB-119']` (Rank: 1) ✅

---

## 3. Methodology & Governance Invariants

1. **Hybrid Retrieval:** PostgreSQL `tsvector` full-text search combined with `pgvector` cosine embeddings fused via Reciprocal Rank Fusion (RRF $k=60$).
2. **Draft Safety:** Enforces grounded citations with explicit abstention when confidence < 0.65.
3. **Strict Human-in-the-Loop:** Automated actions never send unreviewed text to customer channels.
