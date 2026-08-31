# ResolveIQ Beyond Parity Roadmap

## Honest objective

ResolveIQ can surpass the referenced AI Support System project, but not by adding more microservices or more AI labels. It must first reach feature parity, then demonstrate five differentiated workflows that solve real support-team problems end to end.

The winning position should be:

> ResolveIQ is a proactive, evidence-driven customer-support operations platform that detects widespread issues, preserves conversations across channels, safely executes resolutions, understands customer evidence, and continuously verifies whether customers were actually helped.

## Part 1: Current ResolveIQ gaps

The executable design, delivery order, API contracts, data changes, UI journeys, and
verification gates for this part are defined in
[`RESOLVEIQ_PART1_IMPLEMENTATION_PLAN.md`](RESOLVEIQ_PART1_IMPLEMENTATION_PLAN.md).
That document is the source of truth for completing Part 1 before any Part 2
differentiated feature is started.

**Completion status (2026-08-31):** Part 1 is implemented and acceptance-verified. The completion checklist, measured test results, closed acceptance defects and remaining owner-run portfolio artifacts are recorded in that plan. Part 2 can now start from this verified baseline.

### P0 — Correctness and credibility gaps

These must be fixed before building new features.

| Gap | Required improvement | Completion proof |
|---|---|---|
| Seeded hybrid search returns zero results | Generate embeddings during seeding and improve lexical query handling | Documented long query returns the expected article |
| Several UI panels contain hardcoded values | Connect citations, SLA, confidence, customer, similar cases and governance metrics to APIs | No production screen displays fictional constants |
| Agent workspace opens only the first ticket | Implement paginated/selectable personal and team queues | Agent can filter, sort, assign and open any authorized ticket |
| Static administrator screens | Implement real workflow, outbox, DLQ, routing and user-management APIs | Admin actions change persisted state |
| Incomplete role experiences | Add genuine Team Lead, Knowledge Manager and Auditor pages | Every seeded role has a distinct working journey |
| Knowledge lifecycle incomplete | Implement draft, review, publish, supersede and rollback | Only approved active versions participate in retrieval |
| Missing attachment handling | Add secure MinIO upload, malware checks and ownership validation | Customer and agent can safely exchange files |
| Limited automated coverage | Add integration, Testcontainers, Kafka redelivery and browser E2E suites | CI proves the principal workflows |
| Retrieval seed has no embeddings | Seed through the real ingestion pipeline instead of direct incomplete SQL | Every published chunk has the configured embedding |
| Search semantics are too strict | Use `websearch_to_tsquery`, relaxed fallback and vector candidates | Natural questions do not require every word to match |

### P1 — Areas where the other project is ahead

ResolveIQ needs parity in:

- Real Spring AI integration with Gemini/OpenAI-compatible providers
- Composable input/output guardrails
- PII and secret redaction
- Prompt-injection testing
- Token and cost budgets
- Metadata-aware retrieval
- Query rewriting
- WebSocket or server-sent real-time updates
- Resilience4j circuit breakers and bulkheads
- Kubernetes manifests
- OpenAPI documentation
- Role-complete frontend
- Larger automated test suite
- Architecture diagrams, screenshots and demonstration video

The other project already has dedicated agent/tool orchestration, metadata-aware RAG, writing-assistant APIs, guardrails, MCP adapters and richer dashboards in its [orchestration service](https://github.com/avisheksingha/ai-support-system/tree/main/ai-orchestration-service), [RAG service](https://github.com/avisheksingha/ai-support-system/tree/main/rag-service), and [React dashboard](https://github.com/avisheksingha/ai-support-system/tree/main/ai-support-dashboard).

### P2 — ResolveIQ advantages to preserve

Do not lose these strengths while reaching parity:

- Mandatory human approval before customer-visible AI responses
- Tenant and customer ownership enforcement
- Scoped command idempotency
- Transactional outbox processing
- Persisted retrieval runs and citations
- Immutable AI-suggestion feedback
- Knowledge plus sanitized resolved-case retrieval
- Production configuration guards
- Deterministic offline development
- Honest separation of API-backed and demo-only functionality

These make ResolveIQ safer and more defensible during an interview.

# Part 2: Five differentiated features

## 1. Support Incident Radar and proactive communication

### Real-world problem

During an outage, hundreds of customers create nearly identical tickets. Agents repeatedly investigate and answer them while engineering may already be handling one underlying incident.

Atlassian explicitly positions proactive incident communication as a way to reduce duplicate support requests. See [Atlassian Statuspage](https://www.atlassian.com/software/statuspage).

### Feature

ResolveIQ automatically:

1. Embeds new ticket symptoms.
2. Finds similar tickets within a recent time window.
3. Detects an abnormal increase by product, region, error code or tenant.
4. Creates a proposed incident.
5. Links matching tickets to that incident.
6. Correlates application telemetry and deployment events.
7. Shows affected customers and components.
8. Drafts an audience-specific update.
9. Requires an incident manager to approve publication.
10. Sends updates to all linked customers.
11. Resolves or updates linked tickets when the incident changes.

### UI

Add an **Incident Radar** workspace containing:

- Live anomaly graph
- Emerging ticket clusters
- Sample symptoms and error codes
- Affected products, regions and tenant count
- Confidence and explanation
- Linked tickets
- Associated deployment or monitoring alert
- **Create Incident** approval
- Incident timeline
- Customer communication preview
- Status-page publication controls

Customers should see an incident banner before opening another ticket.

### Backend design

Start as modules inside orchestration and ticket services:

- `TicketSimilarityService`
- `ClusterDetectionService`
- `IncidentService`
- `CustomerImpactService`
- `IncidentCommunicationService`

New entities:

- `support_incident`
- `incident_cluster`
- `incident_ticket_link`
- `incident_component`
- `incident_update`
- `customer_impact`
- `notification_subscription`

New events:

- `TicketClusterThresholdReached`
- `IncidentProposed`
- `IncidentApproved`
- `IncidentUpdated`
- `IncidentResolved`

### AI and deterministic logic

AI may summarize symptoms and draft communications. Deterministic code must decide:

- Cluster threshold
- Time window
- Affected-customer scope
- Who may approve
- Notification audience
- Incident state transition

### Acceptance criteria

- Twenty similar tickets produce one proposed cluster.
- An unrelated ticket is not attached.
- Cross-tenant details never leak.
- A linked customer sees the incident before submitting a duplicate ticket.
- One approved update reaches every affected test customer.
- Closing the incident updates all linked tickets without duplicate events.

### Interview value

This demonstrates embeddings, streaming, anomaly detection, event correlation, human approval and proactive customer experience in one workflow.

## 2. Policy-controlled resolution actions

### Real-world problem

Generating a good reply does not resolve the customer’s problem. Agents still switch to payment, identity, order and account systems to perform actions.

### Feature

Allow ResolveIQ to propose and safely execute actions such as:

- Refund a duplicate charge
- Void a pending authorization
- Resend an invoice
- Unlock an account
- Revoke active sessions
- Reset MFA
- Reship an order
- Apply a service credit
- Rotate an API credential
- Retry a failed webhook

### Safety model

The LLM must never directly execute an operation.

Use this flow:

```text
AI proposes action
→ typed command generated
→ policy engine validates it
→ system fetches current state
→ trusted UI displays exact effect
→ authorized human approves
→ command executes idempotently
→ result is reconciled
→ audit event is persisted
→ customer receives confirmation
```

OWASP recommends limiting agent permissions, enforcing authorization outside the model, and requiring human approval for high-impact actions. See [OWASP Excessive Agency guidance](https://genai.owasp.org/llmrisk/llm062025-excessive-agency/).

### UI

In the Agent Workspace, add a **Resolution Actions** panel:

- Proposed action
- Reason and supporting evidence
- Customer/account affected
- Exact amount or configuration change
- Risk level
- Policy that permits or rejects it
- Before/after preview
- Approval requirement
- Idempotency status
- Execution result
- Compensation or rollback option

The trusted UI must render actual normalized command arguments, not an AI-generated description.

### Backend design

Create a controlled action framework:

```java
interface ResolutionAction<I, O> {
    ActionType type();
    ValidationResult validate(ActionContext context, I input);
    O execute(ActionContext context, I input);
    CompensationResult compensate(ActionContext context, O result);
}
```

Required components:

- Action registry
- JSON-schema validation
- Policy engine
- Risk classifier
- Approval service
- Idempotency store
- Credential vault adapter
- Execution audit
- Compensation workflow
- Reconciliation scheduler

Begin with simulated external systems. Implement only two actions deeply:

1. Duplicate-charge refund
2. Account unlock

### Acceptance criteria

- A customer cannot trigger an action through prompt injection.
- An agent cannot exceed their financial approval limit.
- Repeated execution with the same key produces one external action.
- Changing the amount invalidates the earlier approval.
- Failed execution can be retried safely.
- Every operation has approver, policy version, normalized input and result.
- A compensation action is available where the provider supports it.

### Interview value

This moves the system from “AI writes replies” to “AI helps resolve problems safely.”

## 3. Omnichannel conversation continuity and intelligent handoff

### Real-world problem

Customers contact support through web, email, Slack, Teams, WhatsApp or voice and must repeat their story after changing channels or reaching a human.

Only 15% of consumers in a Twilio report described AI-to-human handoff as seamless. See the [Twilio conversational AI report](https://www.twilio.com/en-us/press/releases/twilio-s-latest-report-highlights-conversational-ai-rapid-adop).

### Feature

Create one canonical conversation across:

- Customer portal
- Email
- Slack
- Microsoft Teams
- WhatsApp sandbox
- Optional voice transcript

A customer can begin through email and continue in the portal without losing:

- Messages
- Attachments
- Authentication state
- AI analysis
- Agent notes
- Promised actions
- Prior attempted resolutions

### UI

Customer UI:

- Channel history
- Current assigned agent
- Human-handoff button
- Estimated response window
- Visible AI/human identity
- Channel preference
- Communication consent

Agent UI:

- Unified chronological timeline
- Channel badges
- Customer identity confidence
- Duplicate-conversation merge
- Internal notes
- Human-handoff summary
- Reply-channel selection

### Backend design

Introduce:

- `ChannelAdapter` interface
- Web and email adapters first
- Inbound message normalization
- External-message idempotency
- Customer identity resolution
- Conversation merge/split
- Consent and channel preferences
- Outbound delivery status
- Provider webhook signature validation

Canonical event:

```json
{
  "externalMessageId": "...",
  "channel": "EMAIL",
  "conversationId": "...",
  "tenantId": "...",
  "senderIdentity": {},
  "content": {},
  "attachments": [],
  "receivedAt": "..."
}
```

Microsoft describes unified routing as assigning work from chat, messaging, voice and other channels using rules, skills and AI classification. See [Microsoft unified routing](https://learn.microsoft.com/en-us/dynamics365/release-plan/2025wave2/service/dynamics365-customer-service/unified-routing).

### Acceptance criteria

- Retried provider webhooks do not create duplicate messages.
- Replies are delivered through the correct channel.
- A customer moving from email to portal retains the complete conversation.
- Identity merges require verification when confidence is insufficient.
- Internal notes never appear in customer channels.
- Human handoff includes an evidence-backed summary.
- Channel failure falls back without losing the message.

### Interview value

This demonstrates adapters, webhook security, identity resolution, event normalization, idempotency and real-time UX.

## 4. Multimodal Support Evidence Lab

### Real-world problem

Customers frequently attach screenshots, PDFs, logs, audio recordings and screen-capture videos. Text-only ticket analysis misses the most valuable diagnostic evidence.

### Feature

ResolveIQ should accept and analyze:

- Screenshots
- PDF invoices
- Application logs
- HAR files
- CSV exports
- Short screen recordings
- Voice messages

It should extract:

- OCR text
- Error codes
- Stack-trace fingerprints
- Timestamps
- Relevant UI region
- Redacted invoice facts
- Reproduction steps
- Video moments where failure occurs

### UI

Customer:

- Drag-and-drop upload
- File validation and progress
- Sensitive-data warning
- Preview and remove controls
- Consent before AI processing

Agent:

- Evidence timeline
- Screenshot annotations
- Extracted error codes
- Log-event grouping
- Video chapter markers
- **Jump to failure at 00:42**
- Redacted/original access based on role
- Download audit indicator

### Backend design

Use an asynchronous pipeline:

```text
Upload session
→ malware/type validation
→ quarantine
→ metadata extraction
→ PII/secret detection
→ redacted derivative
→ OCR/transcription/frame sampling
→ chunking and embedding
→ evidence linking
→ agent-ready event
```

Components:

- MinIO storage adapter
- Signed URLs
- Antivirus integration
- Apache Tika for documents
- OCR adapter
- Log parser
- Audio transcription port
- Video frame sampler
- Multimodal model port
- Evidence retention policy

Never place untrusted attachment text directly into the system prompt. Treat it as quoted, untrusted evidence and scan for indirect prompt injection.

### Acceptance criteria

- Unsupported and oversized files are rejected.
- Cross-tenant file access returns no data.
- Malware remains quarantined.
- Secrets are redacted before model processing.
- Original and redacted objects have different authorization rules.
- Screenshot OCR extracts a seeded error code.
- A seeded video produces the correct failure timestamp.
- Deleting a ticket follows the configured evidence-retention policy.

### Interview value

This creates a highly visual demonstration involving object storage, asynchronous processing, multimodal AI, security and vector retrieval.

## 5. Verified Resolution and Knowledge Flywheel

### Real-world problem

A ticket marked `RESOLVED` does not prove that the customer’s problem was solved. Incorrect resolutions lead to reopened tickets, repeated contacts and stale knowledge articles.

### Feature

ResolveIQ should measure outcome, learn from validated resolutions and improve knowledge safely.

Flow:

1. Agent sends a resolution.
2. Customer confirms whether it worked.
3. The system monitors reopening or repeat-contact signals.
4. The resolution receives an outcome score.
5. Successful cases become knowledge candidates.
6. PII is removed.
7. A Knowledge Manager reviews the before/after diff.
8. The candidate is evaluated against a frozen dataset.
9. Approved content becomes a new version.
10. Retrieval performance is compared before activation.
11. Poor versions can be rolled back.

### UI

Customer:

- **Did this solve your problem?**
- Yes / Partly / No
- Optional reason
- Reopen ticket
- Request human assistance

Knowledge Manager:

- Content gaps
- Repeated unsuccessful answers
- Suggested article update
- Source cases
- Sanitization diff
- Retrieval evaluation comparison
- Publish/cancel/rollback

Administrator:

- First-contact resolution
- Reopen rate
- Repeat-contact rate
- Citation usefulness
- AI acceptance/edit/rejection rate
- Article success rate
- Abstention quality
- Cost per successfully resolved ticket

### Backend design

Add:

- `ResolutionOutcome`
- `RepeatContactSignal`
- `KnowledgeCandidate`
- `SanitizationReview`
- `EvaluationDatasetVersion`
- `EvaluationRun`
- `KnowledgeRelease`
- `RollbackRecord`

Important rules:

- Feedback never directly changes a prompt or article.
- Training and activation require evaluation plus approval.
- Customer outcome outweighs agent `resolved` status.
- Metrics must come from persisted events, not hardcoded dashboards.
- Evaluation datasets must include paraphrases, typos, conflicting articles, attacks and zero-result cases.

### Acceptance criteria

- A **No** response reopens or escalates the ticket.
- Repeat contact links to the earlier resolution.
- A knowledge candidate contains no customer PII.
- Publishing requires Knowledge Manager approval.
- Retrieval evaluation runs before activation.
- A regression blocks activation.
- An older article version can be restored atomically.
- Dashboard metrics reconcile with database records.

### Interview value

This demonstrates that ResolveIQ is not merely using AI—it evaluates whether AI improves real customer outcomes.

# Part 3: Recommended implementation sequence

## Stage 0 — Establish a trustworthy baseline

Complete:

1. Search and embedding defect
2. API-backed UI
3. Queue selection
4. Knowledge lifecycle
5. Role completeness
6. Real provider and guardrails
7. Integration and browser E2E tests
8. Observability and resilience
9. Secure attachments
10. Deployment documentation

Do not begin all five differentiators before this gate passes.

## Stage 1 — Incident Radar

Build clustering using existing tickets and embeddings. It reuses Kafka, RAG, tenant isolation and the current human-approval model.

## Stage 2 — Resolution Actions

Implement two high-quality simulated actions. This provides the strongest backend/system-design interview story.

## Stage 3 — Omnichannel continuity

Start with portal plus email. Add Slack or WhatsApp only after message normalization and idempotency are proven.

## Stage 4 — Multimodal Evidence Lab

Start with screenshots, PDF and logs. Add audio/video after storage, redaction and access control are reliable.

## Stage 5 — Verified Resolution Flywheel

Use the outcomes generated by previous stages to create meaningful evaluation and knowledge-improvement data.

# Definition of “better”

ResolveIQ should only claim superiority when one repeatable demonstration proves:

1. A customer reports an issue from email with a screenshot.
2. ResolveIQ merges it into the customer’s existing conversation.
3. The screenshot produces a redacted error-code extraction.
4. Similar-ticket clustering detects a broader incident.
5. The incident manager approves a proactive update.
6. Duplicate tickets are linked automatically.
7. An agent receives evidence-backed resolution guidance.
8. A policy-controlled action is proposed.
9. An authorized human approves the exact action.
10. The action executes once despite a retry.
11. The customer confirms that the issue is solved.
12. The successful case becomes a sanitized knowledge candidate.
13. Evaluation proves that the new article improves retrieval.
14. A Knowledge Manager publishes it with rollback available.
15. Every step is tenant-safe, traceable and covered by automated tests.

That complete story would place ResolveIQ meaningfully above a standard AI ticket-classification and RAG demonstration.
