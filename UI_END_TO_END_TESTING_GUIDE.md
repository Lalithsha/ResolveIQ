# ResolveIQ UI End-to-End Testing Guide

## 1. Purpose and testing rule

> **Purpose / feature:** Defines how manual acceptance is recorded and prevents a visual/demo-only screen from being mistaken for working functionality. A pass means the stated API-backed outcome was observed, not merely that the page rendered.

This guide is the manual acceptance checklist for the UI that exists in the current repository. Follow it from top to bottom to exercise every role and every user-visible control without guessing.

Record each check as `PASS`, `FAIL`, or `LIMITATION`. A `PASS` requires the expected API-backed result described here. A page that only displays sample content or an alert is explicitly marked `DEMO-ONLY` and must not be reported as a completed feature.

## 2. Start the complete local environment

> **Purpose / feature:** Starts the distributed ResolveIQ application and verifies that its browser, gateway, database, messaging, discovery and object-storage dependencies are available. This establishes a healthy baseline before testing business features.

The following ports avoid the common local conflicts on PostgreSQL `5432`, Kafka `9092`, gateway `8080`, and frontend `3000`:

```bash
POSTGRES_PORT=55432 \
KAFKA_PORT=19092 \
GATEWAY_PORT=18080 \
FRONTEND_PORT=3300 \
docker compose --profile app up -d --build
```

Check all services:

```bash
POSTGRES_PORT=55432 \
KAFKA_PORT=19092 \
GATEWAY_PORT=18080 \
FRONTEND_PORT=3300 \
docker compose --profile app ps
```

Wait until the application services report `healthy`. During the first development start, each backend container compiles its module before starting, so this may take longer than a production-image start.

Open these URLs:


| Component          | URL                                      | Expected result                                    |
| ------------------ | ---------------------------------------- | -------------------------------------------------- |
| Web application    | `http://localhost:3300`                  | ResolveIQ login page                               |
| API gateway health | `http://localhost:18080/actuator/health` | JSON containing `"status":"UP"`                    |
| Service discovery  | `http://localhost:8761`                  | Eureka dashboard                                   |
| Kafka console      | `http://localhost:8090`                  | Redpanda Console                                   |
| MinIO console      | `http://localhost:9001`                  | MinIO login page; clean ticket attachments are stored here |


If startup fails with `port is already allocated`, run `docker compose --profile app down` without `-v`, then use the conflict-free command above. Never add `-v` unless you intentionally want to delete local ResolveIQ data.

## 3. Load the fictional demo data

> **Purpose / feature:** Creates repeatable users, tickets, routing rules and indexed knowledge needed by later journeys. Running the real ingestion path proves the demo does not depend on incomplete hand-written database records.

Run this once after the schemas have been created and all backend services are healthy:

```bash
./scripts/seed-data.sh
```

The seed contains only fictional data and uses deterministic identifiers so it can be rerun safely. Relational fixtures are upserted, and knowledge articles are created/published through the authenticated lifecycle API instead of inserting incomplete chunks directly.

The seed command verifies lifecycle indexing and deterministic embeddings for local knowledge. If Help Center search returns zero results after a reset, rerun `./scripts/seed-data.sh`, confirm that lifecycle ingestion completes, and treat any remaining zero-result response as a defect.

### Demo accounts

> **Purpose / feature:** Maps each fictional identity to the role-specific workspace it is authorized to test. Switching accounts proves that functionality and data visibility change according to persisted roles.

All seeded accounts use the fictional password `ResolveIQ2026!`.


| Persona       | Email                           | Role              | Primary UI to test                      |
| ------------- | ------------------------------- | ----------------- | --------------------------------------- |
| Alex Morgan   | `alex.morgan@acme.com`          | CUSTOMER          | Ticket creation and customer replies    |
| Sarah Chen    | `sarah.chen@resolveiq.local`    | AGENT             | Queue, AI draft, feedback, and approval |
| Marcus Vance  | `marcus.vance@resolveiq.local`  | TEAM_LEAD         | Team queue, assignment and SLA risk      |
| Elena Rostova | `elena.rostova@resolveiq.local` | KNOWLEDGE_MANAGER | Knowledge lifecycle and retrieval        |
| David Kross   | `admin@resolveiq.local`         | ADMIN             | Operations, routing, users and governance |
| Priya Nair    | `auditor@resolveiq.local`       | AUDITOR           | Read-only evidence and governance        |


Use a private/incognito browser window when changing personas, or click the sign-out icon before the next login.

## 4. Global authentication and layout checks

> **Purpose / feature:** Verifies the shared login, session and role-aware application shell used by every persona. These checks prove users enter the correct tenant workspace without exposing or manually supplying trusted identity data.

### 4.1 Login form

> **Purpose / feature:** Tests credential entry, invalid-login handling and role-aware authentication. A pass proves inputs are usable and successful login returns the correct persisted identity and allowed roles.

1. Open `http://localhost:3300`.
2. Confirm the right panel says **Welcome back**.
3. Click the email input and type a complete address.
4. Confirm every typed character is visible in both light and dark operating-system themes.
5. Click the password input and type at least 12 characters.
6. Confirm visible password-mask dots appear for each character.
7. Submit an invalid email/password combination.
8. Confirm an inline red error appears and the page does not pretend login succeeded.
9. Login with one of the seeded personas.
10. Confirm the navbar displays the correct full name and only roles assigned to that user.

Expected: access tokens remain in memory, the refresh session is cookie-backed, and identity/tenant headers are not entered by the user.

### 4.2 Customer self-registration

> **Purpose / feature:** Tests safe public onboarding for a new customer. It proves registration creates a usable customer account while preventing privilege selection or staff-role creation.

1. Sign out.
2. Click **Create account**.
3. Confirm the heading changes to **Create your account**.
4. Enter a fictional full name, a unique email such as `ui.customer.01@example.test`, and a password of 12–128 characters.
5. Click **Create account**.
6. Confirm the user is signed in and sees the Customer navigation.
7. Sign out, then sign in again with the new credentials.

Expected: public registration creates only a `CUSTOMER`; it cannot create an agent, administrator, or knowledge manager.

### 4.3 Refresh-session check

> **Purpose / feature:** Tests refresh-token session continuity and logout invalidation. A pass proves a reload restores a valid session, while logout prevents that session from returning.

1. Login successfully.
2. Refresh the browser tab.
3. Confirm **Preparing your workspace…** appears briefly and the authenticated workspace returns without another login.
4. Click the sign-out icon in the top-right corner.
5. Refresh again.

Expected: refresh restores a valid session before logout; after logout the login page remains.

### 4.4 Global controls

> **Purpose / feature:** Tests that the shared header reflects authenticated backend data and clearly identifies unfinished controls. It proves role switching is limited to assigned roles and prevents the notification placeholder from being claimed as complete.

1. Confirm the navbar shows `ResolveIQ`, `Alpha`, user name, role selector, notification icon, and sign-out icon.
2. Confirm the role selector lists only the roles returned for the current account.
3. Click the notification bell.

Expected: the bell currently has no behavior and is `DEMO-ONLY`. Do not report notifications as implemented.

## 5. Customer journey — API-backed

> **Purpose / feature:** Exercises the complete customer self-service path: create a problem report, follow asynchronous triage, continue the conversation and search approved knowledge. It proves customers interact only with their own persisted data.

Login as `alex.morgan@acme.com`.

### 5.1 Create a billing ticket

> **Purpose / feature:** Tests idempotent ticket intake and the event-driven triage trigger. A pass creates one real ticket and starts analysis, retrieval, routing and SLA work without making the customer wait for AI processing.

1. Select **Create Ticket** in the sidebar or **New Request** at the top.
2. Enter subject: `Duplicate payment authorization after checkout`.
3. Select **Billing & Payments**.
4. Enter description: `Two payment authorizations appeared within five minutes. The order is still pending. Please verify the duplicate safely without exposing card data.`
5. Click **Submit Request** once.
6. Confirm the button changes to **Submitting Ticket...** while the request is active.
7. Confirm **Ticket Created Successfully** appears with a real `RIQ-YYYY-NNNNNN` ticket number.
8. Record the ticket number in the test-results table at the end of this document.

Expected backend effects: ticket persistence, scoped idempotency key, ticket-created outbox event, AI analysis, hybrid retrieval, routing, SLA calculation, grounded draft generation, completion outbox event, and ticket projection.

### 5.2 Verify asynchronous triage

> **Purpose / feature:** Tests the Kafka/outbox orchestration that converts a newly accepted ticket into agent-ready work. A pass proves background processing updates the correct customer-owned ticket to `READY_FOR_AGENT`.

1. Click **View My Tickets**.
2. Click **Refresh** until the new ticket appears.
3. Confirm the ticket progresses from `NEW` to `READY_FOR_AGENT`. Normally this takes a few seconds after services are warm.
4. If it remains `NEW`, inspect `docker compose logs ticket-service ai-orchestration-service` and record a failure.
5. Confirm the ticket remains visible only to the customer who created it.



### 5.3 Inspect and reply to a ticket

> **Purpose / feature:** Tests persisted two-way ticket conversation and customer ownership. A pass proves a public reply survives navigation/reload and appears only on the owning customer’s ticket.

1. Click the created ticket row.
2. Confirm its ticket number, subject, and description match the submitted values.
3. Confirm **Conversation History** loads existing public messages, or shows the no-replies state for a new ticket.
4. Enter `Additional detail: the second authorization is still pending.` in **Add Reply to Support Team**.
5. Click **Send Reply**.
6. Confirm the new message appears in **Conversation History** as **You**, without reloading the page.
7. Click **Back to All Tickets** and confirm the ticket remains present.
8. Reopen the ticket and confirm the reply is loaded from the backend.

Expected: the reply is persisted as a public customer message and is visible after reopening the ticket.

### 5.4 Search the self-service Help Center

> **Purpose / feature:** Tests hybrid lexical/vector retrieval over approved active knowledge. A pass proves natural-language customer questions return real indexed articles instead of hardcoded search cards or unpublished content.

1. Select **Help Center** in the sidebar or the customer portal sub-navigation.
2. Enter `duplicate charge invoice billing dispute` and click **Search Articles**.
3. Confirm a loading state appears, followed by a real result count.
4. Confirm the seeded payment-dispute article **Payment Reconciliation & Duplicate Charge Handling** appears when the retrieval service is healthy and indexed.
5. Open a result and confirm its title, source type, text snippet, and optional confidence score are displayed.
6. Click **Back to Results**, then **Clear Results**.
7. Click the **Billing & Invoice Disputes** popular topic and confirm it runs a pre-filled search.
8. Click **Submit a Support Ticket** in the bottom banner and confirm the create-ticket view opens.

Expected: searches call the hybrid knowledge retrieval API. Curated topic cards provide search queries; they are not hardcoded article results.

### 5.5 Customer navigation coverage

> **Purpose / feature:** Confirms every Customer navigation entry opens its intended implemented workflow. This prevents inaccessible or demo-only pages from being counted as customer features.


| Sidebar item  | Current behavior                                          | Result classification |
| ------------- | --------------------------------------------------------- | --------------------- |
| Create Ticket | Opens the real ticket form                                | API-BACKED            |
| My Tickets    | Opens the real customer ticket list and conversation view | API-BACKED            |
| Help Center   | Opens hybrid knowledge search and article-result details  | API-BACKED            |




## 6. Agent journey — fully API-backed workspace

> **Purpose / feature:** Exercises the agent’s real queue-to-resolution workflow using persisted tickets, AI evidence and explicit human control. It proves an agent can investigate authorized work without fabricated UI data or automatic customer messaging.

Login as `sarah.chen@resolveiq.local` after completing the customer flow. The seeded ticket `RIQ-2026-000412` is also assigned to this agent and can be used if a new ticket is not yet available.

### 6.1 Load the queue and AI suggestion

> **Purpose / feature:** Tests authorized queue selection, filtering and complete ticket/AI context loading. A pass proves the agent can choose real assigned work and inspect persisted classifications, citations, SLA and drafts before acting.

1. Select **My Queue**.
2. Confirm the paginated queue lists only tickets assigned to Sarah and does not auto-open an unauthorized ticket.
3. Select two different rows and confirm the center panel changes to the chosen persisted ticket.
4. Use search, priority, status and sort controls; confirm the list and total count update.
5. Confirm ticket number, priority, status, customer, team/assignee, first-response SLA, messages and attachments correspond to the selected backend ticket.
6. Confirm classification badges, confidence, provider/prompt metadata, citations and similar cases show persisted values or an explicit `No ...` state—never sample values.
7. Confirm the response composer contains the persisted AI suggestion when one exists.
8. Confirm **Approve & Send** is visible but no message is sent automatically.

### 6.2 Accept feedback

> **Purpose / feature:** Tests immutable positive feedback for an unchanged AI suggestion. This records that the draft was useful for later governance metrics without sending it to the customer.

Use a ticket with a suggestion that has no previous feedback.

1. Do not edit the composer.
2. Click **Accept**.
3. Confirm `Feedback recorded: ACCEPTED` appears.
4. Click **Dismiss**.

Expected: one feedback record is stored for that suggestion. Feedback is intentionally immutable, so do not try another feedback action on the same suggestion.

### 6.3 Edited feedback

> **Purpose / feature:** Tests whether ResolveIQ captures the agent’s corrected version of an AI draft. This distinguishes “useful after editing” from acceptance and supplies auditable quality feedback.

Use a different newly triaged ticket.

1. Change the composer text while preserving a safe, professional response.
2. Click **Edit** in the feedback area.
3. Confirm `Feedback recorded: EDITED` appears.

Expected: the edited content and `EDITED` action are persisted. The **Edit** button records the current composer content; it does not open a separate modal.

### 6.4 Rejected feedback

> **Purpose / feature:** Tests safe rejection of an incorrect or unsupported AI draft with a required reason. A pass proves harmful suggestions can be audited and cannot be silently treated as successful.

Use a third newly triaged ticket.

1. Click **Reject**.
2. Enter a reason such as `The draft makes an unsupported claim about a completed refund.`
3. Confirm `Feedback recorded: REJECTED` appears.
4. Repeat only to verify validation if desired: canceling or submitting a blank reason should not record rejection.



### 6.5 Human approval and customer send

> **Purpose / feature:** Tests ResolveIQ’s central Human-in-the-Loop safety boundary. A pass proves no AI draft reaches the customer until an authorized agent explicitly approves it and the persisted ticket status changes accordingly.

Use a ticket that has not already been sent.

1. Review or edit the response composer.
2. Click **Approve & Send**.
3. Confirm the button changes to **Sending...**.
4. Confirm the success message states that the ticket moved to `WAITING_ON_CUSTOMER`.
5. Sign out and login as the owning customer.
6. Open **My Tickets** and confirm the ticket status is `WAITING_ON_CUSTOMER`.

Expected: ResolveIQ records feedback if needed, creates the agent’s public message, and changes ticket status only after explicit approval.

### 6.6 Agent navigation coverage

> **Purpose / feature:** Confirms every Agent navigation entry is connected to an authorized API-backed queue or retrieval workflow. It also proves team and SLA views are scopes of real ticket data, not static dashboards.


| Sidebar item     | Current behavior                                            | Result classification |
| ---------------- | ----------------------------------------------------------- | --------------------- |
| My Queue         | Selectable, filterable assigned-ticket queue                | API-BACKED            |
| Team Queue       | Team-scoped selectable queue                                | API-BACKED            |
| SLA Risk         | Deadline-sorted at-risk queue within authorized scope       | API-BACKED            |
| Knowledge Search | Opens the real hybrid retrieval screen                      | API-BACKED search     |

### 6.7 Secure attachment exchange

> **Purpose / feature:** Tests safe evidence exchange through validation, malware scanning, object storage and scoped download authorization. A pass proves clean files round-trip while unsafe, unsupported or foreign-owned content remains inaccessible.

1. Open an owned ticket and click **Attach**.
2. Upload a small `.txt`, `.json`, `.png`, `.jpg`, or `.pdf` file containing only fictional data.
3. Confirm **Scanning…** appears and the clean attachment is listed with status `CLEAN`.
4. Click the attachment and confirm the downloaded SHA/content matches the original.
5. Attempt an unsupported executable/archive and confirm it is rejected without adding a row.
6. Use the standard EICAR test string only in an isolated local test file; confirm the upload returns a malware rejection and is not downloadable.

Expected: the server normalizes the name, validates size/type/magic bytes, computes SHA-256, scans before storage, generates the object key, and authorizes list/download through tenant and ticket scope.

## 7. Knowledge Manager journey

> **Purpose / feature:** Exercises governed knowledge authoring and retrieval rather than direct insertion into the vector index. It proves only reviewed, active and tenant-approved content can influence customer or agent answers.

Login as `elena.rostova@resolveiq.local`.

### 7.1 Article lifecycle

> **Purpose / feature:** Tests the complete `DRAFT → IN_REVIEW → PUBLISHED → SUPERSEDED/ROLLED_BACK/ARCHIVED` feature. The goal is to prove indexing and activation are controlled, reversible and exclude every version that is not currently approved.

1. Select **Articles & Chunks** and confirm the heading is **Knowledge lifecycle**.
2. Click **New article**. Create `UI lifecycle payment guide` in `BILLING`, add a product, summary and unique safe troubleshooting content.
3. Click **Save draft** and confirm the article/version is `DRAFT` with no active version.
4. In a separate Customer Help Center search, confirm the unique draft text is not returned.
5. Click **Submit for review**, then **Publish**. Confirm the version becomes `PUBLISHED` and its ID is displayed as active.
6. Open **Vector Indexes**, search the unique text and confirm the published article is returned with title, snippet and RRF score.
7. Return to the lifecycle, click **New version**, save different unique content, submit and publish it.
8. Confirm version 1 is `SUPERSEDED`, version 2 is `PUBLISHED`, and only version 2 participates as the active version.
9. Click **Rollback** on version 1. Confirm version 1 becomes active again and retrieval follows the rolled-back content.
10. Create another draft, submit it, click **Reject**, enter a required actionable note and confirm `REJECTED` plus the persisted review note.
11. Click **Archive** and confirm the document is excluded from retrieval while its history remains visible.

Expected: indexing finishes before the active version changes; unpublished, rejected, superseded and archived content is excluded from normal retrieval.

### 7.2 Retrieval and navigation coverage

> **Purpose / feature:** Tests that each Knowledge Manager area reads the lifecycle’s persisted documents, sanitized cases and vector index. It proves publication state and metadata actually control what hybrid retrieval can return.


| Control           | Current behavior                                        | Result classification |
| ----------------- | ------------------------------------------------------- | --------------------- |
| New article       | Creates persisted draft/version history                 | API-BACKED            |
| Review controls   | Submit, publish, reject with note, supersede and rollback | API-BACKED            |
| Archive           | Removes article from retrieval without deleting history | API-BACKED            |
| Articles & Chunks | Loads persisted documents and lifecycle versions         | API-BACKED            |
| Sanitized Cases   | Lists only approved privacy-sanitized resolved cases     | API-BACKED            |
| Vector Indexes    | Runs metadata-aware lexical/vector RRF retrieval         | API-BACKED            |


Use `duplicate charge invoice billing dispute credit card` as the long-query regression. It must return **Payment Reconciliation & Duplicate Charge Handling** after the lifecycle seed completes.

## 8. Administrator journey

> **Purpose / feature:** Exercises tenant-wide operational control using persisted APIs and audited mutations. It proves administrators can manage platform configuration without relying on fictional metrics or bypassing service ownership.

Login as `admin@resolveiq.local`.

### 8.1 Operations and governance

> **Purpose / feature:** Tests operational visibility into outboxes, workflows, security events and governed AI usage. A pass proves failures and model activity are observable, sanitized and recoverable through real backend state.

1. Select **Overview** and confirm the heading is **Operations overview**.
2. Confirm tenant users, active routing rules, AI invocations, ticket/workflow outbox pending/dead totals, security events and failed-workflow count come from APIs.
3. If a data source is unavailable, confirm the UI says `Unavailable` instead of displaying a sample number.
4. If a real failed workflow exists, click **Retry** and confirm it disappears only after the replay command is accepted. Do not manufacture a failure solely for this manual smoke test.
5. Select **AI governance** and verify totals, valid-output rate, guardrail blocks, estimated cost and recent sanitized model traces.
6. Confirm raw provider output, API keys and unredacted sensitive input are absent.

### 8.2 Routing and user administration

> **Purpose / feature:** Tests persisted routing configuration and staff-role administration. A pass proves authorized changes survive refresh, are tenant-scoped and prevent unsafe self-role modification.

1. Select **Teams & routing** and confirm persisted teams, capacities, rules and SLA policies are listed.
2. Toggle one routing rule inactive, refresh, and confirm the state persists. Restore its original state and refresh again.
3. Select **Users & roles** and create a fictional staff user with a 12+ character temporary password and a non-customer role.
4. Confirm the new user appears in the tenant list.
5. Change that user's primary role and confirm readback after refresh.
6. Confirm the current administrator cannot change their own role from this table.

### 8.3 Administrator navigation coverage

> **Purpose / feature:** Confirms the complete Administrator sidebar maps to working tenant-wide capabilities. This ensures each advertised admin area has real readback and appropriate mutation authority.


| Sidebar item     | Current behavior                                      | Result classification |
| ---------------- | ----------------------------------------------------- | --------------------- |
| Overview         | Persisted operations, outbox and workflow state       | API-BACKED            |
| All Tickets      | Tenant-wide selectable queue and assignment controls | API-BACKED            |
| Teams & Routing  | Teams, rules, SLA policies and persisted rule toggle  | API-BACKED            |
| Knowledge Base   | Full knowledge lifecycle with administrator authority | API-BACKED            |
| AI Governance    | Persisted sanitized usage/guardrail/cost traces       | API-BACKED            |
| Users & Roles    | Tenant staff creation and audited role changes        | API-BACKED            |


Every displayed row must be persisted or explicitly empty/unavailable. A successful admin mutation must survive browser refresh.

## 9. Team Lead and Auditor journeys

> **Purpose / feature:** Verifies the two non-customer specialist roles have distinct capabilities: operational team control for the Team Lead and immutable read-only evidence for the Auditor. It proves they do not fall through to another role’s UI.

### 9.1 Team Lead team operations

> **Purpose / feature:** Tests team-scoped queue access, assignment and SLA-risk management. A pass proves the lead can manage only authorized teams and that assignment changes persist.

Login as `marcus.vance@resolveiq.local`.

1. Confirm authentication succeeds with only the `TEAM_LEAD` role.
2. Open **Team queue** and verify only Marcus's authorized team is selectable.
3. Select a ticket and assign/reassign its team or agent. Refresh and confirm persistence.
4. Open **SLA risk** and confirm the authorized at-risk projection and SLA ordering.
5. Open **Knowledge** and confirm retrieval works without knowledge-publication controls.

### 9.2 Auditor read-only evidence

> **Purpose / feature:** Tests the independent audit workspace and defense-in-depth mutation denial. A pass proves the Auditor can inspect evidence across governed workflows but cannot change tickets, knowledge, users, routing or AI outcomes.

Sign out and login as `auditor@resolveiq.local`.

1. Confirm the distinct sidebar contains **Security audit**, **Ticket evidence**, **Workflow audit**, and **AI governance**.
2. Confirm security events, all-tenant ticket evidence, workflows and sanitized model traces load from APIs.
3. Confirm assignment, retry, routing-toggle, user-creation, role-change, knowledge-publication, feedback and send controls are absent.
4. Use an API client for one auditor mutation request and confirm backend `403`; hidden UI alone is not authorization proof.

## 10. Authorization-negative tests through the UI

> **Purpose / feature:** Tests RBAC, session separation and customer-data isolation by attempting access that should not be available. These negative checks prove security is based on authenticated ownership/roles rather than simply hiding menu items.

1. Login as a customer and confirm the role selector does not offer AGENT, KNOWLEDGE_MANAGER, or ADMIN.
2. Login as an agent and confirm CUSTOMER/ADMIN roles are not available unless explicitly assigned.
3. Login as the knowledge manager and attempt normal knowledge search; confirm it succeeds.
4. Logout between personas and confirm one persona’s session is not reused as another.
5. Open two incognito windows with different personas and confirm customer ticket lists do not leak across accounts.

The SPA has no URL router with protected deep links, so endpoint-level forbidden checks cannot all be performed by navigation alone. Use backend integration tests or an API client for exhaustive `401`, `403`, and cross-tenant checks.

## 11. Hot-reload acceptance checks

> **Purpose / feature:** Verifies the local development productivity feature added to Compose. A pass proves ordinary frontend/backend source edits become visible automatically without rebuilding or restarting the entire stack.

Perform these only after the development stack was built once.

### 11.1 Frontend hot module replacement

> **Purpose / feature:** Tests Vite HMR for React, TypeScript and CSS changes. A pass means the browser reflects a saved frontend edit without any Docker Compose command or container restart.

1. Keep `http://localhost:3300` open.
2. Change a visible label under `frontend/src`.
3. Save the file.
4. Confirm the browser updates without a Compose command or container restart.



### 11.2 Backend automatic rebuild and restart

> **Purpose / feature:** Tests module-scoped Spring Boot recompilation and restart while preserving the last successful process after a compile error. This proves backend development changes do not require rebuilding every container.

1. Run `docker compose logs -f ticket-service`.
2. Make a compilable change under `ticket-service/src/main` and save.
3. Confirm logs contain `[dev-reload] Source change detected for ticket-service`.
4. Confirm Maven compiles the module and only ticket-service restarts.
5. Introduce a temporary compilation error and save.
6. Confirm the build fails while the last successful process remains running.
7. Correct the error and save.
8. Confirm automatic compilation and restart succeed.

Changes to `common-contracts` or `common-security` intentionally trigger all watching backend services. Changes to Compose, Dockerfiles, ports, container environment variables, or frontend dependencies still require a rebuild.

## 12. Part 2 test-data preparation

> **Purpose / feature:** Prepares safe fictional records and media for the five Part 2 journeys. Part 2 screens are state-driven, so a button may correctly be absent until its ticket, incident, action, evidence or resolution reaches the required state.

1. Complete Sections 2 and 3, then keep the full Compose stack running.
2. Complete Customer tests 5.1–5.3 at least three times with closely related subjects such as `Checkout returns payment gateway timeout`, `Payment confirmation stuck after checkout`, and `Duplicate authorization after gateway timeout`. Use fictional details only.
3. Let asynchronous triage finish and record all three ticket numbers.
4. Prepare harmless fixture text containing a timestamp, `ERR-PAY-502`, a fictional request ID and a fictional email. The current Evidence Lab form accepts pasted bytes through **Content / Fixture Text**; it does not yet contain a native binary file picker.
5. Do not upload real customer data, secrets, payment details or personal recordings.
6. Keep two browser sessions available: a normal window for staff and an incognito window for Alex.

Expected: related tickets exist for incident clustering; safe fixture content exists for the UI evidence smoke test; no test relies on production data. Real PNG/PDF/MP4 adapter acceptance is not testable through the current UI and belongs to the automated/API corpus gate.

## 13. Part 2 Feature 1 — Support Incident Radar

> **Purpose / feature:** Detects when many separate tickets are symptoms of one larger outage. The Team Lead reviews the proposed cluster, controls its lifecycle and publishes customer-safe updates instead of agents treating every report independently.

Login as `marcus.vance@resolveiq.local` and select **Incident Radar**.

### 13.1 Detect and review a proposed incident

1. Confirm the metrics and lists load without sample-data errors.
   - With the current seeded data, the correct initial result is `0` Active Incidents, `0` Proposed Clusters, `0` Affected Customers and **All Systems Operational**.
   - No red error banner may be present. `Invalid UUID string: active` is an API-contract failure, not a valid empty state.
2. Click **Scan Anomaly Radar** once and wait for the scan result.
3. If a proposed cluster appears, confirm its component, severity, ticket count and sample ticket IDs match the related test tickets.
4. Confirm unrelated tickets are not included merely because they share common words.
5. Capture the proposal ID/title for the test record.
6. Use one disposable proposal to test **Dismiss**; refresh and confirm it no longer appears as pending.
7. On the intended proposal, click **Declare Incident**.
8. Confirm it moves from **Proposed Incident Clusters** to **Active Outages** and remains after refresh.

Expected: scanning proposes rather than silently declares an outage; a human confirms the cluster; dismissal and declaration are persisted.

If no proposal appears, record `LIMITATION` rather than manually claiming success. Detection requires enough related tickets inside its active time window and healthy semantic retrieval.

### 13.2 Inspect membership and control lifecycle

1. Select the declared incident.
2. Confirm title, component, severity, summary, linked-ticket count and affected-customer count load from the backend.
3. Copy one eligible ticket UUID from a selected ticket context and use the link-ticket control.
4. Confirm the ticket appears once; repeating the link must not duplicate it.
5. Unlink that manually added ticket and confirm impact counts are recalculated while the incident remains.
6. Move the incident through **INVESTIGATING → IDENTIFIED → MONITORING**.
7. Refresh after each transition and confirm the state persists.
8. Do not select **RESOLVED** until the communication checks below are complete.

Expected: membership is auditable, duplicate links are prevented and lifecycle changes survive reload.

### 13.3 Draft, approve and publish a customer update

1. In **Customer Communications**, draft an update with summary `Payment gateway mitigation in progress` and a customer-safe message that contains no internal IDs.
2. Confirm it is saved as `DRAFT` and is not presented as published.
3. For a HIGH/CRITICAL incident, confirm the author cannot self-approve; login as a different authorized lead/admin to approve it.
4. Click **Approve**, then **Publish** only after reviewing the exact customer-facing content.
5. Confirm the update becomes `PUBLISHED` with a publication time.
6. In Alex's session, open **My Tickets** and confirm the active-incident banner/update is visible only when Alex owns a linked ticket.
7. Return as Team Lead, transition the incident to **RESOLVED**, refresh and confirm it leaves the active list.

Expected: high-impact communication follows draft/approval/publication stages and only affected customers see it.

## 14. Part 2 Feature 2 — Policy-Controlled Resolution Actions

> **Purpose / feature:** Lets support propose sensitive operations such as a duplicate-charge refund or account unlock while policy, approval, recent authentication, idempotency and a durable worker prevent unsafe or repeated effects. Local actions use a simulator; no real money or identity provider is contacted.

Login as Sarah, open **My Queue**, select an owned ticket, and locate **Resolution Actions** in the right evidence column.

### 14.1 Propose and reject an action

1. Click the duplicate-refund proposal control.
2. Enter fictional values: account `acct-ui-001`, payment reference `pay-ui-002`, original reference `pay-ui-001`, currency `USD`, amount `4000`, and a clear rationale.
3. Click **Propose Action**.
4. Confirm the card displays normalized values, a canonical digest, risk level and `PROPOSED` or `AWAITING_APPROVAL` status.
5. Refresh and confirm the proposal persists.
6. Create a second disposable proposal, click **Reject**, and confirm it becomes `REJECTED` and cannot execute.

Expected: the UI never directly performs a refund; it creates an immutable, reviewable intent and rejection is terminal.

### 14.2 Approve and execute safely

1. Review the exact target, amount, currency and duplicate reference before approval.
2. Click **Approve** on the intended proposal. If recent authentication is required, sign out/in and retry; do not bypass the check.
3. Confirm status becomes `APPROVED`.
4. Click **Execute Action** once.
5. Confirm the response says the operation is queued or processing; refresh until it reaches `SUCCEEDED`/`RECONCILED`, or an explicit retry/manual-review state.
6. Click **Execute Action** again if the control remains available and confirm no second business effect is created.
7. Repeat with **Propose Account Unlock** using a fictional seeded user UUID and confirm the same review/approval/queued-execution behavior.

Expected: execution is asynchronous and idempotent. A timeout must not be displayed as success; it must reconcile or move to an honest failure/manual-review state.

### 14.3 Negative policy checks

1. Propose an invalid or over-policy amount and confirm approval/execution is denied with a clear reason.
2. Leave the session idle beyond the recent-authentication window, then attempt approval and confirm it fails until re-login.
3. Login as Customer or Auditor and confirm proposal, approval and execution controls are absent.

Expected: hiding controls is supplemented by backend denial; no denied attempt changes provider or business state.

## 15. Part 2 Feature 3 — Omnichannel continuity and intelligent handoff

> **Purpose / feature:** Keeps portal messages, verified email identity, internal notes and escalation context in one conversation. Customers should not repeat their story when a specialist takes over, and staff-only notes must never leak to them.

### 15.1 Verify a customer email identity

1. Login as Alex, open the Part 2 test ticket in **My Tickets**, and locate **Omnichannel Continuity**.
2. Click **Verify Email**, enter Alex's fictional seeded email, and click **Send Code**.
3. In the verification-code step, click **Load test code** inside **Local development mailbox**. Confirm the six-digit code is filled into the input.
   - This control is compiled into the frontend only in Vite development mode.
   - Its backend endpoint is registered only for the `docker` and `local` Spring profiles.
   - It requires the authenticated `CUSTOMER` who requested the still-active challenge; staff and other customers are denied.
   - Production verification responses never include the plaintext challenge.
4. Click **Confirm & Link**.
5. Confirm the card shows **Email Verified** and remains verified after refresh.
6. Try a wrong/expired code in a separate attempt and confirm it is rejected.

Expected: email is not trusted merely because a user typed it; verification creates a persisted channel identity.

### 15.2 Portal/email timeline and internal-note isolation

1. As Alex, send a new portal reply from the omnichannel card and confirm it appears with channel `PORTAL`.
2. Login as Sarah, select the same ticket and confirm the portal reply is in the unified timeline.
3. Select **Note**, enter `Internal check: validate fictional gateway request ID`, and click **Save Internal Note**.
4. Confirm it is visibly labelled **INTERNAL NOTE (Staff Only)**.
5. Return to Alex and refresh; confirm the internal note is absent.
6. As Sarah, select **Email**, send a harmless response, and confirm the timeline reports the honest delivery status. `ACCEPTED`/queued is not the same as `DELIVERED`.

Expected: all public channels share one ordered conversation while internal content remains staff-only.

### 15.3 Request and claim a human handoff

1. As Alex, click **Talk to a Person**.
2. Enter `The automated guidance did not resolve the duplicate authorization` and click **Enter Queue**.
3. Confirm handoff state becomes `QUEUED` and the summary contains the issue, verified context and actual attempted steps.
4. As Sarah, open the same ticket and click **Claim Handoff**.
5. Confirm the state changes only after assignment succeeds and persists after refresh.
6. Verify Sarah can see the prior portal/email context without asking Alex to repeat it.

Expected: the UI never claims a specialist joined before assignment succeeds, and the handoff summary is derived from persisted conversation activity.

## 16. Part 2 Feature 4 — Multimodal Evidence Lab

> **Purpose / feature:** Converts customer-provided screenshots, logs, PDFs and screen recordings into sanitized, timestamped observations for investigation. Original evidence remains protected and every privileged view requires a reason and audit record.

Login as Sarah, select an owned ticket, and locate **Multimodal Evidence Lab**.

### 16.1 Upload and analyze evidence

1. In **Attach Diagnostic Evidence**, enter `payment-error.log`, select **Log / Trace**, and paste the prepared harmless fixture text into **Content / Fixture Text**.
2. Check **Allow AI to extract error codes and sanitize diagnostic evidence**, then click **Upload & Analyze**.
3. Confirm the job progresses through explicit scanning/processing states rather than instantly showing fabricated output.
4. Refresh until the job reaches its terminal state.
5. Confirm extracted observations include relevant error text and locations where supported.
6. Confirm the sanitized view removes the fictional email/secret pattern while retaining diagnostic content such as `ERR-PAY-502`.
7. Repeat once with consent unchecked and confirm analysis does not proceed as though consent existed; then grant consent and confirm the state is updated.
8. Record binary image/PDF/MP4 extraction as `LIMITATION` for UI-only testing. Typing text while selecting those media types does not create a valid binary file and is not proof that Tesseract, pdftotext or FFmpeg works.

Expected: real adapters process supported media, failures remain failures, and raw content is not stored/displayed as an unprotected substitute for evidence processing.

### 16.2 Audited original access and deletion

1. Select processed evidence and click **Request Original (Audited)** without a reason; confirm validation rejects it.
2. Enter `Investigating OCR mismatch for ticket acceptance test` and request again.
3. Confirm authorized access displays the original and a success notice says the security audit was logged.
4. Login as a role without `EVIDENCE_VIEW_ORIGINAL` and confirm original access is absent or denied.
5. Use **Delete Evidence** on disposable evidence, confirm deletion, refresh, and verify it cannot be reopened through the UI.

Expected: original access is least-privilege and reason-audited; deletion removes user access and initiates the persisted deletion lifecycle.

## 17. Part 2 Feature 5 — Verified resolution and knowledge flywheel

> **Purpose / feature:** Measures whether the customer says a solution worked and promotes repeated, sanitized solutions into governed knowledge only after quality checks. This closes the loop between support outcomes and future retrieval.

### 17.1 Customer resolution confirmation and reopen

1. As Sarah, resolve/send the intended solution for the test ticket so the customer receives a resolution confirmation prompt.
2. As Alex, open the ticket in **My Tickets**.
3. Choose **Yes** and optionally enter a fictional reason.
4. Confirm the outcome becomes confirmed/resolved and the resolution history shows its attempt and score.
5. On a separate resolved ticket choose **Partly** or **No** and explain what remains unresolved.
6. Confirm the ticket returns to an active/in-progress state rather than being counted as a successful resolution.
7. On an eligible resolved ticket click **Reopen Ticket**, enter a reason, and confirm a new resolution attempt/history entry is retained.

Expected: customer outcome—not an agent click alone—determines verified success; partial/negative outcomes and reopen events reduce quality claims and preserve history.

### 17.2 Create, sanitize, evaluate and release knowledge

1. Login as Elena and select **Release Flywheel**.
2. Confirm metrics load from persisted resolution outcomes or explicitly show empty/unavailable.
3. Click **Propose Candidate** and enter a fictional title, category, solution content containing a deliberate fictional email, a score at least 80 and a distinct-customer count of at least 5.
4. Click **Save Candidate** and confirm it enters **Knowledge Candidates Quality Queue**.
5. Click **Sanitize PII & Secrets** and confirm the deliberate email is removed from sanitized content.
6. Click **Run Holdout Evaluation**.
7. Continue only if the UI reports a real passing evaluation. If prerequisite evidence is absent or evaluation is simulated/unavailable, record `LIMITATION`; do not claim the feature passed.
8. Click **Approve & Release** and confirm the candidate becomes `RELEASED`.
9. As Alex, search the Help Center using unique candidate wording and confirm the released knowledge is retrievable.
10. Return as Elena, click **Rollback Release**, enter a reason and **Confirm Rollback**.
11. Search again as Alex and confirm the rolled-back content is no longer active while audit/history remains.

Expected: only eligible, sanitized, evaluated and human-approved knowledge becomes searchable; rollback removes it from active retrieval without erasing governance history.

## 18. Integrated Part 1 + Part 2 showcase journey

> **Purpose / feature:** Proves the application works as one support system rather than a collection of pages. Use this as the final recruiter/demo flow after individual tests pass.

1. Customer creates a ticket with an attachment and searches self-service knowledge first.
2. Asynchronous AI triage classifies, retrieves evidence, routes and prepares a grounded draft.
3. Agent inspects citations/evidence, sends a human-approved reply and records AI feedback.
4. Multiple similar tickets cause Incident Radar to propose a cluster; Team Lead declares it and publishes an approved update.
5. Agent proposes a simulated resolution action; policy and human approval queue exactly one durable execution.
6. Customer verifies email, continues the same conversation across portal/email and requests a specialist handoff.
7. Agent processes multimodal evidence and uses sanitized observations while original access remains audited.
8. Customer confirms whether the resolution worked or reopens the ticket.
9. Knowledge Manager sanitizes and evaluates a qualified repeated solution, releases it, verifies retrieval and demonstrates rollback.
10. Administrator checks operations/AI governance; Auditor confirms immutable evidence with no mutation controls.

Pass only if identifiers and state changes persist across refresh and persona changes. A rendered card, seeded label or success toast without backend readback is not sufficient.

## 19. Honest UI and acceptance boundary

> **Purpose / feature:** Defines what the current product intentionally does not offer in the UI. Reviewing this boundary prevents placeholders, API-only operations and future roadmap items from being presented as completed features.

The Part 1 role workflows and the current Part 2 screens are wired to APIs, but UI walkthroughs alone cannot certify concurrency, crash recovery, cross-tenant denial, real semantic/media quality or delivery guarantees. These items must not be claimed from a manual UI pass:

- password reset/recovery;
- notification-center behavior behind the bell icon;
- creating/editing routing rules, teams or SLA policies (administrators can inspect and activate/deactivate rules);
- approving a sanitized resolved case through the UI (approved cases are listed and used for retrieval);
- a manual vector repair button (the authenticated reindex endpoint and lifecycle seed command provide operational repair);
- synthetic creation of failed workflows for the replay screen;
- recording the final portfolio demonstration video.
- real refunds, real account-provider unlocks or production email delivery;
- exactly-once behavior under crashes or multiple replicas;
- complete malware, OCR, PDF, audio/video or vector-quality acceptance without the automated Part 2 gates and real corpus;
- Part 2 completion while `verification/part2/final_p2_certificate.json` is `NOT_ACCEPTED`.



## 20. Final manual test record

> **Purpose / feature:** Provides traceability from each tested feature to a pass, failure or defect and its evidence. Completing this table turns an informal walkthrough into a reproducible acceptance record.

Copy this table into an issue or test report and fill it during execution.


| ID           | Scenario                                      | Persona           | Result | Evidence or defect |
| ------------ | --------------------------------------------- | ----------------- | ------ | ------------------ |
| AUTH-01      | Visible login inputs and invalid-login error  | Any               |        |                    |
| AUTH-02      | Customer registration and relogin             | New customer      |        |                    |
| AUTH-03      | Refresh session and logout                    | Any               |        |                    |
| CUST-01      | Create ticket                                 | Customer          |        | Ticket:            |
| CUST-02      | Async triage reaches READY_FOR_AGENT          | Customer          |        |                    |
| CUST-03      | View ticket and add reply                     | Customer          |        |                    |
| CUST-04      | Help Center hybrid search and article details | Customer          |        |                    |
| AGENT-01     | Load real assigned ticket and suggestion      | Agent             |        |                    |
| AGENT-02     | Accept feedback                               | Agent             |        |                    |
| AGENT-03     | Edited feedback                               | Agent             |        |                    |
| AGENT-04     | Rejected feedback with reason                 | Agent             |        |                    |
| AGENT-05     | Approve/send and WAITING_ON_CUSTOMER          | Agent/Customer    |        |                    |
| AGENT-06     | Select/filter/sort/paginate authorized queue  | Agent             |        |                    |
| FILE-01      | Clean attachment upload and download          | Customer/Agent    |        |                    |
| FILE-02      | Unsafe and unsupported attachment rejection   | Customer/Agent    |        |                    |
| RAG-01       | Long-query search returns payment article     | Customer/KM       |        |                    |
| RAG-02       | Draft/publish/supersede/rollback/archive      | Knowledge Manager |        |                    |
| LEAD-01      | Team queue, SLA scope and assignment          | Team Lead         |        |                    |
| ADMIN-01     | Persisted overview/outbox/governance          | Admin             |        |                    |
| ADMIN-02     | Routing toggle and user/role mutation         | Admin             |        |                    |
| AUDIT-01     | Read-only evidence journey and mutation 403   | Auditor           |        |                    |
| RBAC-01      | Role selector exposes only assigned roles     | All               |        |                    |
| ISOLATION-01 | Customer ticket lists remain isolated         | Two customers     |        |                    |
| DEV-01       | Frontend HMR                                  | Developer         |        |                    |
| DEV-02       | Backend automatic build/restart               | Developer         |        |                    |
| INC-01       | Detect and review incident proposal           | Team Lead         |        |                    |
| INC-02       | Incident membership and lifecycle             | Team Lead         |        |                    |
| INC-03       | Two-person update approval and publication    | Team Lead/Admin   |        |                    |
| ACT-01       | Propose and reject simulated action            | Agent             |        |                    |
| ACT-02       | Approve, queue, reconcile and deduplicate      | Agent/Admin       |        |                    |
| OMNI-01      | Verify customer email identity                | Customer          |        |                    |
| OMNI-02      | Unified timeline and internal-note isolation  | Customer/Agent    |        |                    |
| OMNI-03      | Request and claim human handoff               | Customer/Agent    |        |                    |
| EVID-01      | Analyze supported multimodal evidence         | Agent             |        |                    |
| EVID-02      | Audited original access and deletion          | Agent/Auditor     |        |                    |
| RES-01       | Confirm, reject and reopen resolution          | Customer          |        |                    |
| FLY-01       | Sanitize, evaluate, release and rollback      | Knowledge Manager |        |                    |
| FULL-01      | Integrated Part 1 + Part 2 journey            | All roles         |        |                    |




## 21. Defect-report template

> **Purpose / feature:** Standardizes the evidence needed to reproduce and fix a failed UI journey. Recording persona, identifiers, expected/actual behavior and logs allows an engineer to diagnose the correct service quickly.

```text
Title:
Test ID:
Persona/account:
Ticket/workflow ID:
Browser and OS:
Steps to reproduce:
Expected result:
Actual result:
Visible error/correlation ID:
Relevant container logs:
Screenshot/video:
Severity:
```
