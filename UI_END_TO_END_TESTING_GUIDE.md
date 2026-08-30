# ResolveIQ UI End-to-End Testing Guide

## 1. Purpose and testing rule

This guide is the manual acceptance checklist for the UI that exists in the current repository. Follow it from top to bottom to exercise every role and every user-visible control without guessing.

Record each check as `PASS`, `FAIL`, or `LIMITATION`. A `PASS` requires the expected API-backed result described here. A page that only displays sample content or an alert is explicitly marked `DEMO-ONLY` and must not be reported as a completed feature.

## 2. Start the complete local environment

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

| Component | URL | Expected result |
|---|---|---|
| Web application | `http://localhost:3300` | ResolveIQ login page |
| API gateway health | `http://localhost:18080/actuator/health` | JSON containing `"status":"UP"` |
| Service discovery | `http://localhost:8761` | Eureka dashboard |
| Kafka console | `http://localhost:8090` | Redpanda Console |
| MinIO console | `http://localhost:9001` | MinIO login page; attachment UI is not implemented |

If startup fails with `port is already allocated`, run `docker compose --profile app down` without `-v`, then use the conflict-free command above. Never add `-v` unless you intentionally want to delete local ResolveIQ data.

## 3. Load the fictional demo data

Run this once after the schemas have been created and all backend services are healthy:

```bash
./scripts/seed-data.sh
```

The seed contains only fictional data. Repeated execution is not part of this test because some generated routing/SLA fixtures may be duplicated.

### Demo accounts

All seeded accounts use the fictional password `ResolveIQ2026!`.

| Persona | Email | Role | Primary UI to test |
|---|---|---|---|
| Alex Morgan | `alex.morgan@acme.com` | CUSTOMER | Ticket creation and customer replies |
| Sarah Chen | `sarah.chen@resolveiq.local` | AGENT | Queue, AI draft, feedback, and approval |
| Marcus Vance | `marcus.vance@resolveiq.local` | TEAM_LEAD | Current role/navigation limitation |
| Elena Rostova | `elena.rostova@resolveiq.local` | KNOWLEDGE_MANAGER | Hybrid knowledge search |
| David Kross | `admin@resolveiq.local` | ADMIN | Governance shell and role restrictions |

Use a private/incognito browser window when changing personas, or click the sign-out icon before the next login.

## 4. Global authentication and layout checks

### 4.1 Login form

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

1. Sign out.
2. Click **Need a customer account? Register**.
3. Confirm the form changes to **Create customer account**.
4. Enter a fictional full name, a unique email such as `ui.customer.01@example.test`, and a password of 12–128 characters.
5. Click **Create account**.
6. Confirm the user is signed in and sees the Customer navigation.
7. Sign out, then sign in again with the new credentials.

Expected: public registration creates only a `CUSTOMER`; it cannot create an agent, administrator, or knowledge manager.

### 4.3 Refresh-session check

1. Login successfully.
2. Refresh the browser tab.
3. Confirm **Loading secure workspace…** appears briefly and the authenticated workspace returns without another login.
4. Click the sign-out icon in the top-right corner.
5. Refresh again.

Expected: refresh restores a valid session before logout; after logout the login page remains.

### 4.4 Global controls

1. Confirm the navbar shows `ResolveIQ`, `v1.0-alpha`, user name, role selector, notification icon, and sign-out icon.
2. Confirm the role selector lists only the roles returned for the current account.
3. Click the notification bell.

Expected: the bell currently has no behavior and is `DEMO-ONLY`. Do not report notifications as implemented.

## 5. Customer journey — API-backed

Login as `alex.morgan@acme.com`.

### 5.1 Create a billing ticket

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

1. Click **View My Tickets**.
2. Click **Refresh** until the new ticket appears.
3. Confirm the ticket progresses from `NEW` to `READY_FOR_AGENT`. Normally this takes a few seconds after services are warm.
4. If it remains `NEW`, inspect `docker compose logs ticket-service ai-orchestration-service` and record a failure.
5. Confirm the ticket remains visible only to the customer who created it.

### 5.3 Inspect and reply to a ticket

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

1. Select **Help Center** in the sidebar or the customer portal sub-navigation.
2. Enter `duplicate charge invoice billing dispute` and click **Search Articles**.
3. Confirm a loading state appears, followed by a real result count.
4. Confirm the seeded payment-dispute article appears when the retrieval service is healthy and indexed.
5. Open a result and confirm its title, source type, text snippet, and optional confidence score are displayed.
6. Click **Back to Results**, then **Clear Results**.
7. Click the **Billing & Invoice Disputes** popular topic and confirm it runs a pre-filled search.
8. Click **Submit a Support Ticket** in the bottom banner and confirm the create-ticket view opens.

Expected: searches call the hybrid knowledge retrieval API. Curated topic cards provide search queries; they are not hardcoded article results.

### 5.5 Customer navigation coverage

| Sidebar item | Current behavior | Result classification |
|---|---|---|
| Create Ticket | Opens the real ticket form | API-BACKED |
| My Tickets | Opens the real customer ticket list and conversation view | API-BACKED |
| Help Center | Opens hybrid knowledge search and article-result details | API-BACKED |

## 6. Agent journey — API-backed core with static context panels

Login as `sarah.chen@resolveiq.local` after completing the customer flow. The seeded ticket `RIQ-2026-000412` is also assigned to this agent and can be used if a new ticket is not yet available.

### 6.1 Load the queue and AI suggestion

1. Select **My Queue**.
2. Confirm a real ticket loads automatically.
3. Confirm the ticket number, priority, subject, and customer description correspond to the selected backend ticket.
4. Confirm the response composer contains the persisted AI suggestion when one exists.
5. Confirm **Approve & Send** is visible but no message is sent automatically.

Important distinction: the customer/team labels, SLA countdown text, AI classification badges, `94% confidence`, citation cards, and similar-case card are currently hardcoded presentation examples. The selected ticket and composer suggestion are API-backed; those context widgets are `DEMO-ONLY` until wired to their persisted fields.

### 6.2 Accept feedback

Use a ticket with a suggestion that has no previous feedback.

1. Do not edit the composer.
2. Click **Accept**.
3. Confirm `Feedback recorded: ACCEPTED` appears.
4. Click **Dismiss**.

Expected: one feedback record is stored for that suggestion. Feedback is intentionally immutable, so do not try another feedback action on the same suggestion.

### 6.3 Edited feedback

Use a different newly triaged ticket.

1. Change the composer text while preserving a safe, professional response.
2. Click **Edit** in the feedback area.
3. Confirm `Feedback recorded: EDITED` appears.

Expected: the edited content and `EDITED` action are persisted. The **Edit** button records the current composer content; it does not open a separate modal.

### 6.4 Rejected feedback

Use a third newly triaged ticket.

1. Click **Reject**.
2. Enter a reason such as `The draft makes an unsupported claim about a completed refund.`
3. Confirm `Feedback recorded: REJECTED` appears.
4. Repeat only to verify validation if desired: canceling or submitting a blank reason should not record rejection.

### 6.5 Human approval and customer send

Use a ticket that has not already been sent.

1. Review or edit the response composer.
2. Click **Approve & Send**.
3. Confirm the button changes to **Sending...**.
4. Confirm the success message states that the ticket moved to `WAITING_ON_CUSTOMER`.
5. Sign out and login as the owning customer.
6. Open **My Tickets** and confirm the ticket status is `WAITING_ON_CUSTOMER`.

Expected: ResolveIQ records feedback if needed, creates the agent’s public message, and changes ticket status only after explicit approval.

### 6.6 Agent navigation coverage

| Sidebar item | Current behavior | Result classification |
|---|---|---|
| My Queue | Loads the real agent ticket endpoint and first ticket | API-BACKED |
| Team Queue | Renders the same agent workspace; no distinct filtered list | LIMITATION |
| SLA Risk | Renders the same agent workspace; no live risk dashboard | LIMITATION |
| Knowledge Search | Opens the real hybrid retrieval screen | API-BACKED search |

Current limitation: there is no selectable queue list in the agent workspace; it automatically opens the first returned ticket.

## 7. Knowledge Manager journey

Login as `elena.rostova@resolveiq.local`.

### 7.1 Hybrid lexical/vector retrieval

1. Select **Articles & Chunks**.
2. Enter `duplicate charge payment authorization refund` in the search field.
3. Click **Search Index**.
4. Confirm the loading indicator appears.
5. Confirm a result references the payment-reconciliation article or its relevant snippet.
6. Confirm each returned row shows a title, snippet, and score.
7. Click **Clear Results**.
8. Search for a nonsense phrase and confirm either an empty result state or low/nonmatching results—not a fabricated success.

Expected: the request uses tenant-filtered PostgreSQL lexical and pgvector retrieval with reciprocal-rank fusion.

### 7.2 Knowledge controls and navigation coverage

| Control | Current behavior | Result classification |
|---|---|---|
| Search Index | Calls the real retrieval API | API-BACKED |
| Clear Results | Clears current UI results | UI-BACKED |
| New Article | Displays an informational browser alert only | DEMO-ONLY |
| Article cards | Static sample cards, not loaded from list API | DEMO-ONLY |
| Articles & Chunks | Displays Knowledge Console | PARTIAL |
| Sanitized Cases | Displays the same Knowledge Console | LIMITATION |
| Vector Indexes | Displays the same Knowledge Console | LIMITATION |

Do not claim article authoring, version publishing, sanitized-case approval, or vector-index administration through the UI yet. Backend endpoints may exist, but these controls are not connected.

## 8. Administrator journey

Login as `admin@resolveiq.local`.

### 8.1 Governance screen

1. Select **Operations Overview**.
2. Confirm the page heading is **AI Governance & Operations**.
3. Confirm the warning explicitly states that metric cards are demonstration targets.
4. Inspect Recall@5, MRR, Auto-Send Rate, and Kafka Outbox Lag.

Expected: these cards are `DEMO-ONLY`; they are not loaded from live metric APIs.

### 8.2 Failed-workflow replay control

1. Locate **Dead-Letter Queue (DLQ) & Failed Workflows**.
2. Observe the displayed workflow `wf-8a9b2c1d`.
3. Click **Retry Workflow**.

Expected current result: the row is static and its identifier is not a real UUID, so the API should return a visible error. A success must not be recorded unless the table is first connected to `listFailedWorkflows()` and supplies a real persisted workflow UUID.

### 8.3 Administrator navigation coverage

| Sidebar item | Current behavior | Result classification |
|---|---|---|
| Operations Overview | Governance demonstration page | DEMO/PARTIAL |
| All Tickets | Falls through to Agent Workspace | LIMITATION |
| Teams & Routing | Falls through to Agent Workspace | LIMITATION |
| Knowledge Base | Opens Knowledge Console with real search | API-BACKED search only |
| AI Governance & Eval | Governance demonstration page | DEMO/PARTIAL |
| System Settings | Falls through to Agent Workspace | LIMITATION |

The model-invocation table is static demonstration data. User administration, live routing configuration, live metrics, audit search, and system settings do not currently have connected UI screens.

## 9. Team Lead and unsupported-role checks

Login as `marcus.vance@resolveiq.local`.

1. Confirm authentication succeeds with the `TEAM_LEAD` role.
2. Inspect the sidebar.
3. Click each available item.

Current result: `TEAM_LEAD` has no dedicated sidebar case and falls into the administrator-style navigation, while the content renderer usually falls into Agent Workspace. This is a known navigation mismatch and must be recorded as `LIMITATION`, not passed as a completed team-lead workflow.

`AUDITOR` exists in frontend types but has no seeded account and no dedicated UI. Auditor testing is therefore `NOT AVAILABLE`.

## 10. Authorization-negative tests through the UI

1. Login as a customer and confirm the role selector does not offer AGENT, KNOWLEDGE_MANAGER, or ADMIN.
2. Login as an agent and confirm CUSTOMER/ADMIN roles are not available unless explicitly assigned.
3. Login as the knowledge manager and attempt normal knowledge search; confirm it succeeds.
4. Logout between personas and confirm one persona’s session is not reused as another.
5. Open two incognito windows with different personas and confirm customer ticket lists do not leak across accounts.

The SPA has no URL router with protected deep links, so endpoint-level forbidden checks cannot all be performed by navigation alone. Use backend integration tests or an API client for exhaustive `401`, `403`, and cross-tenant checks.

## 11. Hot-reload acceptance checks

Perform these only after the development stack was built once.

### Frontend

1. Keep `http://localhost:3300` open.
2. Change a visible label under `frontend/src`.
3. Save the file.
4. Confirm the browser updates without a Compose command or container restart.

### Backend

1. Run `docker compose logs -f ticket-service`.
2. Make a compilable change under `ticket-service/src/main` and save.
3. Confirm logs contain `[dev-reload] Source change detected for ticket-service`.
4. Confirm Maven compiles the module and only ticket-service restarts.
5. Introduce a temporary compilation error and save.
6. Confirm the build fails while the last successful process remains running.
7. Correct the error and save.
8. Confirm automatic compilation and restart succeed.

Changes to `common-contracts` or `common-security` intentionally trigger all watching backend services. Changes to Compose, Dockerfiles, ports, container environment variables, or frontend dependencies still require a rebuild.

## 12. Features that cannot currently be fully tested through UI

The following must remain open backlog items even if their backend schemas or partial endpoints exist:

- ticket attachments and MinIO upload/download;
- password reset;
- full agent-side message timeline rendering (the customer timeline is implemented);
- selectable agent/team queues and assignment controls;
- live SLA-risk dashboard;
- article authoring and version publishing;
- sanitized resolved-case approval;
- vector-index administration;
- real failed-workflow list and replay selection;
- live governance/evaluation/trace metrics;
- administrator user, team, routing-rule, and settings screens;
- notification center;
- dedicated Team Lead and Auditor experiences.

## 13. Final manual test record

Copy this table into an issue or test report and fill it during execution.

| ID | Scenario | Persona | Result | Evidence or defect |
|---|---|---|---|---|
| AUTH-01 | Visible login inputs and invalid-login error | Any |  |  |
| AUTH-02 | Customer registration and relogin | New customer |  |  |
| AUTH-03 | Refresh session and logout | Any |  |  |
| CUST-01 | Create ticket | Customer |  | Ticket: |
| CUST-02 | Async triage reaches READY_FOR_AGENT | Customer |  |  |
| CUST-03 | View ticket and add reply | Customer |  |  |
| CUST-04 | Help Center hybrid search and article details | Customer |  |  |
| AGENT-01 | Load real assigned ticket and suggestion | Agent |  |  |
| AGENT-02 | Accept feedback | Agent |  |  |
| AGENT-03 | Edited feedback | Agent |  |  |
| AGENT-04 | Rejected feedback with reason | Agent |  |  |
| AGENT-05 | Approve/send and WAITING_ON_CUSTOMER | Agent/Customer |  |  |
| RAG-01 | Hybrid search returns payment article | Knowledge Manager |  |  |
| ADMIN-01 | Governance static/live distinction | Admin |  |  |
| ADMIN-02 | Invalid static replay surfaces error | Admin |  |  |
| RBAC-01 | Role selector exposes only assigned roles | All |  |  |
| ISOLATION-01 | Customer ticket lists remain isolated | Two customers |  |  |
| DEV-01 | Frontend HMR | Developer |  |  |
| DEV-02 | Backend automatic build/restart | Developer |  |  |

## 14. Defect-report template

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
