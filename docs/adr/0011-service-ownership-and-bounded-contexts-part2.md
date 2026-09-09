# ADR 0011: Service Ownership and Bounded Contexts for Part 2

## Status
Accepted

## Context
Part 2 introduces five differentiated capabilities: Support Incident Radar, policy-controlled resolution actions, omnichannel continuity, multimodal evidence processing, and the verified resolution flywheel. Expanding into numerous microservices would create significant network hop overhead, distributed transaction complexity, and shared database anti-patterns. We need crisp domain boundaries that align with existing bounded contexts without adding unnecessary services.

## Decision
We enforce strict ownership across existing services:
1. `ticket-service` owns: Support Incidents, incident clusters, customer impact, incident updates, canonical conversations, channel message metadata, channel identities, and customer resolution outcomes/repeat signals.
2. `ai-orchestration-service` owns: Durable resolution action proposals, policy evaluation, human approvals, action executions, reconciliations, and cluster detection scheduling.
3. `rag-service` owns: Tenant-scoped ticket similarity retrieval, sanitized evidence chunk embeddings, and the verified knowledge candidate lifecycle (evaluations, releases, rollbacks).
4. `ai-analysis-service` owns: The asynchronous multimodal evidence extraction and sanitization pipeline (OCR, document parsing, log analysis, media frame sampling).
5. `auth-service` owns: Fine-grained permission definitions, role templates, and recent-authentication step-up attestations.

No service directly reads or writes another service's database schema. Cross-service interactions use versioned Kafka events or authenticated REST APIs.

## Consequences
- **Positive:** Domain cohesion is maintained; no unnecessary infrastructure sprawl; clear transactional boundaries.
- **Negative:** Services like `ticket-service` manage additional domain aggregates (incidents, conversations, outcomes).
- **Reversal Trigger:** If incident management or conversation streaming requirements necessitate a separate operational scaling boundary or independent team ownership.
