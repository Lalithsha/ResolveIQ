# ADR 0008: Resolved-Case Privacy and Sanitization Policy

## Status
Accepted

## Context
Historic resolved support cases contain valuable troubleshooting knowledge for retrieval. However, raw tickets often contain Personally Identifiable Information (PII) such as customer names, email addresses, credit card numbers, order IDs, IP addresses, or internal keys. Feeding un-sanitized tickets into retrieval risks data leakage across tenants or to agents who should not view specific customer data.

## Decision
1. Resolved tickets are **never** indexed automatically.
2. Ingestion requires a two-step privacy gate:
   - Automated regex and entity redaction for emails, phone numbers, cards, and addresses.
   - Explicit human review and approval by an authorized Knowledge Manager or Team Lead.
3. Only approved records (`resolved_cases`) with `sanitized = true` are chunked and embedded in `rag_schema`.
4. Tenant boundaries are strictly enforced during vector retrieval filters.

## Consequences
- **Positive:** Guaranteed customer privacy, GDPR/CCPA compliance, zero cross-tenant leakage.
- **Negative:** Knowledge manager review required to convert resolved tickets into retrieval assets.
- **Reversal Trigger:** None.
