# ResolveIQ Part 1 Completion Plan

> **Status:** Implemented and acceptance-verified on 2026-08-31
> **Scope:** All correctness, credibility, and parity gaps listed in Part 1 of `RESOLVEIQ_BEYOND_PARITY_ROADMAP.md`
> **Delivery rule:** Finish and prove the P0 vertical slices before claiming the P1 parity capability
> **Architecture rule:** Extend the current owning services; do not create another microservice for a screen or a small capability

## 1. Outcome

Part 1 is complete only when ResolveIQ is an honest, role-complete support system in which:

1. a customer can create a ticket, search approved knowledge, and securely exchange attachments;
2. an agent can select any authorized ticket from a paginated queue and see only persisted ticket, SLA, classification, suggestion, citation, and similar-case data;
3. a team lead can operate a team queue, assign tickets, and manage SLA risk;
4. a knowledge manager can move an article through draft, review, publish, supersede, and rollback states;
5. an administrator can operate users, routing, workflows, outbox health, and governance using persisted APIs;
6. an auditor has a distinct, read-only audit journey;
7. AI input and output pass composable guardrails, budgets, provider abstraction, and resilience boundaries;
8. published knowledge is indexed through application code and natural-language retrieval uses metadata-aware lexical and vector candidates;
9. important ticket and workflow changes reach an open browser without a full-page reload; and
10. automated and reproducible evidence proves the principal journeys.

No UI may silently substitute a fictional customer, team, SLA, score, citation, case, metric, workflow, model invocation, or ticket when an API returns no data. Empty, loading, unavailable, and permission-denied states are first-class states.

## 2. Baseline audit and gap mapping

| Roadmap gap | Current baseline | Required Part 1 result |
|---|---|---|
| Seeded hybrid search | Deterministic embedding repair and relaxed lexical fallback are implemented locally | Seed via an authenticated ingestion command, use `websearch_to_tsquery`, retain vector candidates, and keep the documented long-query regression test |
| Hardcoded UI panels | Agent, knowledge, and governance screens contain fictional constants | Render API data or an explicit empty/unavailable state; add a static-value regression test |
| First-ticket-only workspace | Ticket APIs have simple limited lists; UI chooses index zero | Paginated selectable mine/team/SLA queues with filtering, sorting, authorization, and assignment |
| Static administrator screens | Workflow retry is real; metrics, DLQ row, invocation rows, and routing content are static | Persisted workflow/outbox/governance/routing/user APIs with real mutations and readback |
| Incomplete role experiences | Customer, agent, knowledge manager, and admin shells exist; team lead and auditor fall through | Distinct authorized navigation and pages for all six roles |
| Knowledge lifecycle | Creating an article immediately publishes and embeds it | Explicit `DRAFT -> IN_REVIEW -> PUBLISHED -> SUPERSEDED`; reject/rollback/archive operations; retrieval sees only the active approved version |
| Attachments | Database table and MinIO container exist | Validated multipart upload, malware scan, object ownership, clean-only download, audit metadata, and customer/agent UI |
| Automated coverage | Unit tests and CI basics exist | Repository/service tests, PostgreSQL/Kafka Testcontainers suites, frontend interaction tests, and browser E2E smoke journey |
| Seed ingestion bypass | SQL creates chunks directly | Idempotent seed command calls the knowledge lifecycle/ingestion API and verifies every active chunk has an embedding |
| Strict search semantics | Strict `plainto_tsquery` plus OR fallback | `websearch_to_tsquery` primary lexical search, relaxed fallback, query normalization/rewrite, vector candidates, and metadata filters |
| Provider integration | Deterministic and direct OpenAI-compatible adapters exist | A provider-neutral port with OpenAI-compatible and Gemini modes, production credential guards, usage metadata, and deterministic offline mode |
| Guardrails and budgets | Ad-hoc string replacement and JSON validation exist | Ordered input/output guardrail pipeline, PII/secret redaction, injection findings, token/cost limits, persisted outcomes, and adversarial tests |
| Real-time updates | Browser reload/polling only | Authenticated SSE stream backed by ticket-domain events and reconnect handling |
| Resilience | Timeouts and dependencies are present; Resilience4j is not applied end to end | Circuit breaker, retry, time limiter/bulkhead policies around remote AI/routing/RAG calls with observable failure state |
| OpenAPI and deployment | No served API contract; Kubernetes intentionally deferred | Springdoc contracts for owning APIs plus an honest Kustomize base for application workloads and managed infrastructure dependencies |
| Portfolio proof | ADRs and runbooks exist | Updated architecture diagrams, screenshot checklist, demo script, and generated evidence index; video recording remains an owner-run artifact |

## 3. Architecture and ownership

### 3.1 Service ownership

| Capability | Owning service | Reason |
|---|---|---|
| Ticket queue, assignment projection, messages, attachments, SSE ticket updates | `ticket-service` | Ticket aggregate and authorization boundary |
| User directory, staff creation, role/status changes, security audit | `auth-service` | Identity and role source of truth |
| Teams, agents, routing rules, SLA policies | `routing-service` | Routing configuration source of truth |
| Workflow attempts, failed workflow replay, orchestration outbox health | `ai-orchestration-service` | Workflow state owner |
| Analysis guardrails, provider usage, model invocation audit | `ai-analysis-service` | Model invocation owner |
| Knowledge lifecycle, ingestion, embeddings, retrieval, citations | `rag-service` | Knowledge and retrieval owner |
| Cross-service navigation and view composition | React frontend | UI composes authorized APIs; no cross-schema database reads |

The frontend may combine responses from multiple APIs, but a mutation is sent only to its owning service. Service databases remain private. No administrator endpoint may directly update another service's schema.

### 3.2 Security invariants

- The gateway removes client-supplied identity headers and derives trusted identity from a validated access JWT.
- Owning services validate the same JWT again and enforce method/domain authorization.
- Every lookup and mutation includes `tenant_id`; customer ticket and attachment operations also include `customer_id` ownership.
- Agents may open only assigned tickets. Team leads and administrators may open team/tenant queues; auditors are read-only.
- Only knowledge managers and administrators may review or publish knowledge.
- Only clean attachments are downloadable. Storage keys are generated server-side and never accepted from a client.
- Model text never authorizes a role, ticket access, knowledge publication, workflow replay, or attachment download.
- SSE streams are authenticated with the normal bearer token and scoped to the authenticated tenant/user.

### 3.3 Failure and consistency model

- Ticket, lifecycle, attachment metadata, and outbox writes use short local transactions.
- Object upload uses: validate -> scan -> store object -> persist metadata. If metadata persistence fails, delete the just-created object best-effort and emit an operational error.
- Knowledge publication uses: approve state -> chunk -> embed -> atomically activate after all chunks are valid. Failed indexing leaves the former active version unchanged.
- Kafka remains at-least-once. Consumers and replay commands remain idempotent.
- SSE is a convenience projection, not the source of truth. Reconnect always refreshes the queue via REST.
- Remote dependency failures are persisted as workflow failures; there is no silent fabricated response.

## 4. Data model changes

### 4.1 Ticket and queue projection

Add to `ticket_schema.tickets`:

- `intent VARCHAR(100)`
- `sentiment VARCHAR(30)`
- `urgency VARCHAR(30)`
- `triage_confidence DOUBLE PRECISION`

Populate these from `TicketTriageCompleted`. Do not store customer or team display names in the ticket schema; resolve them through authorized directory/routing APIs.

Extend `ticket_attachments` with:

- `sha256 VARCHAR(64)`
- `scan_engine VARCHAR(100)`
- `scan_details VARCHAR(500)`
- `scanned_at TIMESTAMPTZ`

Add tenant/ticket/status indexes. Existing attachment rows are treated as unavailable until a clean scan result exists.

### 4.2 Knowledge lifecycle

Add to `knowledge_versions`:

- `status`: `DRAFT`, `IN_REVIEW`, `PUBLISHED`, `SUPERSEDED`, `REJECTED`
- `created_by_user_id`
- `submitted_at`
- `reviewed_by_user_id`
- `reviewed_at`
- `review_note`
- `superseded_at`

Document status is derived as `DRAFT`, `IN_REVIEW`, `PUBLISHED`, or `ARCHIVED`. `active_version_id` changes only after indexing succeeds. A unique `(document_id, version_number)` constraint prevents version races.

### 4.3 AI governance

Add to analysis results:

- `input_tokens`, `output_tokens`
- `estimated_cost_micros`
- `guardrail_outcome`
- `guardrail_findings JSONB`
- `provider_request_id`

Only hashes and sanitized content are persisted; raw provider output remains excluded.

## 5. API contracts

### 5.1 Agent queues and ticket context

`GET /api/v1/agent/tickets/queue`

Query parameters:

- `scope=mine|team|all|sla-risk`
- `teamId` when authorized and required
- `status`, `priority`, `query`
- `sort=createdAt|updatedAt|priority|firstResponseDueAt|resolutionDueAt`
- `direction=asc|desc`
- `page` and `size` with maximum `100`

Response:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`GET /api/v1/agent/tickets/{id}/context` returns the authorized ticket, messages, latest suggestion, structured citations, and attachment metadata. Customer and routing display names are resolved separately so service ownership is preserved.

`POST /api/v1/agent/tickets/{id}/assign` is restricted to team lead/admin. A reassignment is persisted and emitted through the ticket outbox.

### 5.2 Directory, routing, and administration

- `GET /api/v1/directory/users/{id}`: tenant-scoped staff lookup needed to render a ticket.
- `GET /api/v1/admin/users`: paginated tenant staff list.
- `POST /api/v1/auth/users`: existing admin creation command, surfaced in UI.
- `PATCH /api/v1/admin/users/{id}/roles`: validated role update with security audit.
- `GET /api/v1/routing/teams`, `GET /api/v1/routing/agents`, `GET /api/v1/routing/rules`, `GET /api/v1/routing/sla-policies`.
- Existing create endpoints remain admin-only; rule activation changes require an audited command.
- `GET /api/v1/admin/operations/outbox`: counts pending, published, retrying, and exhausted events.
- `GET /api/v1/workflows/failed`: real failed workflows.
- `POST /api/v1/workflows/{id}/retry`: existing audited replay with readback.
- `GET /api/v1/analysis/invocations`: paginated sanitized model invocation metadata.
- `GET /api/v1/analysis/governance/summary`: computed validation, guardrail, token, cost, and latency aggregates.

### 5.3 Knowledge lifecycle

- `POST /api/v1/knowledge/articles`: create document and version 1 as `DRAFT`; do not index.
- `POST /api/v1/knowledge/articles/{id}/versions`: create the next draft version.
- `GET /api/v1/knowledge/articles/{id}/versions`: version history.
- `POST /api/v1/knowledge/articles/{id}/versions/{versionId}/submit`: move draft to review.
- `POST /api/v1/knowledge/articles/{id}/versions/{versionId}/reject`: review note required.
- `POST /api/v1/knowledge/articles/{id}/versions/{versionId}/publish`: index then activate.
- `POST /api/v1/knowledge/articles/{id}/rollback/{versionId}`: activate a previously published/superseded indexed version and supersede the current version.
- `POST /api/v1/knowledge/articles/{id}/archive`: remove document from retrieval without deleting history.
- `POST /api/v1/knowledge/admin/reindex-missing`: idempotently repair active versions with missing/wrong-dimension embeddings.

The old `POST .../{id}/publish` content-creation shortcut is removed from the UI and retained only as a documented compatibility endpoint if existing automated clients require it.

### 5.4 Attachments

- `POST /api/v1/customer/tickets/{id}/attachments` multipart field `file`.
- `GET /api/v1/customer/tickets/{id}/attachments`.
- `GET /api/v1/customer/tickets/{id}/attachments/{attachmentId}/content`.
- Equivalent agent endpoints, with internal attachment metadata reserved for staff.

Limits:

- 10 MiB per file and 5 files per ticket in this portfolio release;
- allow PDF, PNG, JPEG, plain text, and JSON;
- verify magic bytes for PDF/images and reject executable/archive formats;
- normalize the original display name, generate a storage key, compute SHA-256, scan, and store only a clean object;
- return `422` for infected content, `415` for unsupported content, `413` for size/count, `404` for unauthorized ownership to avoid enumeration.

### 5.5 Retrieval

Extend search input with optional `category`, `product`, `language`, and `sourceTypes`. The query path is:

```text
validate and budget query
-> deterministic normalization/query rewrite
-> metadata-scoped web-search lexical candidates
-> relaxed lexical candidates when primary recall is empty
-> metadata-scoped vector candidates
-> reciprocal-rank fusion
-> persist retrieval run and citations
```

`websearch_to_tsquery` is the primary parser because it accepts natural search syntax safely. The relaxed fallback converts normalized lexemes to OR semantics. SQL always applies tenant, published document, active version, and metadata predicates before rank/distance ordering.

### 5.6 Real-time stream

`GET /api/v1/agent/tickets/stream` uses `text/event-stream`. Events contain only IDs and safe queue projection fields:

- `ticket.created`
- `ticket.updated`
- `ticket.assigned`
- `ticket.triage.completed`
- `ticket.message.added`

The frontend consumes it with authenticated `fetch`, reconnects with exponential backoff, and refreshes the active queue after each event. It never treats an SSE payload as authoritative for access control.

## 6. UI and UX specification

### 6.1 Shared states

Every API-backed panel implements:

- skeleton/loading state;
- empty state with the next valid action;
- permission-denied state;
- dependency-unavailable state with retry;
- success confirmation for mutations;
- destructive or high-impact confirmation where appropriate.

Use the existing navy/slate visual system. Persisted status uses semantic tokens: blue for informational, green for successful/clean/published, amber for at-risk/review/pending, and red for failure/infected/breached. Do not introduce a second color system.

### 6.2 Agent workspace

The page becomes four functional regions:

1. queue toolbar: mine/team/SLA scope, search, status, priority, sort, pagination;
2. selectable ticket list: number, subject, priority, status, SLA clock, assignee;
3. conversation/composer: real message history, attachment list/upload, suggestion draft, explicit approval;
4. evidence panel: actual confidence, citations, resolved-case citations, provider/prompt metadata, immutable feedback state.

If a value is absent, display `Not assigned`, `No SLA`, `No classification`, `No grounded citations`, or `No similar resolved case`; never a fixture.

### 6.3 Team lead

Team Lead opens on Team Queue and can:

- filter by team and SLA risk;
- select a ticket;
- assign/reassign team and agent;
- inspect workload and deadline context;
- reply/review suggestions within the team scope.

### 6.4 Knowledge manager

Knowledge Console shows persisted article cards/table and a version timeline. Authoring uses a real form. Buttons are state-specific:

- Draft: `Edit`, `Submit for review`;
- In review: `Publish`, `Reject`;
- Published: `Create new version`, `Archive`;
- Superseded: `Rollback`.

The UI displays indexed chunk count/model only from API data. Search remains available as a separate verification tab.

### 6.5 Administrator

Administrator navigation maps to distinct API-backed views:

- Overview: live counts and dependency status;
- Tickets: all-scope queue;
- Teams & Routing: teams, agents, rules, SLA policies and create commands;
- Knowledge: same lifecycle with administrator permission;
- Governance: analysis aggregates/invocations plus failed workflows and retry;
- Settings/Users: staff list and create-user form.

### 6.6 Auditor

Auditor receives a read-only Audit Explorer: security events, model invocations, workflow failures/attempts, and ticket state metadata. No mutation control is rendered, and backend authorization rejects mutation requests even if a URL is called manually.

## 7. AI, guardrail, retrieval, and resilience design

### 7.1 Provider modes

- `deterministic`: reproducible offline development and CI; forbidden in production.
- `openai-compatible`: OpenAI-compatible chat/embedding endpoints.
- `gemini`: Gemini REST adapter with the same port and normalized usage metadata.

Provider adapters return content plus model, provider request ID, input tokens, output tokens, and estimated cost. Credentials stay in environment/secret stores and never appear in DTOs, logs, exceptions, or persisted payloads.

### 7.2 Composable guardrails

Input guardrails execute in order:

1. size/token budget;
2. Unicode/control-character normalization;
3. PII and secret detection/redaction;
4. prompt-injection detection and policy flags;
5. safe untrusted-data envelope construction.

Output guardrails execute in order:

1. maximum output size/token budget;
2. strict JSON parse/schema/enums/ranges;
3. PII/secret leakage scan;
4. unsupported instruction/action scan;
5. confidence/citation policy validation.

A finding is either `INFO`, `REDACTED`, or `BLOCKED`. Prompt-injection language in a support ticket is classified as untrusted content and flagged; it cannot change the system prompt or trigger a business action. Block only malformed/oversized or secret-exfiltration requests.

### 7.3 Budgets

Configuration defines per-call maximum input tokens, output tokens, and estimated micro-dollar cost. The application checks the input budget before provider invocation and the output/cost budget before accepting output. Budget breaches create a validated abstention/failure outcome and are visible in governance metrics.

### 7.4 Resilience policies

Apply named Resilience4j policies to analysis, routing, and RAG clients:

- connect timeout 3 seconds, read timeout 5 seconds;
- circuit breaker based on a minimum call window;
- maximum two retries only for connection/timeout/5xx failures;
- no retry for validation, authentication, authorization, or budget failures;
- per-dependency semaphore bulkhead;
- metrics exported through Actuator/Micrometer.

Exhaustion fails the workflow and emits the existing triage-failed event. It must not fabricate an AI response, route, citation, or success status.

## 8. Delivery phases and gates

### Phase 1 — Retrieval correctness and ingestion

- Complete current deterministic embedding repair.
- Change primary lexical SQL to `websearch_to_tsquery`.
- Add metadata filters and deterministic query normalization.
- Add reindex/ingestion command and make seed call it instead of inserting chunks.
- Add long-query, metadata-isolation, active-version-only, and missing-vector tests.

**Gate:** the documented customer query returns the expected active article, unpublished versions never return, and every active seeded chunk has the configured embedding.

### Phase 2 — Ticket projection, queues, and real agent context

- Persist triage fields.
- Add pageable/specification queue query and context response.
- Enforce mine/team/all/SLA authorization.
- Add directory/routing read endpoints.
- Replace fictional agent data and implement selectable queues.
- Add authenticated SSE and queue refresh.

**Gate:** an agent can select any assigned ticket; a team lead can filter/assign team tickets; a customer/foreign agent cannot open the UUID; no fictional constant remains.

### Phase 3 — Knowledge lifecycle

- Apply lifecycle migration/state machine.
- Implement draft/version/review/reject/publish/rollback/archive APIs.
- Index before atomic activation.
- Replace Knowledge Console fixtures with real forms/history/actions.

**Gate:** only the approved active version retrieves; rollback changes search results without deleting history; a failed embedding leaves the former active version available.

### Phase 4 — Attachments

- Add entity/repository/storage/scanner ports and MinIO/scan adapters.
- Add ownership-checked upload/list/download endpoints.
- Add customer and agent attachment UI.
- Add validation, EICAR, ownership, cleanup, and object-storage integration tests.

**Gate:** clean allowed content round-trips; infected/unsupported/cross-owner content cannot be stored or read.

### Phase 5 — Admin and complete roles

- Add real outbox/governance/routing/directory APIs.
- Implement Team Lead and Auditor navigation/pages.
- Replace administrator fixtures and connect mutations.
- Add auditor mutation-denial tests.

**Gate:** every seeded role has a distinct reproducible UI journey and every displayed row comes from an API.

### Phase 6 — AI parity and production interfaces

- Introduce structured provider response and Gemini mode.
- Implement guardrail pipeline, budgets, usage persistence, and adversarial tests.
- Apply Resilience4j client policies.
- Add OpenAPI endpoints and examples.
- Add Kustomize application manifests, health probes, HPA/PDB, ingress, and network policies.

**Gate:** provider/guardrail/budget outcomes are visible and tested; dependency failure opens the circuit and persists failure; OpenAPI contracts render; Kubernetes manifests build without embedding secrets.

### Phase 7 — Automated proof and portfolio evidence

- Add PostgreSQL/pgvector and Kafka Testcontainers suites.
- Add frontend queue/lifecycle/admin/role interaction tests.
- Add Playwright customer-to-agent and knowledge lifecycle smoke tests.
- Update architecture diagrams, UI guide, screenshot checklist, demo script, and evidence index.

**Gate:** the verification matrix below is green and documentation reports only measured behavior.

## 9. Verification matrix

| Area | Required proof |
|---|---|
| Java unit/service | `./mvnw test` on Java 21 |
| Java full verification | `./mvnw clean verify` on Java 21 |
| Frontend | lint, Vitest, typecheck, production build |
| Migrations | clean pgvector PostgreSQL startup plus upgrade from current V1/V2 schemas |
| Kafka | duplicate delivery, redelivery, exhausted retry, and replay tests |
| Object storage | MinIO upload/download/cleanup plus malware rejection |
| Retrieval | long query, metadata, tenant isolation, active version, rollback, vector-missing repair |
| Security | customer/agent/team lead/admin/auditor authorization matrix and attachment IDOR |
| Browser | customer create/upload -> triage -> agent select/evidence/send -> customer read; knowledge draft -> review -> publish -> retrieve -> rollback |
| Compose | `docker compose config --quiet` and all application health checks |
| Kubernetes | `kubectl kustomize infra/k8s/overlays/local` or equivalent schema validation |
| Secrets | `./scripts/scan-secrets.sh` and no credential in generated client bundles |

## 10. Rollout, compatibility, and rollback

- All schema changes are additive before code switches behavior.
- Queue API is added alongside the current list endpoint; the frontend migrates first, then the legacy endpoint may be deprecated.
- Existing published knowledge is backfilled as `PUBLISHED`; its active version remains available during lifecycle migration.
- The compatibility publish endpoint may remain temporarily, but must internally use the lifecycle service.
- Attachment upload is feature-flagged by `resolveiq.attachments.enabled`; disabling it hides UI controls and returns `503` for upload while preserving existing clean downloads.
- SSE failure falls back to manual refresh; it never blocks core ticket operations.
- Provider modes are configuration-selected. Deterministic mode remains the rollback for local/test only; production startup rejects it.
- Kubernetes deployment is optional for local development; Compose remains the primary portfolio run path.

## 11. Definition of done

Part 1 is done only when all of the following are true:

- [x] No production screen contains a fictional business value.
- [x] Search and seed ingestion gates pass.
- [x] Agent and team queues are selectable, pageable, filterable, sortable, assignable, and authorized.
- [x] Admin actions mutate and read back persisted state.
- [x] Customer, Agent, Team Lead, Knowledge Manager, Admin, and Auditor have distinct working journeys.
- [x] Knowledge draft/review/publish/supersede/rollback is proven.
- [x] Clean attachments round-trip through MinIO and unsafe/foreign files are rejected.
- [x] Guardrails, budgets, provider metadata, query rewrite, real-time updates, and resilience policies are proven.
- [x] OpenAPI and Kubernetes artifacts validate.
- [x] CI contains backend, frontend, integration, E2E, security, and evaluation gates.
- [x] Architecture/demo documentation matches the measured repository state.

Part 2 feature work must not begin while a required Part 1 gate is red.

## 12. Implementation and acceptance record

Part 1 is implemented across the existing service ownership boundaries. The completed result includes:

- real tenant-scoped agent, team, SLA-risk and tenant queues with pagination, filtering, sorting, assignment and persisted ticket context;
- authenticated SSE refresh hints with safe disconnected-client cleanup;
- secure attachment upload, content validation, malware scanning, generated storage keys, ownership checks and clean-only download;
- explicit knowledge draft, review, rejection, publish, supersede, rollback and archive transitions with atomic active-version indexing;
- hybrid retrieval using query normalization, `websearch_to_tsquery`, relaxed lexical fallback, vector candidates, metadata predicates and active-published-version isolation;
- authenticated lifecycle seeding, deterministic embeddings, a repair/reindex endpoint and idempotent routing/SLA seed business keys;
- six distinct API-backed role workspaces with read-only Auditor behavior and human approval before customer-visible AI output;
- provider-neutral deterministic, OpenAI-compatible and Gemini adapters, ordered guardrails, PII/secret handling, token/cost budgets and persisted governance readback;
- resilient analysis, routing and retrieval calls with explicit workflow failure persistence;
- canonical Kafka triage topics shared by producer and consumer contracts, duplicate-delivery protection and transactional outbox processing;
- served OpenAPI documents, a Kustomize application base, HPA/PDB/Ingress/NetworkPolicy resources, hot-reload Compose profiles and CI acceptance/security/evaluation gates.

### Acceptance defects found and closed

The final browser journey caught and closed three issues that unit-only verification would not have exposed:

1. Historical random seed identifiers had accumulated duplicate SLA policies. Migration `V2__routing_configuration_uniqueness.sql` deduplicates existing rows, preserves routing-decision references, enforces tenant business keys and makes subsequent seeds idempotent.
2. The orchestration outbox publisher used legacy topic names while the ticket consumer subscribed to the canonical contract names. Both now use `TicketEvents.TICKET_TRIAGE_COMPLETED` and `TicketEvents.TICKET_TRIAGE_FAILED`, protected by regression tests.
3. A staff team queue could issue an unauthorized request before its routing directory finished loading. The frontend now waits for the authenticated user's permitted team instead of falling back to the first tenant team.

### Measured verification

The final acceptance run produced these results:

| Gate | Result |
|---|---|
| Full Java reactor | `./mvnw verify` — all 11 modules `BUILD SUCCESS`, including PostgreSQL/pgvector, Kafka and concurrent-login integration tests |
| Frontend lint | `npm run lint` — zero warnings/errors |
| Frontend component tests | Vitest — 3/3 passed |
| Frontend production build | TypeScript and Vite build passed |
| Browser acceptance | Playwright — 8/8 passed: six role boundaries plus customer-to-agent and knowledge lifecycle journeys |
| Retrieval evaluation | 100 cases; Recall@5 94%, MRR 0.8344, PII leak 0, unsafe auto-send 0 |
| Routing seed upgrade | exactly one SLA policy per tenant/priority and one seed rule per tenant/name/version after repeated execution |
| Runtime contracts | six gateway OpenAPI documents returned HTTP 200; all Compose application services reached healthy state |
| Deployment/security | Compose validation, Kustomize render and repository secret scan passed |

The owner-run screenshots and demonstration video listed in `docs/part1/ARCHITECTURE_AND_DEMO_EVIDENCE.md` remain presentation artifacts. They do not represent unfinished product functionality.
