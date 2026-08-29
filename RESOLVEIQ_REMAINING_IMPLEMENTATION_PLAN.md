# ResolveIQ Remaining Implementation Plan

> **Document status:** Implementation handoff and execution source of truth  
> **Prepared from:** Repository audit against `RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md`  
> **Audit date:** 2026-08-29  
> **Scope:** All work still required to turn the current architectural prototype into a secure, measurable, end-to-end portfolio application  
> **Repository:** `resolveiq`  

---

## 1. Purpose

This document tells an implementation agent exactly what remains, the order in which it must be built, how each part must behave, and how completion must be proven. It supplements the full product blueprint; it does not replace its product, UX, security, architecture, or production requirements.

The current repository is a broad prototype. It contains service modules, database migrations, event contracts, a React visual design, Compose infrastructure, ADRs, runbooks, and some passing unit tests. It does **not** yet provide a secure and genuine end-to-end product flow. Several visible features are static, simulated, or only represented by schemas.

The implementing agent must deliver this primary journey:

1. A customer securely signs in and creates a ticket.
2. The ticket is committed together with an outbox event.
3. Kafka delivers the event without loss or duplicate state changes.
4. AI analysis produces validated structured facts or a deterministic fallback.
5. Routing and SLA are calculated from stored policies.
6. RAG retrieves authorized, active knowledge and sanitized resolved cases using PostgreSQL full-text search and pgvector.
7. The system creates and persists a grounded draft with resolvable citations.
8. An authenticated agent reviews, edits, approves, and explicitly sends the response.
9. Feedback and operational metadata are stored.
10. Evaluation, telemetry, tests, and a demo prove that the behavior is real.

No resume, README, dashboard, or evaluation report may claim functionality or metrics until a test or reproducible command proves them.

---

## 2. Relationship to the main blueprint

Before changing code, the implementation agent must read these sections of `RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md` in full:

- roles and permission model;
- architecture principles and service ownership;
- workflows and state machines;
- event-driven design;
- data and API architecture;
- AI, retrieval, routing, SLA, and frontend specifications;
- security, reliability, observability, performance, and testing requirements;
- phased implementation plan and definition of done.

When this document and the blueprint appear to conflict:

1. security and privacy requirements win;
2. service/data ownership boundaries win;
3. the more testable and failure-safe interpretation wins;
4. record a new ADR if a material architecture decision changes;
5. do not silently reduce scope or weaken an acceptance gate.

---

## 3. Verified current state

### 3.1 What exists

- Java 21/Spring Boot multi-module backend.
- Modules for discovery, gateway, authentication, tickets, analysis, routing, RAG, orchestration, and common contracts.
- React/Vite/Tailwind frontend with customer, agent, knowledge, and administration visual shells.
- PostgreSQL migrations, including pgvector-oriented columns.
- Kafka/Redpanda, PostgreSQL, MinIO, OpenTelemetry Collector, Prometheus, and Grafana Compose definitions.
- Shared versioned event-envelope contracts.
- Ticket and workflow outbox entities.
- Ticket state model, optimistic version fields, workflow records, knowledge entities, and evaluation folders.
- Architecture decisions, threat model, runbooks, CI definitions, and repository documentation.
- Clean Git working tree on `main`, tracking `origin/main` at audit time.

### 3.2 Verification performed

| Check | Audit result |
|---|---|
| `./mvnw clean verify` | Passed; test coverage is small and primarily mock/unit level |
| Frontend production build | Passed |
| Frontend unit test command | Failed because no test files exist |
| Frontend lint command | Failed because ESLint is not installed/configured |
| Compose configuration parse | Passed |
| Full Compose runtime | Not verified because Docker daemon was unavailable |
| Evaluation runner | Runs but produces hardcoded, non-evidential metrics |
| Git state | Clean and synchronized with configured upstream at audit time |

### 3.3 Current gaps that must not be mistaken for completed functionality

- Gateway routes do not provide complete JWT enforcement, trusted identity propagation, RBAC, or tenant isolation.
- Public registration accepts requested roles and can enable privilege escalation.
- Several services accept optional identity/tenant headers and provide unsafe defaults.
- Event retries and orchestration event publishing are incomplete.
- Orchestration performs remote calls inside a database transaction and silently falls back to mock results.
- AI suggestion IDs can be emitted without a corresponding persisted suggestion.
- Retrieval loads all tenant chunks and scores them in JVM memory; it does not execute production pgvector/full-text retrieval.
- Embeddings and chat behavior are simulated.
- Citation records and important model/governance metadata are not fully persisted.
- Frontend pages primarily display hardcoded content and are not connected to real APIs.
- Evaluation always assigns rank one and reports unmeasured safety values.
- Application containerization, integration tests, E2E tests, load/failure testing, public deployment, and a verified demo remain incomplete.

---

## 4. Non-negotiable implementation rules

The implementation agent must follow all of these rules:

1. Work through the work packages in dependency order. Do not start a later gate while a required earlier gate is red.
2. Inspect the existing implementation before creating a replacement. Reuse correct entities, migrations, contracts, styles, and ports.
3. Preserve unrelated changes and do not rewrite broad modules without a bounded reason.
4. Never commit secrets, real support data, generated build output, database volumes, tokens, or local model caches.
5. Never commit or push unless the repository owner explicitly asks.
6. Public clients must never choose their tenant, user, or role through trusted headers or request bodies.
7. Services must derive identity from a validated credential or a trusted, authenticated internal context.
8. Every repository query for tenant-owned data must be tenant-scoped. A UUID alone is not authorization.
9. Never place model calls, HTTP calls, Kafka waits, or object-store calls inside a database transaction.
10. Use short local transactions and durable workflow state between external steps.
11. Assume Kafka delivery is at least once. All consumers must be idempotent.
12. AI output is untrusted input. Validate enums, ranges, lengths, citations, PII, and policy compliance.
13. No model or tool may send a customer-visible response or mutate business state without a deterministic application check and explicit human approval.
14. No test may be weakened, skipped, or replaced by a hardcoded success result to pass a gate.
15. Do not add Kubernetes, extra databases, a separate vector database, or autonomous agent frameworks before Phase 9 is complete.
16. Keep the modular service structure, but prefer correctness and a working vertical slice over adding new services.
17. Each completed issue must include tests, observable behavior, documentation, and rollback/disable behavior.

---

## 5. Required execution strategy

### 5.1 Branch and change discipline

For each issue:

1. Read its affected code, migrations, tests, and contracts.
2. Record the intended behavior and failure cases.
3. Add or update a failing test where practical.
4. Implement the smallest complete behavior.
5. Run module tests, then root verification.
6. Update API/event documentation and examples.
7. Report changed files, migrations, commands, results, limitations, and rollback.
8. Wait for owner instruction before committing or pushing.

Suggested branch naming when the owner authorizes branches:

- `fix/security-registration-rbac`
- `feat/reliable-workflow-outbox`
- `feat/pgvector-hybrid-retrieval`
- `feat/frontend-api-integration`
- `test/end-to-end-quality-gates`

### 5.2 Work package order

| Order | Package | Depends on |
|---:|---|---|
| 0 | Baseline truth and quality gates | None |
| 1 | Authentication, authorization, and tenant isolation | 0 |
| 2 | Ticket domain, idempotency, attachments, and SLA foundation | 1 |
| 3 | Reliable events and orchestration | 1, 2 |
| 4 | Knowledge lifecycle and production hybrid retrieval | 1, 3 |
| 5 | Safe AI triage and response copilot | 3, 4 |
| 6 | Real frontend integration and complete UX | 1–5 |
| 7 | Feedback, evaluation, and controlled tools | 4–6 |
| 8 | Observability, resilience, and production hardening | 1–7 |
| 9 | Containerization, deployment, and portfolio demo | 0–8 |

Packages may overlap only when they do not share migrations/contracts and when all dependency gates remain satisfied.

---

## 6. WP-0 — Establish an honest, reproducible baseline

### Objective

Make local and CI results trustworthy before adding features.

### Tasks

- [ ] Add and configure ESLint for TypeScript/React; make `npm run lint` pass with zero warnings.
- [ ] Add at least one real frontend test so `npm run test` does not pass through an empty suite.
- [ ] Choose one JavaScript package manager/lockfile and document it. Remove the redundant lockfile only after confirming the selected workflow.
- [ ] Make CI run backend tests, frontend lint, frontend tests, and frontend build.
- [ ] Add JaCoCo and frontend coverage reporting. Set initially realistic thresholds; increase them for security/domain packages.
- [ ] Add Testcontainers dependencies to the modules that genuinely run PostgreSQL/Kafka integration tests; a BOM alone is insufficient.
- [ ] Create a root verification script or documented command sequence that fails on any red check.
- [ ] Mark unimplemented UI/dashboard features as demo placeholders until connected, or hide them behind a clearly named demo fixture mode.
- [ ] Correct documentation that claims RBAC, hybrid retrieval, safety metrics, or production readiness before those gates pass.
- [ ] Ensure `target`, `dist`, `node_modules`, reports with timestamps, IDE files, and runtime data remain ignored.

### Required CI sequence

```bash
./mvnw clean verify
cd frontend
npm ci
npm run lint
npm run test
npm run build
cd ..
docker compose config --quiet
```

Add separate CI jobs later for integration, E2E, security scanning, container scanning, and evaluation. Do not overload a single opaque job.

### Tests

- Root build from a clean dependency cache where CI permits.
- Frontend component smoke test.
- Migration startup test using a real PostgreSQL container with pgvector enabled.
- CI check that no test command succeeds because no tests were found.

### Gate WP-0

- [ ] All baseline commands return zero.
- [ ] CI reflects the same commands developers run locally.
- [ ] Documentation clearly distinguishes implemented, demo, and planned behavior.
- [ ] No generated or secret material is tracked.

---

## 7. WP-1 — Authentication, RBAC, and tenant isolation

### Objective

Eliminate privilege escalation, IDOR, spoofed identity headers, and cross-tenant access before implementing more product behavior.

### 7.1 Public registration and account lifecycle

- [ ] Remove `roles` and any trusted `tenantId` choice from public registration input.
- [ ] Public self-registration creates only a `CUSTOMER` in the configured tenant/onboarding flow.
- [ ] If multi-tenant onboarding is demonstrated, create a separate verified tenant-onboarding command with explicit rules; do not overload user registration.
- [ ] Add an authenticated admin-only invitation/user-creation endpoint for assigning staff roles.
- [ ] Validate role assignments against the permission matrix and tenant ownership.
- [ ] Add email normalization, password policy, breached/common-password protection where practical, and enumeration-safe errors.
- [ ] Implement password reset using hashed, single-use, expiring tokens.
- [ ] Implement refresh-token rotation with reuse detection and token-family revocation.
- [ ] Implement logout-current and logout-all.
- [ ] Store refresh tokens in `HttpOnly`, `Secure` in production, appropriately scoped `SameSite` cookies. Do not store refresh tokens in browser local storage.
- [ ] Add login throttling and temporary lockout with audit events.
- [ ] Audit registration, login success/failure, token refresh/reuse, password reset, role changes, lockouts, and logout-all without logging secrets.

### 7.2 Gateway security

- [ ] Configure the gateway as an OAuth2 resource server using the auth service's signed JWT contract.
- [ ] Validate signature, issuer, audience, expiry, not-before, token type, and required claims.
- [ ] Strip all inbound identity headers such as `X-Tenant-Id`, `X-User-Id`, `X-Roles`, and internal-service markers.
- [ ] Derive identity exclusively from validated JWT claims.
- [ ] Forward a trusted identity context only to private backend routes. Prefer forwarding the original JWT and validating it again in services; if internal signed headers are used, document signing, expiry, replay protection, and key rotation in an ADR.
- [ ] Add correlation IDs and preserve a valid incoming ID only after format/length validation.
- [ ] Define public, authenticated, staff-only, and admin-only route groups.
- [ ] Add per-IP limits for unauthenticated endpoints and per-user/tenant limits for authenticated expensive endpoints.
- [ ] Restrict CORS to configured origins and methods; do not use wildcard credentials.
- [ ] Keep service ports private in staging; only gateway/frontend entry points may be public.

### 7.3 Service-level authorization

- [ ] Add Spring Security resource-server enforcement to every public-facing owning service, not only auth-service.
- [ ] Introduce a shared immutable request principal containing `userId`, `tenantId`, roles, correlation ID, and authentication type.
- [ ] Do not generate fallback tenants/users when headers or claims are absent. Return `401` for missing identity and `403` for insufficient permission.
- [ ] Add method-level authorization and domain-level ownership checks.
- [ ] Ensure tenant-owned repositories expose methods such as `findByIdAndTenantId`, never controller flows that call only `findById`.
- [ ] For customer endpoints, scope reads and writes by both tenant and authenticated customer ownership unless the permission matrix explicitly allows wider access.
- [ ] Agents may access tickets only through team/assignment permissions defined by the blueprint.
- [ ] Knowledge managers may author/publish only within their tenant.
- [ ] Auditors receive read-only access and cannot mutate configuration.
- [ ] Internal event consumers must validate envelope tenant and aggregate consistency before mutation.

### 7.4 Minimum authorization matrix to automate

| Operation | Customer | Agent | Team lead | Knowledge manager | Admin | Auditor |
|---|---:|---:|---:|---:|---:|---:|
| Create own ticket | Allow | Configurable | Configurable | No | Allow | No |
| View own ticket | Allow | N/A | N/A | No | Allow | Read only |
| View tenant queue | No | Scoped team | Scoped teams | No | Allow | Read only |
| Change ticket status | Own reply only | Assigned/scoped | Scoped teams | No | Allow | No |
| Review/send AI draft | No | Assigned/scoped | Scoped teams | No | Allow | No |
| Create knowledge draft | No | Suggest only | Suggest only | Allow | Allow | No |
| Publish knowledge | No | No | No | Allow | Allow | No |
| Configure routing/SLA | No | No | Scoped | No | Allow | No |
| View governance/audit | Own data only | Limited | Team | Limited | Allow | Allow read only |
| Assign roles | No | No | No | No | Allow | No |

Exact permissions must follow the main blueprint where it is more restrictive.

### 7.5 Security tests

Create parameterized integration tests covering:

- missing, expired, malformed, wrong-issuer, wrong-audience, and wrong-signature JWTs;
- role escalation during public registration;
- customer A reading customer B's ticket in the same tenant;
- user in tenant A reading a UUID from tenant B;
- spoofed inbound tenant/user/role headers through the gateway;
- agent outside the assigned team;
- auditor attempting every mutation endpoint;
- refresh token replay after rotation;
- logout-all invalidating all active token families;
- CORS and rate-limit behavior;
- error bodies that do not reveal whether unrelated resources exist.

Prefer `404` for inaccessible resource identifiers where revealing existence would leak information; apply consistently.

### Gate WP-1

- [ ] Public registration cannot assign privileged roles or arbitrary tenant identity.
- [ ] Every protected API rejects missing/invalid credentials.
- [ ] Gateway spoofed-header tests pass.
- [ ] Cross-customer and cross-tenant IDOR suites pass.
- [ ] Refresh rotation/reuse/logout behavior passes integration tests.
- [ ] Threat model and API documentation match actual enforcement.

---

## 8. WP-2 — Ticket domain, idempotency, attachments, and SLA foundation

### Objective

Make the non-AI support workflow correct and useful even when every AI provider is unavailable.

### 8.1 Ticket identity and lifecycle

- [ ] Replace process-local/hardcoded ticket-number generation with a database-backed, concurrency-safe strategy.
- [ ] Use a unique constraint and a clear format such as `RIQ-YYYY-000001`; define timezone and yearly reset behavior.
- [ ] Complete the ticket state machine, including allowed actor, source state, target state, required fields, and emitted event.
- [ ] Record immutable status history with actor, timestamp, reason, and correlation ID.
- [ ] Keep optimistic locking and return a standardized conflict response for stale updates.
- [ ] Implement public-facing lookup by ticket number only where ownership checks remain enforced.
- [ ] Add paginated/sortable queue APIs; never return unbounded tenant lists.
- [ ] Add messages/replies as owned domain records rather than overwriting ticket description.

### 8.2 Command idempotency

- [ ] Require `Idempotency-Key` for ticket creation, message creation, approval/send, and other retry-prone commands.
- [ ] Scope keys by tenant, authenticated actor, route, and operation.
- [ ] Persist request hash, response status/body reference, state, creation time, and expiry.
- [ ] Same key plus same request returns the original result.
- [ ] Same key plus different request returns `409 Conflict`.
- [ ] Concurrent identical requests create one business result.
- [ ] Add cleanup/retention behavior without removing active keys.

### 8.3 Attachments

- [ ] Add attachment metadata owned by ticket-service and binary storage through a MinIO/object-store port.
- [ ] Use server-generated object keys; never trust filenames as paths.
- [ ] Enforce allow-listed content types, size/count limits, sanitized display names, and tenant/ticket ownership.
- [ ] Support quarantine/scanning state; do not expose unscanned uploads as trusted content.
- [ ] Use short-lived signed upload/download URLs where suitable.
- [ ] Do not feed raw attachments into an LLM until extraction and safety policy explicitly allow it.
- [ ] Add cleanup for abandoned uploads and audit downloads of sensitive attachments.

### 8.4 Teams, routing facts, and SLA

- [ ] Complete team, membership, skill, routing-rule, and SLA-policy CRUD with RBAC.
- [ ] Store routing-rule priority and deterministic tie-breaking.
- [ ] Compute first-response and resolution deadlines from policy, priority, channel, tenant timezone, and business calendar.
- [ ] Persist the selected policy/rule IDs and a human-readable reason code.
- [ ] Implement SLA states: safe, at risk, breached, paused, and completed where applicable.
- [ ] Recalculate deadlines only under documented triggers; retain audit history.
- [ ] Provide a deterministic fallback queue and default SLA when no rule matches.

### 8.5 Standard API errors

- [ ] Use RFC 9457-style problem details consistently.
- [ ] Include stable error code, status, safe detail, instance, correlation ID, and validation fields.
- [ ] Never expose stack traces, SQL, JWT contents, provider payloads, or cross-tenant resource existence.

### Tests

- Concurrent ticket-number generation.
- Every allowed and forbidden ticket transition.
- Optimistic-lock conflicts.
- Idempotency same/different/concurrent request cases.
- Attachment traversal, oversized file, MIME mismatch, ownership, expiry, and quarantine tests.
- SLA calendar boundary, timezone, pause/resume, breach, and fallback cases.
- Manual ticket handling while Kafka, RAG, and AI are unavailable.

### Gate WP-2

- [ ] A customer and agent can complete a manual support journey without AI.
- [ ] Ticket numbering is restart- and concurrency-safe.
- [ ] Duplicate commands do not duplicate tickets, messages, or sends.
- [ ] Every transition and SLA calculation has deterministic tests.
- [ ] Attachments cannot cross tenant/ticket boundaries.

---

## 9. WP-3 — Reliable events and durable orchestration

### Objective

Provide explainable at-least-once event processing without data loss or duplicate business effects.

### 9.1 Event contract

- [ ] Retain one versioned event envelope with event ID, event type/version, occurred time, producer, tenant ID, aggregate type/ID, correlation ID, causation ID, actor where appropriate, and payload.
- [ ] Validate required envelope fields at producer and consumer boundaries.
- [ ] Key ticket topics by ticket ID to preserve per-ticket ordering.
- [ ] Document topic names, ownership, schema version, producer, consumer, retention, retry, and DLQ.
- [ ] Keep backward compatibility for active versions or implement an explicit migration strategy.

### 9.2 Transactional outbox

Apply to every service that publishes a business event:

- [ ] Write the aggregate change and outbox record in the same local transaction.
- [ ] Use statuses `PENDING`, `RETRY`, `PUBLISHED`, and `DEAD` with `attempt_count`, `next_attempt_at`, `last_error_code`, and timestamps.
- [ ] Select both due `PENDING` and due `RETRY` rows.
- [ ] Claim batches safely for multiple publisher instances using PostgreSQL locking such as `FOR UPDATE SKIP LOCKED` or a documented equivalent.
- [ ] Never block a database transaction waiting indefinitely for Kafka acknowledgement.
- [ ] Use bounded publish timeout, exponential backoff with jitter, maximum attempts, and dead transition.
- [ ] Retain published records according to policy and metrics needs.
- [ ] Add the missing publisher in orchestration-service.
- [ ] Expose operational counts/age for pending, retry, and dead records.

### 9.3 Consumer idempotency

- [ ] Add a processed-event table per consuming service or a correctly owned equivalent.
- [ ] Insert event ID and perform business mutation in the same transaction.
- [ ] A unique event-ID constraint must turn redelivery into a no-op, not an error loop.
- [ ] Validate tenant ID, aggregate ID, event type, and supported version before processing.
- [ ] Do not swallow malformed messages. Route non-retryable failures to DLQ with safe error metadata.

### 9.4 Retry, DLQ, and replay

- [ ] Classify transient failures such as timeouts/503 as retryable.
- [ ] Classify validation, unsupported schema, authorization, and invariant violations as non-retryable.
- [ ] Implement bounded retry topics or a documented consumer retry mechanism.
- [ ] Persist DLQ metadata and provide admin/auditor visibility.
- [ ] Implement an authenticated admin-only replay endpoint that records actor, reason, original event ID, new replay ID, and outcome.
- [ ] Replay must still pass idempotency and authorization/invariant validation.

### 9.5 Orchestration transaction boundaries

Refactor triage into resumable steps:

1. Consume `TicketCreated.v1` idempotently.
2. In a short transaction, create or load the workflow and next step.
3. Commit.
4. Call the analysis provider outside the transaction.
5. In a short transaction, store attempt/output/status and select the next step.
6. Repeat for routing, retrieval, validation, and draft generation.
7. Persist the final suggestion and its evidence before emitting completion.
8. Publish completion through the orchestration outbox.

Additional requirements:

- [ ] Reuse an existing active workflow for duplicate ticket events.
- [ ] Persist every step attempt, start/end time, timeout, error class, and safe error code.
- [ ] Resume safely after process restart.
- [ ] Apply per-step timeout, retry policy, circuit breaker, and provider kill switch.
- [ ] Do not convert every dependency failure into a fake successful result.
- [ ] Distinguish `COMPLETED`, `COMPLETED_WITH_FALLBACK`, `WAITING_RETRY`, `MANUAL_ACTION_REQUIRED`, and `FAILED`.
- [ ] Correlation and trace context must propagate across Kafka and HTTP.

### 9.6 Suggestion ownership and persistence

- [ ] Decide and document the owning service for AI suggestions; prefer the ticket domain if suggestions are part of ticket review state.
- [ ] Persist suggestion ID, ticket/tenant, version, draft text, lifecycle state, citations, retrieval run, model/prompt versions, confidence, validation results, timestamps, and invalidation reason.
- [ ] Never emit a suggestion ID that does not exist.
- [ ] Ticket changes that make a suggestion stale must invalidate it deterministically.
- [ ] Completion event references the persisted suggestion and contains only the minimum necessary summary.

### Tests

- Kafka unavailable after ticket commit: outbox remains due and eventually publishes.
- Publisher crashes after Kafka accepts but before marking published: duplicate delivery causes no duplicate mutation.
- Two publisher instances cannot publish the same claimed batch concurrently without safe duplicate behavior.
- `RETRY` rows are selected after `next_attempt_at`.
- Poison event reaches DLQ and can be audited/replayed.
- Service restart after each workflow step resumes correctly.
- Unsupported event version is isolated, not silently acknowledged.
- Orchestration test uses configured URLs/mocked servers intentionally; no `/null/...` requests are accepted as a passing integration test.

### Gate WP-3

- [ ] Broker outage loses no committed ticket event.
- [ ] Duplicate delivery produces one workflow and one business state change.
- [ ] Orchestration completion events actually reach consumers.
- [ ] Suggestions referenced by events exist and can be retrieved.
- [ ] A DLQ item can be safely replayed with a complete audit record.

---

## 10. WP-4 — Governed knowledge and production hybrid retrieval

### Objective

Replace simulated in-memory scoring with real, authorized, versioned PostgreSQL full-text and pgvector retrieval.

### 10.1 Knowledge lifecycle

- [ ] Implement states such as `DRAFT`, `IN_REVIEW`, `PUBLISHED`, `ARCHIVED`, and `FAILED_INDEXING` according to the blueprint.
- [ ] Publishing creates/activates an immutable version; editing a published article creates a new draft version.
- [ ] Only authorized knowledge managers/admins can publish or archive.
- [ ] Store source, product, locale/language, ACL tags, author, reviewer, effective dates, and content hash.
- [ ] Reject or deduplicate unchanged content by hash.
- [ ] Never expose inactive, failed, expired, unauthorized, or different-tenant versions in retrieval.

### 10.2 Resolved-case privacy lifecycle

- [ ] Only eligible resolved tickets may be proposed as reusable cases.
- [ ] Run deterministic PII/secret sanitization before approval.
- [ ] Store sanitized subject/body separately; never index the raw customer conversation.
- [ ] Require an authorized human approval with sanitization diff and reason.
- [ ] Preserve source ticket linkage internally with strict authorization and audit.
- [ ] Support withdrawal and reindex removal.

### 10.3 Embedding provider abstraction

- [ ] Keep an `EmbeddingPort` with provider adapters selected by configuration.
- [ ] Add at least one real provider suitable for the demo and one deterministic test adapter.
- [ ] Optional local/Ollama support is acceptable but must not be the only documented path unless it is reproducible.
- [ ] Validate returned dimension and reject mismatches.
- [ ] Record provider, model, dimension, normalization, embedding schema version, content hash, and generation time.
- [ ] Never regenerate every stored chunk embedding during a query.
- [ ] Batch embedding calls, apply timeouts/retries/budget caps, and redact logged payloads.
- [ ] Production profile must fail closed if it is configured for a mock adapter.

### 10.4 Ingestion/indexing pipeline

For each knowledge or approved resolved-case version:

1. validate and normalize text;
2. calculate content hash;
3. split with a deterministic, versioned chunking policy;
4. preserve source offsets/section headings;
5. create embeddings asynchronously in batches;
6. write chunks and embedding metadata to a staging index version;
7. validate expected chunk count and dimensions;
8. atomically activate the new index/version;
9. retain the prior active version for rollback;
10. emit indexed/failed events and metrics.

- [ ] Add retryable indexing jobs and an admin retry action.
- [ ] Prevent partial indexing from becoming active.
- [ ] Support reindex by embedding/chunking version.
- [ ] Rollback switches the active version without reconstructing old embeddings.

### 10.5 PostgreSQL retrieval

Implement database queries rather than loading all chunks:

- [ ] Lexical candidate query using stored/generated `tsvector`, language configuration, `websearch_to_tsquery` or a justified alternative, and ranked results.
- [ ] Vector candidate query using pgvector cosine or inner-product distance and an appropriate HNSW/IVFFlat index.
- [ ] Filter both candidate sets by tenant, active version, ACL, product, locale/language, source type, and effective date before ranking.
- [ ] Fetch bounded candidate pools such as top 30–100 from each strategy, then fuse them.
- [ ] Implement Reciprocal Rank Fusion with configured `k` and deterministic tie-breaking.
- [ ] Optional reranking must be behind a port and feature flag; base retrieval must work without it.
- [ ] Return top-K with score components and diagnostic metadata visible only to authorized staff.
- [ ] Avoid exposing raw embeddings or sensitive sanitized-source internals.

### 10.6 Citations

- [ ] Persist citation records for every generated suggestion.
- [ ] Citation includes source type, source ID, immutable version ID, chunk ID, excerpt/offset, retrieval rank/score, and authorization context.
- [ ] Citation resolution must recheck tenant and user authorization.
- [ ] Customer-visible citations expose only approved public-safe fields.
- [ ] If a source is archived later, retain an auditable immutable reference but do not use it for new suggestions.

### 10.7 Retrieval API behavior

Required endpoints or equivalent:

- `POST /api/v1/retrieval/search` for authenticated internal/staff search.
- `POST /api/v1/knowledge/documents` create draft.
- `POST /api/v1/knowledge/documents/{id}/versions` create version.
- `POST /api/v1/knowledge/documents/{id}/versions/{versionId}/publish` publish/index.
- `GET /api/v1/knowledge/index-jobs/{id}` inspect job.
- `POST /api/v1/knowledge/index-jobs/{id}/retry` authorized retry.
- `POST /api/v1/resolved-cases/{ticketId}/propose` sanitize proposal.
- `POST /api/v1/resolved-cases/{id}/approve` human approval.

Use blueprint naming if it already defines a stronger contract. Generate and validate OpenAPI.

### Tests

- Testcontainers PostgreSQL with the vector extension and real Flyway migrations.
- SQL vector nearest-neighbor and full-text ranking tests.
- Hybrid fusion deterministic ordering.
- Cross-tenant, ACL, inactive-version, expired-source, product, and language filters.
- Embedding dimension mismatch and provider outage.
- Atomic activation and rollback after partial indexing failure.
- Sanitization suite containing fictional emails, phones, account numbers, tokens, addresses, and prompt injection.
- Citation resolution and archived-source behavior.
- Performance test proving query work is bounded and does not embed every chunk.

### Gate WP-4

- [ ] Runtime retrieval executes PostgreSQL lexical and pgvector queries.
- [ ] No query-time re-embedding of stored chunks occurs.
- [ ] Unauthorized/inactive content is never returned.
- [ ] Index activation is atomic and rollback is demonstrated.
- [ ] Recall@5 and MRR are measured from a real frozen dataset, not hardcoded.

---

## 11. WP-5 — Safe AI triage and grounded response copilot

### Objective

Deliver useful AI assistance with validated output, evidence, abstention, complete provenance, and mandatory human control.

### 11.1 Provider ports and profiles

- [ ] Maintain separate ports for classification/chat, embeddings, optional reranking, and tools.
- [ ] Implement at least one real chat provider and deterministic test adapters.
- [ ] Configuration selects provider/model; business services must not import vendor-specific types.
- [ ] Apply connect/read/total timeouts, bounded retry for retryable errors, circuit breakers, budget limits, and kill switches.
- [ ] Production profile refuses mock providers and missing mandatory secrets.
- [ ] Never log API keys, complete prompts containing sensitive data, or raw provider responses.

### 11.2 Structured analysis

- [ ] Define a versioned JSON schema for intent, category, sentiment, urgency, language, confidence, safety flags, and explanation/reason codes.
- [ ] Constrain allowed enums, numeric ranges, string lengths, and required fields.
- [ ] Parse strictly. Invalid output is not marked valid.
- [ ] Retry repair at most a bounded number of times; then use a deterministic fallback.
- [ ] Persist raw output only if privacy policy permits, encrypted/protected and access-controlled; otherwise persist a safe normalized form and hash.
- [ ] Fallback must mark provenance and confidence honestly.

### 11.3 Prompt and PII safety

- [ ] Separate system instructions, trusted context, retrieved evidence, and untrusted customer text using clear delimiters/data structures.
- [ ] Treat instructions inside tickets and retrieved content as data.
- [ ] Detect/redact configured PII and secrets before external provider calls where policy requires.
- [ ] Run output validation for PII leakage, secret leakage, disallowed policy claims, unsafe links, unsupported actions, length, and citation coverage.
- [ ] Store validator names, versions, results, and reason codes.
- [ ] Add prompt-injection and data-exfiltration cases to automated safety evaluation.

### 11.4 Grounded draft generation

- [ ] Build the prompt exclusively from the ticket, validated analysis, deterministic policy facts, and authorized retrieved evidence.
- [ ] Require citation markers mapped to persisted citation records.
- [ ] Verify cited claims are supported by the referenced excerpts using deterministic checks and, optionally, a separately measured judge.
- [ ] Compute confidence from explicit components; do not expose a fabricated model confidence.
- [ ] If evidence is absent/weak/conflicting or validation fails, create an abstaining draft that asks the agent to respond manually or request information.
- [ ] Never invent refunds, credits, account actions, deadlines, or policy.
- [ ] Persist prompt template version, model/provider, parameters, token counts, latency, estimated cost, retrieval run, citations, validation outcomes, and fallback state.

### 11.5 Human approval and suggestion lifecycle

States must include an equivalent of:

`GENERATED → IN_REVIEW → APPROVED/EDITED/REJECTED → SENT`, with `INVALIDATED` and `FAILED` paths.

- [ ] Only an authorized assigned/scoped agent can review and send.
- [ ] Approval and send are distinct, idempotent actions or a clearly documented atomic command.
- [ ] Store original draft, final edited text, edit distance, actor, reason, and timestamps.
- [ ] Rejection requires a categorized reason and optional safe comment.
- [ ] Ticket/knowledge changes invalidate stale suggestions.
- [ ] Regeneration requires a reason and creates a new version; it never overwrites history.
- [ ] No scheduled job, event consumer, model, or tool may call the customer-send operation autonomously.
- [ ] Sending must use the non-AI ticket messaging domain and create an audit/event record.

### Tests

- Valid/invalid/partial/malformed provider JSON.
- Provider timeout, rate limit, outage, and kill-switch behavior.
- Prompt injection in ticket and knowledge content.
- No evidence, conflicting evidence, unauthorized evidence, and archived citation.
- Fictional PII leakage and unsafe policy claims.
- Suggestion invalidation, regeneration, concurrent review, duplicate send, and unauthorized send.
- Static/code-level invariant test showing there is no model-to-send dependency path.
- Full vertical integration: ticket event to persisted reviewable suggestion.

### Gate WP-5

- [ ] A real ticket creates a persisted, retrievable, cited suggestion.
- [ ] Every suggestion has real provenance and validation records.
- [ ] Weak evidence produces abstention rather than an invented answer.
- [ ] Provider outage leaves the manual ticket workflow usable.
- [ ] Only an authenticated human action can send a response.

---

## 12. WP-6 — Connect and complete the frontend

### Objective

Turn the existing visual prototype into a responsive, accessible client for real backend behavior.

### 12.1 Frontend architecture

- [ ] Use React Router for real routes and protected route groups.
- [ ] Add a typed API client generated from or checked against OpenAPI.
- [ ] Configure React Query once at the application root for server state, caching, invalidation, retries, and cancellation.
- [ ] Use React Hook Form plus schema validation for forms.
- [ ] Keep auth access token handling in memory where practical and use secure refresh-cookie flow.
- [ ] Remove the role-switch dropdown from normal application behavior. If retained for screenshots, isolate it in an explicit development-only fixture mode.
- [ ] Add a centralized problem-details error mapper and correlation-ID display for supportable errors.
- [ ] Do not duplicate backend authorization logic as security; UI permission checks are presentation only.

### 12.2 Required routes

Implement route names consistent with the final information architecture:

- `/login`, `/register`, `/forgot-password`, `/reset-password`;
- `/customer/tickets`, `/customer/tickets/new`, `/customer/tickets/:id`;
- `/agent/queue`, `/agent/tickets/:id`;
- `/lead/routing`, `/lead/sla`;
- `/knowledge/articles`, `/knowledge/articles/:id`, `/knowledge/index-jobs`;
- `/admin/overview`, `/admin/users`, `/admin/governance`, `/admin/operations`, `/admin/evaluations`, `/admin/audit`.

Unauthorized routes must redirect or render a proper `403`; unauthenticated routes must preserve the safe intended destination.

### 12.3 Customer UX

- [ ] Register/login/reset/logout session experience.
- [ ] Ticket list with status, updated time, SLA-friendly text, pagination, loading, empty, and error states.
- [ ] Ticket creation with validation, idempotency, attachments, success receipt, and retry-safe behavior.
- [ ] Ticket conversation/detail and customer reply.
- [ ] Clear AI disclosure only where AI content is shown.
- [ ] Resolution confirmation and optional satisfaction survey.

### 12.4 Agent and lead UX

- [ ] Queue from real paginated APIs with filters, search, sort, assignment, SLA risk, and stale-data refresh.
- [ ] Ticket workspace with conversation, customer-safe profile, status history, assignment, SLA, and optimistic conflict handling.
- [ ] Intelligence panel showing analysis provenance, citations, similar approved cases, confidence components, validation warnings, and fallback status.
- [ ] Citation click opens the authorized source/version excerpt.
- [ ] Draft editor visually distinguishes original AI draft and agent-edited final response.
- [ ] Approve/edit/reject/regenerate/send actions use real APIs, confirmations, idempotency, disabled/pending states, and success/error feedback.
- [ ] Team lead routing and SLA views expose rule reasons and safe configuration controls.

### 12.5 Knowledge/admin/operations UX

- [ ] Knowledge document list, editor, version comparison, review, publish, archive, indexing progress, failure, retry, and rollback.
- [ ] Resolved-case sanitization diff and approval.
- [ ] Governance view from measured invocation/validation/cost data, never hardcoded metrics.
- [ ] Evaluation run/compare pages backed by stored real reports.
- [ ] Operations view for workflow state, outbox age, retry/dead counts, Kafka/DLQ health, and authorized replay.
- [ ] Audit view with filters and immutable event detail.

### 12.6 UI states and accessibility

Every data-backed view must implement:

- initial loading/skeleton;
- empty state with useful next action;
- validation error;
- `401`, `403`, `404`, `409`, `429`, and `5xx` behavior;
- degraded AI/provider state while manual support remains available;
- retry and refresh behavior;
- responsive desktop/tablet/mobile layout;
- keyboard navigation, visible focus, correct labels/headings, dialog focus trap/return, table semantics, live announcements, and adequate contrast.

### Tests

- Vitest/Testing Library for components, forms, permissions, error mapping, and query states.
- Mock Service Worker or equivalent only for component tests, with contracts kept aligned to OpenAPI.
- Playwright against real composed services for mandatory journeys.
- Axe automated checks on every main route plus manual keyboard smoke test.
- Desktop and mobile viewport tests.
- No test may rely on the development role selector for authentication.

### Mandatory E2E journeys

1. Customer registers/logs in and creates a ticket exactly once despite a retried submission.
2. Agent sees the ticket, opens it, observes workflow progress, reviews real citations, edits the draft, approves, and sends.
3. Customer sees the response and replies/resolves.
4. Knowledge manager creates, reviews, publishes, indexes, and retrieves a new article.
5. Admin observes a failed workflow/outbox/DLQ item and safely retries/replays it.
6. Tenant A and unauthorized roles cannot access tenant B or restricted pages/APIs.
7. AI provider outage still permits manual agent response.

### Gate WP-6

- [ ] No production-facing page depends on hardcoded ticket, model, citation, or metric data.
- [ ] Mandatory E2E journeys pass on desktop and mobile.
- [ ] Frontend lint, unit tests, build, Playwright, and axe gates pass.
- [ ] No critical accessibility violation remains.

---

## 13. WP-7 — Genuine evaluation, feedback, and controlled tools

### Objective

Measure the system honestly and create a defensible improvement loop.

### 13.1 Frozen evaluation datasets

- [ ] Replace the five-case placeholder with at least 100 synthetic, reviewed retrieval cases before making quality claims.
- [ ] Cover intents, products, difficulty, paraphrases, typos, zero-result cases, competing articles, outdated versions, tenant/ACL filters, languages used in the demo, and resolved cases.
- [ ] Store query, relevant source/version IDs, relevance grades where used, expected abstention, and dataset version.
- [ ] Maintain separate safety datasets for PII, prompt injection, unsupported claims, and tool-policy violations.
- [ ] Never put real customer data in datasets.

### 13.2 Real evaluation runner

- [ ] Invoke the same retrieval/application API or core adapter used by production.
- [ ] Remove hardcoded ranks, safety rates, status text, and methodology claims.
- [ ] Calculate Recall@K, MRR, nDCG where graded relevance exists, zero-result rate, latency percentiles, and filter leakage.
- [ ] Evaluate draft citation coverage, groundedness, abstention precision/recall, PII leakage, unsafe action rate, and schema validity.
- [ ] Separate deterministic metrics from model-judge metrics; version judge prompt/model and report limitations.
- [ ] Fail the run when required samples cannot execute; do not silently treat errors as passes.
- [ ] Store configuration, code commit, dataset version, embedding model/version, retrieval parameters, prompt/model versions, run time, raw per-case results, and aggregate report.
- [ ] Make results reproducible within documented non-determinism bounds.

### 13.3 Quality gates

Initial gates must come from the main blueprint and a measured baseline. At minimum:

- Recall@5 target;
- MRR target;
- zero cross-tenant/ACL leakage;
- zero autonomous send paths;
- PII leakage below the declared threshold;
- schema validity above the declared threshold;
- latency/cost bounds suitable for the demo.

Never tune on the frozen test set. Maintain development and final test splits.

### 13.4 Feedback loop

- [ ] Persist accept/edit/reject/regenerate labels with reason categories.
- [ ] Calculate normalized edit distance and time-to-review.
- [ ] Link feedback to immutable suggestion, prompt, model, retrieval run, citations, and ticket category.
- [ ] Show acceptance/edit/abstention/zero-result/groundedness/cost trends with minimum sample-size warnings.
- [ ] Do not automatically train or activate prompts from feedback.
- [ ] Prompt/retrieval activation requires an evaluation comparison, human approval, audit record, feature flag, and rollback.

### 13.5 Controlled read-only tools

Only after core RAG and approval flows pass:

- [ ] Add synthetic read-only tools such as order lookup or service-status lookup behind typed application ports.
- [ ] Validate typed input/output schemas, tenant/actor permission, allow-list, timeout, result size, and audit.
- [ ] Provide only synthetic data.
- [ ] Treat tool results as untrusted evidence and cite/provenance-tag them.
- [ ] No state-changing tool is in Phase 9 scope.
- [ ] MCP exposure is optional and must wait until the internal tool contract and demo client are proven.

### Gate WP-7

- [ ] Evaluation calls real system behavior and can reproduce a stored run.
- [ ] Dashboard values match persisted evaluation/feedback data.
- [ ] A candidate prompt/retrieval change can be compared and rolled back.
- [ ] Every tool call is authorized, bounded, synthetic, read-only, and audited.

---

## 14. WP-8 — Observability, resilience, security, and recovery

### Objective

Make failure diagnosable, bounded, and recoverable.

### 14.1 Telemetry

- [ ] Add OpenTelemetry tracing to gateway, HTTP clients/servers, Kafka producers/consumers, database operations, workflow steps, and model-provider calls.
- [ ] Propagate W3C trace context and application correlation IDs across HTTP and Kafka.
- [ ] Use structured JSON logs in production with service, environment, trace ID, correlation ID, tenant hash/approved identifier, event/workflow IDs, and safe error code.
- [ ] Redact tokens, cookies, authorization headers, PII, ticket bodies, prompts, retrieved text, and provider payloads.
- [ ] Add Micrometer metrics with bounded labels only.

Required metrics include:

- request rate/error/latency;
- auth failure/rate-limit/lockout;
- ticket create/transition/conflict;
- outbox pending/retry/dead count and oldest age;
- consumer lag/retry/DLQ;
- workflow duration by step/status;
- indexing jobs/chunks/failures;
- retrieval latency/result count/zero result;
- provider latency/error/token/cost with bounded model labels;
- suggestion acceptance/edit/reject/abstain;
- SLA at-risk/breached counts.

### 14.2 Dashboards and alerts

- [ ] Check dashboard definitions into the repository; a Grafana container alone is insufficient.
- [ ] Create platform health, ticket/SLA, events/workflows, RAG/indexing, AI quality/cost, and security dashboards.
- [ ] Add alerts for sustained error rate, outbox age, DLQ growth, workflow failures, provider budget/error, DB/Kafka health, and backup failure.
- [ ] Each alert links to a tested runbook and includes service/impact/correlation guidance.

### 14.3 Resilience and budgets

- [ ] Configure timeouts at every remote boundary.
- [ ] Retry only idempotent operations and retryable failures.
- [ ] Add circuit breakers/bulkheads for AI, embedding, object store, and relevant service calls.
- [ ] Bound thread pools, queues, Kafka concurrency, request sizes, page sizes, retrieval candidates, prompt size, and provider tokens.
- [ ] Add per-tenant/provider daily budget and global kill switch.
- [ ] Apply backpressure and expose manual/degraded modes.
- [ ] Document capacity assumptions for the portfolio deployment.

### 14.4 Load and failure testing

- [ ] Add a reproducible load suite for ticket creation, queue reads, retrieval, and workflow throughput using synthetic data.
- [ ] Record p50/p95/p99 latency, throughput, errors, resources, and dataset size.
- [ ] Test Kafka unavailable/slow, PostgreSQL latency, provider timeout/429/5xx, MinIO unavailable, telemetry unavailable, duplicate messages, and service restarts.
- [ ] Confirm telemetry failure never blocks the core transaction.
- [ ] Confirm AI failure preserves manual support.

### 14.5 Backup, restore, and security supply chain

- [ ] Implement backup scripts/configuration, not only prose.
- [ ] Back up PostgreSQL and required object metadata/content according to documented RPO/RTO.
- [ ] Perform and record a restore drill into an isolated environment.
- [ ] Add dependency scanning, secret scanning, SAST, container scanning, and SBOM generation to CI.
- [ ] Build non-root, minimal application images with health checks and pinned base images.
- [ ] Resolve or explicitly accept vulnerabilities with owner, reason, expiry, and compensating control.
- [ ] Update STRIDE threat model from actual implementation.

### Gate WP-8

- [ ] A ticket can be traced from gateway through event, workflow, retrieval, suggestion, and send.
- [ ] Alerts fire in controlled tests and lead to correct runbooks.
- [ ] Load and failure reports meet documented targets or clearly record accepted limits.
- [ ] Restore drill succeeds within target.
- [ ] No unaccepted critical/high security finding remains.

---

## 15. WP-9 — Containerization, deployment, and portfolio delivery

### Objective

Publish a safe, reproducible demonstration that an interviewer can understand and verify.

### 15.1 Local full stack

- [ ] Add Dockerfiles for backend services and frontend.
- [ ] Add Compose profiles for infrastructure-only and full application.
- [ ] Use health checks and dependency readiness, not arbitrary sleeps.
- [ ] Seed only fictional tenants/users/tickets/knowledge/resolved cases.
- [ ] Provide one documented startup sequence and one teardown sequence that preserves or explicitly removes data.
- [ ] Verify a clean clone on a second environment/CI runner.

### 15.2 Staging deployment

- [ ] Deploy over HTTPS with only frontend/gateway public.
- [ ] Use managed secrets, separate production-like profile, restricted CORS, rate limits, and budget caps.
- [ ] Disable public self-registration if abuse controls are insufficient; provide controlled demo accounts.
- [ ] Do not expose Kafka, PostgreSQL, MinIO console, actuator internals, Prometheus, Grafana, discovery, or service ports publicly.
- [ ] Add synthetic demo reset/cleanup behavior.
- [ ] Add uptime/cost ownership and a safe shutdown plan.

### 15.3 Coherent demo scenario

Seed a fictional duplicate-charge scenario containing:

- customer account and ticket;
- published duplicate-charge policy with immutable version;
- two sanitized approved resolved cases;
- deterministic routing rule and SLA policy;
- real hybrid retrieval results;
- grounded cited draft;
- human edit/approval/send record;
- visible feedback and evaluation record;
- one recoverable failure/DLQ example for operations demonstration.

### 15.4 Portfolio assets

- [ ] README opening that explains problem, users, solution, measured result, stack, and demo in under 30 seconds.
- [ ] Architecture component diagram and ticket/event sequence diagram.
- [ ] Security and human-in-the-loop explanation.
- [ ] Real evaluation report with dataset/method limitations.
- [ ] Load-test summary with environment and data size.
- [ ] Screenshots for customer, agent intelligence/citations, knowledge lifecycle, evaluation, and operations.
- [ ] Three-to-five-minute demo video showing the complete vertical journey.
- [ ] ADR summary explaining pgvector, hybrid retrieval, async orchestration, outbox/idempotency, provider abstraction, and Kubernetes deferral.
- [ ] API quick start and sample requests using synthetic credentials/data.
- [ ] Honest limitations and future work.

### Gate WP-9

- [ ] A clean clone can run the full stack from documented commands.
- [ ] The public/demo journey works without manual database edits or hardcoded UI results.
- [ ] Every public metric and claim links to reproducible evidence.
- [ ] HTTPS, abuse prevention, spending cap, synthetic data, and secret handling are verified.
- [ ] Resume wording is updated only with measured, demonstrable functionality.

---

## 16. Exact end-to-end acceptance scenario

The project is not complete until this scenario passes as an automated Playwright/API integration journey and as a recorded manual demo.

### Setup

- Tenant: fictional `Northstar Commerce`.
- Users: customer, agent, team lead, knowledge manager, admin, and auditor.
- Knowledge: published `Duplicate card charge policy`, active version and chunks.
- Resolved cases: at least two approved sanitized examples.
- Routing: billing/duplicate-charge routes to Billing Support.
- SLA: deterministic first-response and resolution policy.
- Providers: configured real embedding/chat provider with a documented budget; deterministic adapter only in test profile.

### Journey

1. Customer authenticates.
2. Customer submits a duplicate-charge ticket with an idempotency key.
3. Retried identical request returns the same ticket.
4. Ticket and `TicketCreated.v1` outbox row commit atomically.
5. Publisher delivers the event; consumer records the event exactly once.
6. Orchestration performs validated classification.
7. Routing selects Billing Support and records reason/policy IDs.
8. SLA deadlines are persisted.
9. RAG executes real SQL lexical/vector queries with active/tenant/ACL filters.
10. Retrieval run and citation candidates are persisted.
11. Draft generation produces a cited suggestion or abstains if evidence is insufficient.
12. Validators record schema, PII, policy, and citation outcomes.
13. Suggestion is persisted before completion is emitted.
14. Agent sees the ticket and suggestion from APIs, opens citation source versions, and edits the draft.
15. Agent approves and sends through an idempotent human action.
16. Customer sees exactly one response.
17. Feedback, audit events, traces, metrics, model metadata, and cost are visible to authorized roles.
18. An unauthorized tenant/customer/auditor attempt is rejected and audited safely.

### Failure variant

Repeat with the chat provider unavailable:

- ticket creation and manual queue continue;
- workflow shows a truthful degraded/manual status;
- no fake successful draft is created;
- agent can manually respond;
- retry/circuit-breaker/alert telemetry is visible;
- no duplicate response occurs after recovery.

---

## 17. Required test architecture

### Unit tests

Use for pure domain behavior:

- ticket state transitions;
- permissions and policy decisions;
- SLA calculations;
- RRF and score calculations;
- schema/output validators;
- sanitization;
- suggestion lifecycle;
- retry classification.

### Integration tests

Use Testcontainers or equivalent real dependencies for:

- Flyway migrations and constraints;
- repository tenant scoping;
- PostgreSQL full-text and pgvector;
- transactional outbox and locks;
- Kafka redelivery/idempotent consumers/DLQ;
- MinIO attachment ownership and expiry;
- auth JWT/refresh lifecycle;
- HTTP service contracts.

### Contract tests

- Versioned event serialization/deserialization and backward compatibility.
- OpenAPI request/response contracts.
- Provider adapter behavior using controlled stub servers.
- Frontend generated/typesafe client compatibility.

### End-to-end tests

- Mandatory journeys from WP-6 and Section 16.
- Run through gateway and real services/databases/broker.
- Use synthetic seeded data and deterministic provider stubs in CI.
- Run a separate opt-in provider smoke test without making CI depend on third-party availability.

### Security tests

- Authorization matrix, IDOR, tenant isolation, spoofed headers, JWT failures, CSRF/cookie behavior, rate limiting, upload abuse, prompt injection, PII, audit integrity, and dependency/container scanning.

### Performance/failure tests

- Representative dataset sizes must be stated in every report.
- Do not publish scale claims extrapolated from a laptop.
- Save configuration and raw summary results alongside reports.

---

## 18. Database and migration rules

For every schema change:

1. create a new forward-only Flyway migration; never edit an applied migration after shared use;
2. keep each service inside its owned schema/database boundary;
3. use tenant-aware unique constraints and indexes;
4. add indexes for actual query predicates and verify with `EXPLAIN ANALYZE` on representative synthetic data;
5. make nullable/backfill/not-null transitions deployment-safe;
6. document data migration, compatibility window, and rollback/forward-fix;
7. add migration integration tests from an empty database and the previous supported schema;
8. do not create cross-service foreign keys or query another service's tables;
9. use UTC instants for persisted event/audit timestamps and explicit tenant timezone for business-calendar calculations;
10. never store raw secrets, refresh tokens, or unnecessary PII.

---

## 19. API and event completion rules

Every API change must provide:

- authenticated actor and authorization rule;
- tenant/resource ownership rule;
- validated request schema;
- success, validation, authentication, authorization, not-found, conflict, rate-limit, and server-error behavior;
- idempotency rule for commands;
- pagination for collections;
- OpenAPI update and examples;
- unit/integration/authorization tests;
- metrics/logging/tracing behavior;
- safe rollback/feature flag where risk requires it.

Every event change must provide:

- event name/version and owning producer;
- schema and example;
- partition key/order expectations;
- consumer list;
- idempotency behavior;
- retryable/non-retryable classification;
- DLQ/replay behavior;
- retention and sensitive-data classification;
- contract tests and backward-compatibility policy.

---

## 20. Production configuration rules

Production-like profile must fail startup when:

- JWT signing/verification configuration is absent or uses a development value;
- mock embedding/chat providers are selected;
- required database/Kafka/object-store credentials are defaults;
- wildcard credentialed CORS is enabled;
- secure refresh-cookie settings are disabled;
- encryption/signing keys are missing;
- dangerous demo reset/seed endpoints are enabled;
- actuator or internal consoles are publicly exposed.

Use `.env.example` only for names and safe local placeholders. Real secrets must come from environment/secret management and must never be printed by startup diagnostics.

---

## 21. Agent completion report template

At the end of every bounded issue, report:

```markdown
## Issue completed

### Outcome
What now works from a user/system perspective.

### Files changed
- path: reason

### Data/API/event changes
- Migration:
- API:
- Event:
- Compatibility:

### Security and privacy
- Authorization:
- Tenant isolation:
- Sensitive data:

### Verification
- `command` — PASS/FAIL and meaningful count/result

### Failure/degraded behavior tested
- scenario and observed result

### Remaining limitations
- honest unresolved item

### Rollback/disable path
- procedure or feature flag

### Recommended next issue
- next dependency-ordered item

### Repository action
No commit/push performed unless explicitly authorized by the owner.
```

Do not say “complete” if a required test was not run. State `not verified` and why.

---

## 22. Master completion checklist

### Baseline

- [ ] Backend verify, frontend lint/test/build, Compose parse, secret scan, and migration tests pass.
- [ ] CI runs all baseline gates.
- [ ] Documentation contains no false implementation claims.

### Security

- [ ] Public registration cannot assign roles/tenant arbitrarily.
- [ ] Gateway and owning services validate authentication.
- [ ] RBAC, tenant, customer ownership, and team scope are enforced and tested.
- [ ] Refresh rotation/reuse/logout/reset/rate-limit/audit flows pass.
- [ ] Spoofed identity headers and IDOR are blocked.

### Manual support domain

- [ ] Manual ticket journey works without AI/Kafka.
- [ ] State machine, history, SLA, numbering, idempotency, messages, and attachments work.

### Events/workflow

- [ ] All producers use working outbox publishers, including retries.
- [ ] All consumers are idempotent.
- [ ] Workflow is resumable and has correct transaction boundaries.
- [ ] DLQ/replay and operations visibility work.
- [ ] Persisted suggestions match emitted IDs.

### Knowledge/RAG

- [ ] Governed document/resolved-case lifecycle works.
- [ ] Embeddings are generated once per version and tracked.
- [ ] PostgreSQL FTS and pgvector retrieval execute with filters.
- [ ] Atomic activation/rollback and citations work.
- [ ] Real retrieval evaluation meets approved targets.

### AI copilot

- [ ] Real provider adapter and safe deterministic test adapter exist.
- [ ] Structured validation, fallback, injection/PII/policy/citation checks pass.
- [ ] Grounded draft or honest abstention is persisted.
- [ ] Human approval is the only send path.
- [ ] Provenance, cost, latency, and feedback are stored.

### Frontend

- [ ] Real authentication/session and protected routes work.
- [ ] Customer, agent, lead, knowledge, admin, audit, evaluation, and operations screens use APIs.
- [ ] Hardcoded production data/metrics and role-switch authentication are removed.
- [ ] Loading/error/degraded/mobile/accessibility states and E2E journeys pass.

### Evaluation/governance

- [ ] Frozen synthetic datasets are large and representative enough for declared claims.
- [ ] Runner measures actual behavior and stores reproducible per-case output.
- [ ] Feedback and activation/rollback workflow work.
- [ ] Optional tools are read-only, authorized, bounded, synthetic, and audited.

### Production/demo

- [ ] Traces, metrics, logs, dashboards, alerts, and runbooks are operational.
- [ ] Load/failure/backup/restore/security/container/SBOM gates pass.
- [ ] Full stack starts from a clean clone.
- [ ] HTTPS staging uses synthetic data, rate limits, and provider budgets.
- [ ] Demo video, diagrams, screenshots, measured reports, and honest README are complete.

---

## 23. Final release decision

ResolveIQ may be called **portfolio-ready** only when WP-0 through WP-9 gates pass and the exact end-to-end scenario is reproducible. Before that point, describe it using the most accurate current label:

- **architecture scaffold** when only modules/schemas/docs exist;
- **prototype** when mocked/static flows demonstrate UX;
- **working vertical slice** when one secure real journey passes;
- **portfolio-ready system** only after evaluation, resilience, deployment, and documentation gates pass.

The final resume statement must use measured values from the checked-in evaluation/load reports and must not imply real customer deployment or business impact unless that evidence exists.
