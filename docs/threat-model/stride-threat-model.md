# STRIDE Threat Model & Security Controls

## System Overview
ResolveIQ processes customer inquiries, routes tickets, and generates AI drafts with grounded citations. The threat model below evaluates all 6 STRIDE threat categories against the architecture.

---

## 1. Threat Analysis Table

| STRIDE Category | Threat Description | Attack Vector | Mitigating Architectural Control | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Spoofing** | Attacker impersonates an agent or customer | Forged JWT access token or expired session | HMAC-SHA256 signature verification, 15m access token expiry, SHA-256 hashed refresh tokens with rotation and reuse detection. | ✅ Implemented |
| **Tampering** | Unauthorized modification of ticket status or suggestions | Cross-tenant / IDOR direct object manipulation | Tenant ID enforced in every SQL repository query (`findByIdAndTenantId`). State machine validates transitions. | ✅ Implemented |
| **Repudiation** | Operator denies performing sensitive action or sending reply | Unaudited agent replies / status changes | `ticket_status_history`, `suggestion_feedback`, and `security_audit_events` log immutable user IDs and timestamps. | ✅ Implemented |
| **Information Disclosure** | Cross-tenant knowledge retrieval or PII leakage | Unfiltered vector similarity search | `tenant_id` and `active_version_id` strictly applied as SQL pre-filters prior to cosine distance scoring; PII redaction on ingest. | ✅ Implemented |
| **Denial of Service** | Model exhaustion / LLM billing spike / Kafka overload | Flooding ticket creation API | Edge rate limiting in API gateway; async decoupling via Kafka; circuit breakers & timeout boundaries on all REST clients. | ✅ Implemented |
| **Elevation of Privilege** | Customer accesses agent workspace or admin controls | Missing RBAC check on endpoints | Spring Security `@PreAuthorize` method security and gateway route path predicate restrictions per persona. | ✅ Implemented |

---

## 2. Special AI Security Guardrails

1. **Prompt Injection Defense:** Strips system delimiters (`<|system|>`, `<|assistant|>`, markdown fences) before prompt interpolation.
2. **Strict Human-in-the-Loop:** System architecture has zero auto-sending paths to external channels; all model suggestions must be reviewed and approved by an agent.
3. **Citation Grounding & Abstention:** Low-confidence (< 0.65) or uncited outputs trigger deterministic abstention fallback.
