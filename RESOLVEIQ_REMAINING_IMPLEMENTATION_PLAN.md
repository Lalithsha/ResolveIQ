# ResolveIQ Remaining Implementation Plan

> **Document status:** Implementation handoff and execution source of truth  
> **Prepared from:** Repository audit against `RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md`  
> **Audit date:** 2026-08-29  
> **Scope:** All work still required to turn the current architectural prototype into a secure, measurable, end-to-end portfolio application  
> **Repository:** `resolveiq`  

---

## Implementation execution update — 2026-08-30

The core customer-to-agent vertical slice described below has now been implemented in code: JWT defense-in-depth, HttpOnly refresh sessions, scoped command idempotency, sequence-backed ticket numbers, reliable outbox publishers, idempotent consumers, short workflow transactions, persisted workflow attempts/replay inputs, strict dependency failure handling, real suggestion persistence, routing/assignee/SLA propagation, deterministic and OpenAI-compatible provider adapters, stored pgvector embeddings, PostgreSQL lexical/vector RRF retrieval, authenticated React flows, and removal of silent UI simulation for primary commands.

Repository verification completed after implementation:

- `./mvnw clean verify` — passes (local Java 26 produces JaCoCo warnings for JDK classes; CI is pinned to Java 21).
- `npm run lint && npm run test && npm run build` in `frontend` — passes.
- `docker compose config --quiet` — passes.
- `./scripts/scan-secrets.sh` — passes.

Docker-backed runtime verification is now complete for the primary automated triage slice. The stack was run with PostgreSQL/pgvector, Kafka, Eureka, gateway, all application services, and the React container. A fictional customer authenticated through the gateway and created ticket `RIQ-2026-100004` (`6041034a-51c7-4c8b-a57c-e385acf3e74d`). Runtime and database evidence confirmed:

- the ticket-created outbox event was durably published;
- orchestration consumed it and persisted a `COMPLETED` workflow;
- deterministic analysis, routing, RAG, and grounded-draft steps ran across real HTTP service boundaries;
- the orchestration completion outbox event reached `PUBLISHED`;
- the ticket consumer projected `READY_FOR_AGENT`, `ai_triage_status=SUCCESS`, a team assignment, and both SLA deadlines;
- the corresponding AI suggestion was persisted as `PENDING_REVIEW`, with model/prompt metadata, confidence `0.6`, and an empty citation set because the test tenant had no approved knowledge chunks.

Runtime verification found and fixed four defects that unit tests had not exposed: executable Spring Boot Docker jars, pgvector extension schema placement, missing orchestration scheduling, and obsolete completion-event topic routing. It also verified that malformed enum/JSON requests return a structured `400` path rather than being misreported as authentication failures.

The unchecked acceptance items later in this document remain the authoritative production-hardening backlog. The verified primary slice does **not** complete object-storage attachments, password reset, exhaustive cross-tenant HTTP tests, Kafka outage/redelivery tests, Testcontainers coverage, load tests, browser E2E coverage, or AI evaluation datasets. Do not claim the full production gate until those checks pass.

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
| Full Compose runtime | Primary automated-triage slice verified on 2026-08-30; broader failure/load/browser gates remain |
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

---

## 24. Second implementation audit — current repository status

> **Re-audit date:** 2026-08-29
> **Audited commit:** `979e618`
> **Verdict:** The repository is a partially integrated prototype. It is not complete, not yet a working AI/vector vertical slice, and not portfolio-ready under this plan.

This section records the state after the first large remediation implementation. It is the starting point for all subsequent agent work. The checkboxes in earlier sections remain unchecked intentionally: an agent may mark an item complete only after its required automated or manual evidence exists.

### 24.1 Commands verified during the second audit

| Command/check | Result | Interpretation |
|---|---|---|
| `./mvnw clean verify` | Passed | Twelve backend unit/mock tests passed; this does not prove service startup or distributed behavior |
| `npm run lint` | Passed | Frontend static lint baseline is operational |
| `npm run test` | Passed | Only one frontend smoke test exists |
| `npm run build` | Passed | TypeScript and Vite production compilation succeed |
| `docker compose config --quiet` | Passed | Compose YAML is syntactically valid |
| `./scripts/scan-secrets.sh` | Passed | The repository's basic pattern scan found no matching secret |
| Evaluation runner | Executed | Recall@5 was 97%; MRR changed between executions because the runner uses randomized Python `hash()` |
| `docker compose ps` | Not run successfully | Docker daemon was unavailable during audit |
| Git state | Clean | `main` tracked `origin/main`; audit made no product change |

The Maven run emitted JaCoCo instrumentation warnings on the local newer JDK because JaCoCo 0.8.12 does not understand class-file major version 70. CI is configured for Java 21, but the local toolchain warning must still be resolved or the project must enforce Java 21 through Maven Toolchains/Enforcer so coverage results cannot silently degrade.

### 24.2 Improvements verified

- Public registration no longer accepts roles and currently assigns `CUSTOMER`.
- Gateway strips inbound tenant/user/role/internal-caller headers.
- Gateway verifies the JWT HMAC signature and injects identity headers.
- Customer ticket endpoints now require tenant/user headers and customer-scoped service queries.
- Ticket creation has an initial idempotency implementation.
- Native PostgreSQL lexical and pgvector query methods exist.
- Retrieval stores citation records.
- Orchestration-service now has an outbox publisher.
- A typed frontend API client and an unused authentication context exist.
- Customer, agent, knowledge, and governance screens make some API calls.
- Frontend ESLint, Vitest, Dockerfiles, a Grafana dashboard, Prometheus alerts, and a 100-query dataset exist.
- CI runs backend build, frontend lint/test/build, and a basic secret scan.

### 24.3 Release-blocking findings still present

#### Runtime/startup blockers

- No Spring bean implements `EmbeddingPort` in `rag-service`.
- No Spring bean implements `ChatClientPort` in `ai-analysis-service`.
- Configuration defaults to mock provider names and mock API keys, but no main-source mock adapter exists either.
- Maven tests instantiate mocks manually and do not start application contexts, so the green build does not detect these missing runtime beans.
- Compose contains infrastructure services only; none of the Spring services or the frontend are started.

#### Security blockers

- Gateway accepts a hardcoded development JWT secret fallback.
- JWT issuer, audience, token type, and required role claims are not fully validated.
- `/actuator/**` is public at gateway and auth-service.
- Gateway authenticates but does not enforce route-level roles.
- Ticket, RAG, routing, analysis, and orchestration services do not independently validate credentials or authorize roles.
- Downstream controllers still trust identity headers; a directly reachable service can be spoofed.
- Workflow read/retry operations lack tenant-safe lookup and operations/admin authorization.
- Access tokens are stored in browser `localStorage`; refresh tokens are returned in JSON rather than secure cookies.
- Password reset, logout-all, cookie refresh, complete refresh-family behavior, CSRF/CORS policy, and authorization matrices are incomplete.

#### Ticket-domain blockers

- Ticket number uses a process-local `AtomicLong` and hardcoded `2026`; restart/concurrency uniqueness is unsafe.
- Idempotency key is not scoped by tenant, user, route, and request hash.
- Same key plus different body is not rejected.
- Concurrent idempotent creates are not proven safe.
- Attachments/MinIO application integration is absent.
- Team membership, skill scope, complete routing administration, business calendars, and complete SLA behavior are not proven.

#### Event/workflow blockers

- Ticket and workflow outbox publishers query only `PENDING`; records changed to `RETRY` are never selected again.
- Publishers block inside a database transaction on Kafka `.get()`.
- There is no due time/backoff, safe multi-instance claim, complete dead-letter persistence, or verified replay.
- Orchestration consumer has no processed-event idempotency and catches malformed events without a real DLQ path.
- The whole triage workflow is one transaction containing remote HTTP calls.
- Workflow steps are not resumable after process termination.
- The completion event contains a random suggestion ID that is not created before publication.
- The current workflow retry endpoint updates status but does not enqueue/resume durable work.

#### RAG blockers

- Ingestion saves only `embedding_model`; it never generates or writes an embedding value.
- Chunk entities do not map the vector column.
- Native pgvector queries therefore cannot retrieve newly ingested chunks.
- Lexical SQL lacks rank ordering.
- ACL/product/language/effective-date/embedding-version filters are incomplete.
- SQL exceptions are swallowed, and empty/error paths fall back to unbounded tenant chunk loading.
- Knowledge versions are published immediately instead of using review, staged indexing, atomic activation, and rollback.
- Resolved-case approval trusts already-sanitized request strings instead of sanitizing and showing an approval diff.

#### AI copilot blockers

- There is no real chat-model adapter.
- Draft generation is a Java string template rather than a provider-backed, schema-controlled generation step.
- Analysis output accepts arbitrary enums/ranges and is recorded as `VALID` even when parsing fails.
- Prompt-injection protection is basic string replacement.
- PII, secret, policy, claim support, citation coverage, and groundedness validators are incomplete.
- Suggestion provenance/lifecycle, invalidation, regeneration, review concurrency, and exactly-once send are incomplete.

#### Frontend blockers

- `AuthProvider` is not mounted.
- React Router and React Query are not mounted.
- `App` starts in an agent role and swaps pages through a role dropdown.
- `AuthContext` supplies a fictional agent by default and writes identity/roles to local storage.
- There are no login/register/reset/protected routes.
- Static metrics, mock model names, alert-based actions, and incomplete data/error states remain.
- There is only one frontend test, checking that the ResolveIQ brand renders.
- No Playwright or axe suite exists.

#### Evaluation/production blockers

- Evaluation uses an in-process token overlap and pseudo-vector implementation, not ResolveIQ's retrieval API/PostgreSQL/provider path.
- Python's process-randomized `hash()` makes MRR non-reproducible.
- PII leakage, cross-tenant leakage, and autonomous-send rate are hardcoded as zero.
- The report always says all gates passed.
- No development/test split, safety dataset execution, per-case persisted diagnostics, or application-backed latency exists.
- No application Compose profile, staging HTTPS deployment, container CI scanning/SBOM, Testcontainers suite, E2E suite, load/failure test, backup script/restore drill, or portfolio demo evidence exists.

### 24.4 Updated status ledger

| Work package | Current state | Gate decision |
|---|---|---|
| WP-0 baseline | Lint/build/basic unit tests improved | **Open** — migration/context/integration and honest-doc gates missing |
| WP-1 security | Registration and gateway header stripping improved | **Open/P0** — service auth, RBAC, JWT/cookie lifecycle missing |
| WP-2 ticket domain | Ownership/idempotency partially improved | **Open** — numbering, robust idempotency, attachments/SLA incomplete |
| WP-3 workflow | Orchestration publisher added | **Open/P0** — retry/idempotency/transaction/suggestion invariants broken |
| WP-4 RAG | Native SQL and citation persistence added | **Open/P0** — no embeddings/provider/index lifecycle |
| WP-5 AI copilot | Existing mock/template behavior remains | **Open/P0** — real provider, validation, persisted suggestion missing |
| WP-6 frontend | Some pages call APIs | **Open** — app shell/auth/routing and complete journeys missing |
| WP-7 evaluation | Dataset expanded to 100 | **Open** — runner is non-production and safety values are asserted |
| WP-8 hardening | Dashboard/alerts/Dockerfiles added | **Open** — instrumentation, tests, recovery/security gates missing |
| WP-9 delivery | Image recipes exist | **Open** — full stack, staging and portfolio evidence missing |

---

## 25. Exact dependency-ordered implementation backlog

The following issues are intentionally prescriptive. Execute them in order. Do not combine all issues into a single large commit. An issue is complete only when its tests and gate pass.

### RIQ-001 — Enforce the toolchain and make startup tests part of the build

**Goal:** Make a green build detect missing application beans, invalid migrations, and unsupported local Java versions.

**Implementation:**

1. Add Maven Enforcer rules at the parent level for Java 21 and the required Maven version.
2. Add `.mvn/jvm.config` only for safe shared JVM flags; do not hide test or instrumentation failures.
3. Upgrade JaCoCo to a version compatible with supported local/CI JDKs or require Maven Toolchains to execute tests with Java 21.
4. Add a minimal `@SpringBootTest` context test to gateway, auth, ticket, analysis, routing, RAG, and orchestration modules.
5. Use a `test` profile with explicit deterministic provider beans and container-backed dependencies. Do not use production mock defaults.
6. Add a Testcontainers PostgreSQL image with pgvector. Run every Flyway migration through it.
7. Add Kafka Testcontainers for producer/consumer integration suites.
8. Remove either `frontend/bun.lock` or `frontend/package-lock.json`; retain npm and `package-lock.json` because CI already uses `npm ci`, unless the owner explicitly chooses Bun.
9. Add a CI integration job with Docker services/Testcontainers and publish test/coverage artifacts.
10. Add a JaCoCo `check` execution with an initial agreed floor. Require high coverage for authorization, state machines, idempotency, event handling, and validators rather than chasing a misleading global percentage.

**Tests/gate:**

- [ ] `java -version` and Maven Enforcer show Java 21 for verification.
- [ ] Every service context starts in `test` profile.
- [ ] Empty-database migration test passes using PostgreSQL/pgvector.
- [ ] A deliberately removed provider bean makes the context test fail.
- [ ] JaCoCo generates reports without unsupported-class warnings and enforces its floor.
- [ ] Exactly one frontend lockfile remains.

### RIQ-002 — Replace the header trust model with defense-in-depth JWT authentication

**Goal:** A client cannot gain identity, tenant, or role by bypassing the gateway or spoofing headers.

**Pattern:** OAuth2 resource server at the gateway **and every owning service**. Forward the original Bearer token; each service derives identity from its validated `Jwt`. Headers may carry correlation/trace metadata but must not be the authorization source.

**Implementation:**

1. Add issuer and audience claims to access tokens; add a `token_type=access` claim and unique `jti`.
2. Configure validation for signature, algorithm allow-list, issuer, audience, expiry, not-before, subject UUID, tenant UUID, token type, and non-empty roles.
3. Remove the default JWT secret in production-like profiles. Startup must fail if the secret/key source is absent or still equals the local example.
4. Prefer asymmetric signing for portfolio staging: auth signs with a private key; gateway/services verify with the public key. If HMAC remains for local scope, document shared-secret distribution and rotation.
5. Keep gateway stripping all inbound identity headers.
6. Stop using those headers inside business controllers. Create a small security adapter in each service that maps `Jwt` to immutable `ResolveIqPrincipal(userId, tenantId, roles)`.
7. Add `SecurityFilterChain`/`SecurityWebFilterChain`, `@EnableMethodSecurity`, and `@PreAuthorize` or explicit authorization policy calls in every owning service.
8. Scope resource lookup by principal tenant and ownership/team permission. Never accept tenant/user identity from a public request body/query/header.
9. Restrict actuator exposure to `health` and `info`; protect details and all metrics/admin endpoints. Do not expose service actuator ports publicly.
10. Restrict CORS to configured frontend origins. Document cookie/CSRF behavior after RIQ-004.
11. If internal service-to-service endpoints require a different audience, mint/use an internal service credential rather than a magic `X-Internal-Caller` string.

**Controller pattern:**

```java
@GetMapping("/{id}")
@PreAuthorize("hasRole('CUSTOMER')")
public TicketResponse getTicket(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    ResolveIqPrincipal principal = principalMapper.from(jwt);
    return ticketService.getCustomerTicket(principal.tenantId(), id, principal.userId());
}
```

Do not centralize JPA repositories or service-specific domain authorization in `common-contracts`. Shared code may contain token-claim names and principal mapping only.

**Tests/gate:**

- [ ] Missing/malformed/expired/future/wrong-signature/wrong-issuer/wrong-audience/wrong-token-type JWTs return `401`.
- [ ] Valid identity with insufficient role returns `403`.
- [ ] Spoofed headers through gateway are overwritten/ignored.
- [ ] Calling a service directly with spoofed headers and no JWT returns `401`.
- [ ] Cross-tenant UUID and same-tenant cross-customer IDOR tests pass for every resource type.
- [ ] Agent team scope and auditor read-only matrix pass.
- [ ] Public actuator exposure is limited to a non-sensitive health response.

### RIQ-003 — Complete authentication and browser session lifecycle

**Goal:** Deliver a secure, usable browser authentication flow.

**Implementation:**

1. Return the short-lived access token in the authentication response but return refresh token only as a `HttpOnly` cookie.
2. Use `Secure=true` outside local HTTP development, explicit path, `SameSite=Lax` or stricter where compatible, bounded expiry, and no broad domain.
3. Make refresh accept the cookie, rotate the token on every use, revoke the previous record, and detect reuse by token family.
4. On reuse, revoke the entire family and audit it.
5. Implement logout-current by revoking the current family and clearing the cookie.
6. Implement logout-all by revoking all active families for the authenticated user.
7. Implement password reset with random high-entropy, hashed, one-time, expiring token; always return an enumeration-safe public response.
8. Add login rate limiting/lockout with consistent audit events and a recovery rule.
9. Ensure admin-created users belong to the admin's tenant unless a platform-level role explicitly exists; do not trust arbitrary tenant ID from a tenant admin.
10. Remove tokens from frontend `localStorage`. Keep access token in memory and perform a cookie-based refresh on application bootstrap.
11. Add CSRF defense if cookie-authenticated state-changing endpoints use browser cookies beyond refresh/logout; document the exact model.

**Tests/gate:**

- [ ] Cookie flags are correct in local and production profiles.
- [ ] Refresh rotates; old-token replay revokes the family.
- [ ] Logout-current and logout-all invalidate expected sessions.
- [ ] Password reset token is hashed, expires, is single-use, and public response is enumeration-safe.
- [ ] Browser reload restores a valid session without local-storage tokens.
- [ ] XSS-accessible storage contains no bearer or refresh token.

### RIQ-004 — Fix ticket numbers and command idempotency

**Goal:** Ticket and send operations remain correct after restart, concurrency, and client retry.

**Implementation:**

1. Add a Flyway migration for a ticket-number sequence or counter table. Prefer a PostgreSQL sequence for global monotonic numbers; if yearly reset is mandatory, lock a `(tenant_id, year)` counter row.
2. Generate the year from a documented business timezone; never hardcode it.
3. Keep a database unique constraint on `(tenant_id, ticket_number)`.
4. Replace the current string-primary-key idempotency design with a record containing tenant ID, actor ID, operation/route, key, canonical request SHA-256, status (`IN_PROGRESS`/`COMPLETED`/`FAILED_RETRYABLE`), response code/body/reference, created/expiry timestamps, and version.
5. Add a unique constraint on `(tenant_id, actor_id, operation, idempotency_key)`.
6. Canonicalize request fields before hashing. Exclude volatile transport headers.
7. Insert/claim the idempotency row in the same transaction as the command. Handle unique-constraint races by re-reading.
8. Same scope/key/hash returns the original response. Same scope/key/different hash returns `409 IDEMPOTENCY_KEY_REUSED`.
9. Require idempotency keys for ticket create, message/reply create, suggestion approve/send, replay, and other externally retried commands.
10. Add bounded retention cleanup with metrics.

**Tests/gate:**

- [ ] Parallel creation test produces one ticket and one outbox event.
- [ ] Service restart cannot repeat ticket number.
- [ ] Same key/body returns identical resource/status.
- [ ] Same key/different body returns `409`.
- [ ] Same textual key in another tenant/user/operation is independent.
- [ ] Duplicate send produces one customer-visible message/event.

### RIQ-005 — Finish the manual ticket, SLA, team, and attachment domain

**Goal:** ResolveIQ remains a complete support application without AI.

**Implementation:**

1. Finish ticket states and transition authorization from the blueprint; persist immutable actor/source/reason/correlation history.
2. Add paginated customer/agent queues and bounded filters/sorts.
3. Persist customer replies, public agent replies, and internal notes separately; never expose internal notes to customers.
4. Implement team, membership, skills, routing-rule, SLA-policy, and business-calendar administration with tenant scope.
5. Persist routing/SLA selected rule IDs and reason codes.
6. Implement SLA pause/resume/at-risk/breach/completion calculations using tenant timezone and business calendar.
7. Introduce `AttachmentStoragePort` in ticket-service and a MinIO adapter.
8. Persist attachment metadata/state. Use server object keys, safe display names, allow-listed MIME/type, size/count limits, quarantine/scanned state, signed URLs, ownership checks, and abandoned-upload cleanup.
9. Keep MinIO implementation behind the port and provide a test adapter.
10. Do not send unscanned attachment contents to models.

**Tests/gate:**

- [ ] Every role/state transition combination is tested.
- [ ] Internal note never appears in customer APIs.
- [ ] Queue pagination is bounded and stable.
- [ ] SLA timezone/calendar/pause/breach boundaries pass.
- [ ] Upload traversal, MIME mismatch, oversize, expired URL, quarantine, cross-ticket, and cross-tenant tests pass.
- [ ] Customer-to-agent-to-customer manual E2E passes with Kafka and AI disabled.

### RIQ-006 — Implement a reusable, correct transactional outbox pattern

**Goal:** Every committed business event is eventually attempted, including failed retry rows, without unsafe database locks around Kafka waits.

**Implementation:**

1. Add `attempt_count`, `next_attempt_at`, `last_error_code`, `claimed_at`, `claimed_by`, and terminal timestamp fields to both outboxes through new migrations.
2. Create repository native query to claim due `PENDING` or `RETRY` rows using `FOR UPDATE SKIP LOCKED`, ordered by due/created time, in a short transaction.
3. Change claimed rows to a transient claimed state or lease them with `claimed_at/claimed_by`; commit the claim transaction.
4. Publish outside the database transaction with bounded timeout.
5. In a new short transaction, mark `PUBLISHED` or calculate exponential-backoff `RETRY`; after maximum attempts mark `DEAD`.
6. Recover expired claims after a worker crash.
7. Use event ID in Kafka headers and ticket ID as partition key.
8. Apply the same tested component/pattern to ticket and orchestration services without sharing their database tables.
9. Add metrics for count/oldest age/attempts/dead and logs with event/correlation IDs.

**Tests/gate:**

- [ ] `RETRY` row becomes eligible only after `next_attempt_at`.
- [ ] Two publisher instances safely split/claim work.
- [ ] Crash after publish but before database update results in harmless duplicate delivery.
- [ ] Broker outage retains events and recovery publishes them.
- [ ] Long Kafka waits do not hold database transactions/row locks.

### RIQ-007 — Add consumer idempotency, retries, DLQ, and audited replay

**Goal:** At-least-once Kafka delivery causes at-most-one business effect, and poison events are visible/recoverable.

**Implementation:**

1. Add processed-event storage to every consuming service, especially orchestration.
2. Validate envelope/event version/tenant/aggregate fields before invoking business logic.
3. Insert processed event and perform the business mutation in the same local transaction.
4. Duplicate event ID returns a successful no-op.
5. Remove catch-and-log swallowing from Kafka listeners.
6. Configure retry classification: timeout/temporary provider/DB errors retry; malformed schema/unsupported version/domain invariant go directly to DLQ.
7. Add retry topics or a Spring Kafka error handler with bounded delays and a dead-letter recoverer.
8. Persist DLQ inventory metadata for operations without duplicating sensitive payload unnecessarily.
9. Implement admin-only tenant-safe replay: actor, reason, original ID, replay ID, timestamp, and outcome. Replay passes normal validation/idempotency.
10. Replace the current status-only workflow retry endpoint with a command that durably enqueues or resumes work.

**Tests/gate:**

- [ ] Duplicate ticket event creates one workflow.
- [ ] Poison event reaches DLQ rather than disappearing.
- [ ] Transient failure retries only the configured number of times.
- [ ] Unauthorized replay returns `403`; cross-tenant replay is impossible.
- [ ] Replayed event is audited and cannot duplicate an already completed effect.

### RIQ-008 — Refactor orchestration into a resumable process manager

**Goal:** No external call occurs inside a database transaction; any service restart resumes from durable state.

**Pattern:** Process manager/saga with explicit step state. The process coordinates local service calls but does not own ticket, analysis, routing, or knowledge tables.

**State model:**

- Workflow: `NEW`, `RUNNING`, `WAITING_RETRY`, `COMPLETED`, `COMPLETED_WITH_FALLBACK`, `MANUAL_ACTION_REQUIRED`, `FAILED`.
- Step: `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED_RETRYABLE`, `FAILED_TERMINAL`, `SKIPPED`.
- Attempt: number, provider/target, input hash, start/end, timeout, safe error code, output reference, trace ID.

**Implementation:**

1. On idempotent `TicketCreated`, create workflow and first pending step in a short transaction.
2. A worker atomically claims one due step and commits.
3. Worker calls analysis/routing/RAG/draft provider outside transaction with trace/correlation and explicit timeout.
4. Worker opens a short transaction to persist attempt result and schedule next step/retry/manual state.
5. Use persisted request/input hashes to make repeated attempts explainable.
6. Resume due workflow steps through scheduled polling or durable step-command events.
7. Distinguish deterministic fallback from successful AI results in state and UI.
8. Do not fabricate successful retrieval/classification after dependency failure.
9. Completion requires a persisted suggestion reference produced by RIQ-011.
10. Emit completion/failure only through the working outbox.

**Tests/gate:**

- [ ] Kill/restart after each step and verify resume without repeated completed effects.
- [ ] Dependency timeout moves to retry/manual state honestly.
- [ ] Configured URLs are non-null; tests use WireMock/MockWebServer rather than accidental `/null` calls.
- [ ] Workflow trace includes every attempt and downstream call.
- [ ] Completion cannot commit without an existing suggestion.

### RIQ-009 — Implement real and deterministic AI provider adapters

**Goal:** Main applications start and can use a real provider, while CI remains deterministic and offline.

**Required adapters:**

- `DeterministicEmbeddingAdapter` and `DeterministicChatAdapter`, active only in `test` and explicit `demo-fixture` profile.
- One real OpenAI-compatible embedding adapter.
- One real OpenAI-compatible chat adapter with structured JSON output support where available.
- Optional Ollama adapters for local no-cloud demos; use the same ports.

**Implementation:**

1. Use Spring `RestClient`/`WebClient` behind adapter classes. Vendor DTOs stay inside adapter packages.
2. Select adapters with validated configuration and `@ConditionalOnProperty`/profiles.
3. Fail startup when provider selection has zero or multiple beans.
4. Production-like profile rejects deterministic/mock provider and missing/default API key.
5. Validate embedding count/dimension/finite values and chat response size/schema envelope.
6. Apply connect/read/total timeout, retry only safe transient failures, circuit breaker, concurrency bulkhead, token/input limit, daily budget, and kill switch.
7. Record provider request ID, model, latency, token usage, estimated cost, retry count, and safe status; never log secret or full sensitive content.
8. Add controlled stub-server contract tests for success, timeout, `429`, `5xx`, malformed JSON, dimension mismatch, and oversized response.

**Gate:**

- [ ] Analysis and RAG contexts start in test and real-provider configuration validation tests.
- [ ] Opt-in smoke test proves one real chat and embedding call without making normal CI depend on external availability.
- [ ] Production-like startup fails with mock/default provider configuration.

### RIQ-010 — Build real embedding ingestion and governed index activation

**Goal:** Every active retrievable chunk has a stored, versioned vector generated once during ingestion.

**Pattern:** Asynchronous staged indexing plus atomic activation.

**Implementation:**

1. Add index-job and embedding-version tables with tenant, source/version, chunker version, provider/model, dimension, content hash, status, counts, timestamps, and error codes.
2. Add vector mapping. Use `pgvector-java` with JDBC/native persistence, or Hibernate Vector support if pinned and proven by integration tests. Do not serialize vectors as arbitrary text fields in entities.
3. Knowledge creation remains `DRAFT`; submission moves to review; authorized publish creates an immutable version and indexing job.
4. Deterministically chunk normalized text with section/offset metadata and content hashes.
5. Batch only changed/new chunks through `EmbeddingPort` outside database transactions.
6. Store embeddings, model/dimension/version, and chunks in a staging index version.
7. Validate dimensions, expected count, non-null vectors, and source ownership.
8. In one short transaction, activate the fully indexed version and retire the prior active pointer.
9. Failed/partial jobs never become active.
10. Retain previous active index for rollback and implement an authorized rollback action.
11. Apply the same pipeline to human-approved sanitized resolved cases.

**Tests/gate:**

- [ ] PostgreSQL integration test verifies vectors are non-null and dimension-correct.
- [ ] Querying never calls embed for stored chunks.
- [ ] Duplicate unchanged content reuses/skips work according to policy.
- [ ] Provider failure leaves old active version searchable.
- [ ] Activation and rollback are atomic and audited.

### RIQ-011 — Complete production hybrid retrieval and suggestion persistence

**Goal:** Retrieve bounded authorized evidence and persist the exact suggestion/citations before completing workflow.

**Implementation:**

1. Add `ts_rank_cd(...) DESC` plus deterministic tie-breaker to lexical SQL.
2. Run vector and lexical queries separately with bounded candidate pools.
3. Apply tenant, active index/version, ACL/team/customer visibility, product, language, source state, effective date, and embedding version in SQL before ranking.
4. Remove the catch-all SQL exception fallback from production. Test profile may use an explicit test repository; production retrieval failure must return a typed degraded/error result.
5. Never use unbounded `findByTenantId` as a production query fallback.
6. Fuse ranks through versioned/configured RRF. Persist component ranks/scores and retrieval parameters.
7. Resolve source title/version with tenant-safe repositories.
8. Persist citation records linked to retrieval run and later suggestion.
9. Define an orchestration-to-ticket command/event containing the complete suggestion draft/provenance and citation references.
10. Ticket-service validates ticket/tenant, creates the suggestion with the supplied ID idempotently, then emits `AiSuggestionCreated.v1` or returns a command acknowledgement.
11. Orchestration emits `TicketTriageCompleted.v1` only after suggestion persistence is confirmed.
12. Never create a random suggestion ID solely for an event.

**Tests/gate:**

- [ ] `EXPLAIN ANALYZE` demonstrates indexes and bounded candidate work on representative synthetic data.
- [ ] Active/ACL/product/language/cross-tenant filters pass PostgreSQL tests.
- [ ] Retrieval database failure produces degraded/manual behavior, not all-tenant scanning.
- [ ] Every completion event references an existing tenant/ticket suggestion.
- [ ] Every citation resolves to the immutable authorized source version used at generation time.

### RIQ-012 — Add strict AI schemas, safety validators, and human-only send

**Goal:** Treat model output as untrusted and prevent unsupported customer-visible responses.

**Implementation:**

1. Define versioned JSON schemas/Java DTOs for classification and draft output.
2. Reject unknown enums, missing required fields, out-of-range confidence, excessive lengths, invalid language, malformed citations, and non-finite numeric values.
3. Attempt one bounded schema repair if configured; otherwise create an explicit deterministic fallback with `validationOutcome=FALLBACK`, never `VALID`.
4. Implement pre-provider PII/secret redaction using tested detectors and tenant policy. Store only necessary redaction metadata.
5. Delimit trusted instructions, untrusted ticket content, and retrieved evidence structurally.
6. Add output validators for PII/secrets, unsafe links, unsupported refunds/account actions/deadlines, policy conflicts, citation existence/coverage, and evidence support.
7. Generate drafts with citation IDs, not copied free-form citation JSON.
8. Compute confidence from documented components; name it `systemConfidence`, not model certainty.
9. Low/contradictory/no evidence produces an abstention/manual-assistance suggestion.
10. Persist prompt/model/provider/parameters/token/cost/latency/retrieval/citations/validator versions and outcomes.
11. Implement immutable suggestion versions and states. Editing creates final content/history rather than overwriting provenance.
12. Invalidate suggestions on relevant ticket/source changes. Regeneration creates a new version with reason.
13. Implement authorized, optimistic-lock-protected review. Approval and send must be explicit idempotent human commands.
14. Enforce in architecture/code tests that no provider/orchestration/event consumer directly invokes customer send.

**Tests/gate:**

- [ ] Malformed/hostile/out-of-range model outputs never become valid suggestions.
- [ ] PII, prompt-injection, unsupported-action, conflicting-evidence, and missing-citation suites pass.
- [ ] No-evidence case abstains.
- [ ] Concurrent review produces one accepted state.
- [ ] Duplicate send creates exactly one public message.
- [ ] Static dependency and E2E tests prove human-only send.

### RIQ-013 — Rebuild the frontend shell around real authentication and server state

**Goal:** Remove the role-switching demo path and make every visible action/data point real.

**Implementation:**

1. Mount `AuthProvider`, `QueryClientProvider`, `BrowserRouter`, error boundary, and notification provider in `main.tsx`.
2. Add `/login`, `/register`, `/forgot-password`, and `/reset-password` pages.
3. On bootstrap, call cookie refresh or `/auth/me`; show a bounded session-loading state.
4. Add protected route components based on authenticated roles. Role choice may select among roles actually held by the user but cannot grant a role.
5. Remove the fictional default user and production role dropdown.
6. Remove access-token/local identity storage. Keep non-sensitive UI preferences only.
7. Convert every page to React Query hooks over the typed client. Add query keys by tenant-safe resource identity; invalidate after commands.
8. Implement the complete route list from WP-6, including lead, evaluation, audit, and operations pages.
9. Replace `alert()` with accessible inline/toast/dialog feedback and real mutation results.
10. Remove hardcoded ticket, citation, provider, metric, workflow, and governance rows. If fixture mode remains, require explicit development configuration and visible `DEMO FIXTURE` labeling.
11. Implement loading, empty, validation, `401/403/404/409/429/5xx`, offline/degraded AI, optimistic conflict, retry, and cancellation behavior.
12. Ensure responsive and keyboard/screen-reader behavior described by the blueprint.
13. Generate/check client types against OpenAPI in CI.

**Tests/gate:**

- [ ] Component tests cover auth bootstrap, protected roles, forms, query/mutation states, and problem details.
- [ ] No source search finds hardcoded production user/model/metric data or browser `alert()`.
- [ ] No bearer/refresh token appears in local/session storage.
- [ ] Desktop/mobile Playwright journeys from WP-6 pass through gateway against real services.
- [ ] Axe finds no critical violations; manual keyboard smoke record exists.

### RIQ-014 — Replace the evaluation script with an application-backed harness

**Goal:** Every reported metric is reproducible and measured against the same code path used by the application.

**Implementation:**

1. Split reviewed synthetic cases into development and frozen test sets. Keep final test labels stable.
2. Extend cases with tenant, ACL, language, active/inactive version, expected source/version, expected abstention, and safety expectations.
3. Seed the dataset through supported knowledge APIs/index jobs or a versioned test seeder that produces identical database state.
4. Call the authenticated retrieval/evaluation API through gateway, or call a shared application adapter that invokes the real PostgreSQL queries and configured deterministic embedding adapter. Do not duplicate retrieval logic in Python.
5. Remove `build_mock_embedding`, Python `hash()`, in-memory RRF duplication, and hardcoded success values.
6. Persist per-case retrieved ranks/source versions/scores, latency, error, filter result, citation result, and safety outcomes.
7. Calculate Recall@K, MRR, optional nDCG, zero-result, filter leakage, p50/p95/p99, schema validity, citation coverage, groundedness, abstention, PII leakage, unsafe action, and autonomous-send invariant from observations.
8. If a metric was not executed, report `NOT MEASURED`; never substitute zero.
9. Report failure when required cases error or gates fail. Exit nonzero for gated CI evaluation.
10. Record dataset version/hash, application commit, provider/model, embedding/chunking/index versions, RRF parameters, prompt versions, seed, environment, and time.
11. Use stable hashing such as SHA-256 for deterministic fixtures.
12. Add baseline/candidate comparison with statistically honest sample-size warnings and rollback decision.

**Tests/gate:**

- [ ] Two deterministic runs on identical commit/data produce identical ranks/metrics.
- [ ] Deliberately broken retrieval lowers metrics and exits nonzero.
- [ ] Deliberate cross-tenant result makes leakage gate fail.
- [ ] PII fixture makes leakage test fail when validator is disabled.
- [ ] Report contains no unmeasured numeric claim.
- [ ] Measured latency includes application/database path and states environment/data size.

### RIQ-015 — Build complete integration, contract, E2E, accessibility, and failure suites

**Goal:** Turn every important plan invariant into an executable gate.

**Implementation order:**

1. PostgreSQL/Flyway repository and pgvector tests.
2. Gateway/service JWT and authorization matrix.
3. Ticket state/SLA/idempotency/attachment integration tests.
4. Kafka outbox/redelivery/retry/DLQ/replay tests.
5. Workflow restart/provider failure tests.
6. Provider stub contract tests.
7. OpenAPI and event compatibility tests.
8. Frontend component/MSW tests.
9. Full-stack Playwright and axe tests.
10. Load and failure/chaos tests.

Create CI jobs with clear names and preserve artifacts/logs on failure. Use deterministic provider adapters in normal CI and an opt-in external-provider smoke workflow. Do not make PR success depend on a paid provider.

**Minimum required test count is behavior-driven, not numeric.** Every matrix row and failure scenario in this plan must have at least one test. Twelve backend tests and one frontend smoke test are not sufficient.

**Gate:** Section 17 test architecture and the exact acceptance journey both pass in CI.

### RIQ-016 — Create a real full-stack Compose environment

**Goal:** One documented command starts infrastructure, all services, gateway, and frontend from a clean clone.

**Implementation:**

1. Keep an `infra` profile for PostgreSQL, Kafka, MinIO, and optional observability.
2. Add discovery, auth, ticket, analysis, routing, RAG, orchestration, gateway, and frontend services using the checked-in Dockerfiles.
3. Use module-specific build args/targets and copy only the correct executable JAR.
4. Run containers as non-root with read-only filesystem where practical, temp mounts, resource limits, and health checks.
5. Add service health/readiness endpoints that test local readiness without exposing secrets.
6. Use Compose health dependencies where useful, while application clients still handle dependency loss after startup.
7. Use internal networks. Expose frontend/gateway plus explicitly local-only consoles; do not publish backend/database/broker ports in the portfolio staging profile.
8. Move local credentials into `.env.example` interpolation. Mark them fictional and reject them in production profile.
9. Add deterministic seed/migration service and explicit demo reset command.
10. Document startup, readiness verification, logs, shutdown preserving data, and explicit destructive teardown.

**Tests/gate:**

- [ ] `docker compose --profile app up --build` starts a healthy full stack.
- [ ] Gateway health and each internal readiness check pass.
- [ ] Exact E2E journey runs without manual database edits.
- [ ] Stopping/restarting service containers preserves/resumes workflow correctly.
- [ ] Clean-clone CI smoke validates the Compose path.

### RIQ-017 — Complete observability, operations, backups, and security supply chain

**Goal:** Demonstrate diagnosis and recovery rather than only shipping monitoring containers.

**Implementation:**

1. Add Micrometer/OpenTelemetry dependencies and configuration to every service.
2. Instrument HTTP, Kafka, DB, workflow step, retrieval, indexing, provider, suggestion review/send, and security audit boundaries.
3. Propagate W3C trace context and correlation IDs across HTTP and Kafka.
4. Add structured production logging and central redaction tests.
5. Provision all Grafana dashboards and Prometheus alert rules through mounted provisioning paths; verify the existing dashboard is actually loaded.
6. Implement metrics listed in WP-8 with bounded labels.
7. Create alert tests and link each alert to a corrected executable runbook.
8. Implement actual PostgreSQL/MinIO backup scripts with checksums, encryption/access handling, retention, and failure exit codes.
9. Restore into an isolated environment, run integrity/E2E checks, and record RPO/RTO evidence.
10. Add dependency review, CodeQL/SAST, secret scanner, image build/scan, SBOM, and artifact provenance/signing as appropriate for the repository.
11. Add synthetic load and dependency-failure suites; record environment, versions, data size, p50/p95/p99, throughput, error/resource results.
12. Update threat model and runbooks to match actual controls rather than intended controls.

**Gate:** All WP-8 acceptance items pass with checked-in configuration and dated evidence reports.

### RIQ-018 — Deploy and produce the evidence-backed portfolio package

**Goal:** Deliver a safe recruiter/interviewer experience with no false claims.

**Implementation:**

1. Deploy the verified images to a small staging platform over HTTPS.
2. Expose only frontend/gateway. Restrict infrastructure, services, actuator details, and dashboards.
3. Configure managed secrets, restrictive CORS, rate limits, synthetic demo accounts, data reset, AI daily budget, provider kill switch, and abuse monitoring.
4. Run post-deploy security and exact E2E smoke tests.
5. Seed the coherent duplicate-charge story defined in WP-9.
6. Capture architecture and request/event sequence diagrams from the final design.
7. Capture screenshots only after removing fixture labels and hardcoded data.
8. Run and check in final application-backed evaluation, load, failure, security, and restore summaries.
9. Record a three-to-five-minute video showing customer creation, durable workflow, real retrieval/citations, human edit/send, customer response, governance, and one failure recovery.
10. Update README and resume statement with only the measured final results.
11. Document costs, limits, deferred Kubernetes/state-changing tools, and future work honestly.

**Gate:** WP-9 and the final release decision pass.

---

## 26. Cross-cutting patterns the implementation agent must follow

### 26.1 Hexagonal boundaries

- Domain/application code depends on ports, not Kafka, HTTP vendor DTOs, MinIO, or model SDKs.
- Inbound adapters translate HTTP/events into application commands.
- Outbound adapters implement repositories, providers, Kafka, object storage, and telemetry.
- Do not create interfaces for trivial internal helpers; use ports at real external/test boundaries.

### 26.2 Transaction pattern

- Transaction: validate current database state, mutate owned records, write outbox, commit.
- No transaction: HTTP/provider/object-store/Kafka wait.
- Follow-up transaction: persist external result/attempt and schedule next state.
- Never solve distributed consistency with cross-service database access or one distributed transaction.

### 26.3 Idempotent command/consumer pattern

- Identify operation by stable command/event ID.
- Scope and hash the input.
- Claim under unique constraint.
- Perform one local business effect.
- Store outcome atomically.
- Redelivery/retry returns prior outcome or a safe no-op.

### 26.4 Tenant-safe repository pattern

- Every tenant-owned lookup includes `tenantId` in SQL/repository method.
- Customer-owned lookup additionally includes `customerId`.
- Agent lookup includes tenant plus validated team/assignment policy.
- Do not load by ID and authorize afterward if a single scoped query can prevent leakage.
- Cross-tenant access should normally appear as not found and never reveal metadata.

### 26.5 AI safety pattern

- Sanitize/minimize before provider.
- Delimit untrusted input.
- Require structured output.
- Parse and validate strictly.
- Retrieve authorized evidence.
- Validate claims/citations/policy/PII.
- Abstain when uncertain.
- Persist provenance.
- Require human action for send/mutation.
- Measure behavior on a frozen dataset.

### 26.6 Failure pattern

- Use stable safe error codes; preserve full safe diagnostic internally.
- Classify retryable versus terminal.
- Bound retries and queue growth.
- Expose manual fallback.
- Emit metrics/traces/audit.
- Test the failure intentionally.

### 26.7 UI data pattern

- Server state through React Query.
- Form state through React Hook Form/schema validation.
- Auth from secure session context.
- Role checks affect visibility but never replace backend authorization.
- Every mutation has pending/disabled/success/conflict/error behavior.
- No hardcoded production metric or user identity.

---

## 27. Final zero-remaining-work verification protocol

An agent must not declare the project complete until it performs this protocol in order and records the result.

### 27.1 Static/repository checks

```bash
git status --short
./scripts/scan-secrets.sh
./mvnw clean verify
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run test
npm --prefix frontend run build
docker compose config --quiet
```

Additionally prove:

- exactly one frontend lockfile;
- no main-source mock/default provider in production profile;
- no hardcoded JWT/provider secret fallback in production;
- no `AtomicLong` ticket numbering;
- no unbounded RAG tenant fallback;
- no remote call inside `@Transactional` workflow method;
- no random suggestion ID without persistence;
- no public controller trusts tenant/user headers as identity;
- no local-storage token;
- no hardcoded UI metrics/users/model results;
- no evaluation hardcoded pass/zero metrics;
- no public application endpoint bypasses gateway/service authentication.

### 27.2 Integration environment

1. Start Docker.
2. Build/start full application profile from clean data.
3. Wait for health gates; do not use arbitrary sleeps.
4. Run Flyway/schema verification.
5. Seed fictional demo/evaluation data through supported path.
6. Run backend integration/contract suites.
7. Run frontend component suite.
8. Run Playwright/axe suite.
9. Run application-backed evaluation.
10. Run load and failure suites.
11. Run backup and isolated restore drill.
12. Run container/security/SBOM gates.

### 27.3 Required proof artifacts

- Unit/integration/contract/E2E reports.
- Coverage reports with enforced thresholds.
- OpenAPI and event-contract compatibility report.
- Per-case evaluation results and aggregate report.
- Load/failure report with environment/data size.
- Security scan and accepted-risk record.
- Backup/restore drill report.
- Architecture and sequence diagrams.
- Desktop/mobile accessibility report/screenshots.
- Demo deployment smoke result and video.

### 27.4 Final manual audit questions

Every answer must be **yes** with evidence:

1. Can a clean clone start the complete system with documented commands?
2. Can each service start with validated runtime dependencies?
3. Can a customer complete manual support when AI/Kafka is degraded?
4. Can no user cross tenant/customer/team/role boundaries?
5. Can a broker outage recover without loss or duplicate business effect?
6. Can a workflow resume after restart at every step?
7. Are stored vectors real, versioned, filtered, and queried through pgvector?
8. Does every cited suggestion point to immutable authorized evidence?
9. Does weak evidence abstain?
10. Is an authenticated human the only customer-send path?
11. Does every displayed metric come from measured persisted data?
12. Are evaluation results reproducible and application-backed?
13. Can an operator trace, alert, diagnose, retry/replay, back up, and restore safely?
14. Does the deployed demo use only synthetic data, HTTPS, limits, and budget controls?
15. Does every README/resume claim match a runnable feature or checked-in report?

If any answer is no or unverified, the project is not complete. Create a bounded issue using the completion-report template and continue in dependency order.
