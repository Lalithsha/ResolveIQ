# ResolveIQ AI Evaluation & Benchmark Quality Report

**Generated At:** `2026-08-30T14:48:03.568161+00:00`  
**Dataset:** `evaluation/datasets/eval_ground_truth.json` (100 test cases across 20 knowledge domains)  
**Status:** ✅ ALL GATES PASSED (DETERMINISTIC SHA-256 HARNESS)

---

## 1. Executive Summary & Production Gate Status

| Metric | Measured Value | Target Gate | Status |
| :--- | :--- | :--- | :--- |
| **Retrieval Recall@5** | **94.00%** | >= 85.0% | ✅ Passed |
| **Mean Reciprocal Rank (MRR)** | **0.8344** | >= 0.7500 | ✅ Passed |
| **Retrieval Latency (p50)** | **0.25 ms** | < 100 ms | ✅ Passed |
| **Retrieval Latency (p95)** | **0.41 ms** | < 250 ms | ✅ Passed |
| **Autonomous Send Rate** | **0.00%** | 0.00% (Strict Invariant) | ✅ Enforced |
| **Cross-Tenant Leakage Rate** | **0.00%** | 0.00% (Zero Tolerance) | ✅ Enforced |
| **PII Leakage Rate** | **0.00%** | < 0.01% | ✅ Enforced |

---

## 2. Evaluation Methodology

1. **Hybrid Retrieval:** Full-text token inverted indexing combined with dense embedding cosine similarity, fused via **Reciprocal Rank Fusion (k=60)**.
2. **Deterministic Hashing:** Embeddings use stable SHA-256 hashing to guarantee 100% reproducible benchmark metrics across environments.
3. **Deterministic Citations:** Grounded drafts reference explicit chunk offsets [1], [2] with mandatory human review before dispatch.
4. **Strict Human-in-the-Loop:** Automated AI processes produce suggestions in status `PENDING_REVIEW` with **0.00% autonomous send rate**.
