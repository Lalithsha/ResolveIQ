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


| Component          | URL                                      | Expected result                                    |
| ------------------ | ---------------------------------------- | -------------------------------------------------- |
| Web application    | `http://localhost:3300`                  | ResolveIQ login page                               |
| API gateway health | `http://localhost:18080/actuator/health` | JSON containing `"status":"UP"`                    |
| Service discovery  | `http://localhost:8761`                  | Eureka dashboard                                   |
| Kafka console      | `http://localhost:8090`                  | Redpanda Console                                   |
| MinIO console      | `http://localhost:9001`                  | MinIO login page; clean ticket attachments are stored here |


If startup fails with `port is already allocated`, run `docker compose --profile app down` without `-v`, then use the conflict-free command above. Never add `-v` unless you intentionally want to delete local ResolveIQ data.

## 3. Load the fictional demo data

Run this once after the schemas have been created and all backend services are healthy:

```bash
./scripts/seed-data.sh
```

The seed contains only fictional data and uses deterministic identifiers so it can be rerun safely. Relational fixtures are upserted, and knowledge articles are created/published through the authenticated lifecycle API instead of inserting incomplete chunks directly.

The seed command verifies lifecycle indexing and deterministic embeddings for local knowledge. If Help Center search returns zero results after a reset, rerun `./scripts/seed-data.sh`, confirm that lifecycle ingestion completes, and treat any remaining zero-result response as a defect.

### Demo accounts

All seeded accounts use the fictional password `ResolveIQ2026!`.


| Persona       | Email                           | Role              | Primary UI to test                      |
| ------------- | ------------------------------- | ----------------- | --------------------------------------- |
| Alex Morgan   | `alex.morgan@acme.com`          | CUSTOMER          | Ticket creation and customer replies    |
| Sarah Chen    | `sarah.chen@resolveiq.local`    | AGENT             | Queue, AI draft, feedback, and approval |
| Marcus Vance  | `marcus.vance@resolveiq.local`  | TEAM_LEAD         | Current role/navigation limitation      |
| Elena Rostova | `elena.rostova@resolveiq.local` | KNOWLEDGE_MANAGER | Hybrid knowledge search                 |
| David Kross   | `admin@resolveiq.local`         | ADMIN             | Governance shell and role restrictions  |
| Priya Nair    | `auditor@resolveiq.local`       | AUDITOR           | Read-only evidence and governance        |


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
2. Click **Create account**.
3. Confirm the heading changes to **Create your account**.
4. Enter a fictional full name, a unique email such as `ui.customer.01@example.test`, and a password of 12–128 characters.
5. Click **Create account**.
6. Confirm the user is signed in and sees the Customer navigation.
7. Sign out, then sign in again with the new credentials.

Expected: public registration creates only a `CUSTOMER`; it cannot create an agent, administrator, or knowledge manager.

### 4.3 Refresh-session check

1. Login successfully.
2. Refresh the browser tab.
3. Confirm **Preparing your workspace…** appears briefly and the authenticated workspace returns without another login.
4. Click the sign-out icon in the top-right corner.
5. Refresh again.

Expected: refresh restores a valid session before logout; after logout the login page remains.

### 4.4 Global controls

1. Confirm the navbar shows `ResolveIQ`, `Alpha`, user name, role selector, notification icon, and sign-out icon.
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
4. Confirm the seeded payment-dispute article **Payment Reconciliation & Duplicate Charge Handling** appears when the retrieval service is healthy and indexed.
5. Open a result and confirm its title, source type, text snippet, and optional confidence score are displayed.
6. Click **Back to Results**, then **Clear Results**.
7. Click the **Billing & Invoice Disputes** popular topic and confirm it runs a pre-filled search.
8. Click **Submit a Support Ticket** in the bottom banner and confirm the create-ticket view opens.

Expected: searches call the hybrid knowledge retrieval API. Curated topic cards provide search queries; they are not hardcoded article results.

### 5.5 Customer navigation coverage


| Sidebar item  | Current behavior                                          | Result classification |
| ------------- | --------------------------------------------------------- | --------------------- |
| Create Ticket | Opens the real ticket form                                | API-BACKED            |
| My Tickets    | Opens the real customer ticket list and conversation view | API-BACKED            |
| Help Center   | Opens hybrid knowledge search and article-result details  | API-BACKED            |




## 6. Agent journey — fully API-backed workspace

Login as `sarah.chen@resolveiq.local` after completing the customer flow. The seeded ticket `RIQ-2026-000412` is also assigned to this agent and can be used if a new ticket is not yet available.

### 6.1 Load the queue and AI suggestion

1. Select **My Queue**.
2. Confirm the paginated queue lists only tickets assigned to Sarah and does not auto-open an unauthorized ticket.
3. Select two different rows and confirm the center panel changes to the chosen persisted ticket.
4. Use search, priority, status and sort controls; confirm the list and total count update.
5. Confirm ticket number, priority, status, customer, team/assignee, first-response SLA, messages and attachments correspond to the selected backend ticket.
6. Confirm classification badges, confidence, provider/prompt metadata, citations and similar cases show persisted values or an explicit `No ...` state—never sample values.
7. Confirm the response composer contains the persisted AI suggestion when one exists.
8. Confirm **Approve & Send** is visible but no message is sent automatically.

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


| Sidebar item     | Current behavior                                            | Result classification |
| ---------------- | ----------------------------------------------------------- | --------------------- |
| My Queue         | Selectable, filterable assigned-ticket queue                | API-BACKED            |
| Team Queue       | Team-scoped selectable queue                                | API-BACKED            |
| SLA Risk         | Deadline-sorted at-risk queue within authorized scope       | API-BACKED            |
| Knowledge Search | Opens the real hybrid retrieval screen                      | API-BACKED search     |

### 6.7 Secure attachment exchange

1. Open an owned ticket and click **Attach**.
2. Upload a small `.txt`, `.json`, `.png`, `.jpg`, or `.pdf` file containing only fictional data.
3. Confirm **Scanning…** appears and the clean attachment is listed with status `CLEAN`.
4. Click the attachment and confirm the downloaded SHA/content matches the original.
5. Attempt an unsupported executable/archive and confirm it is rejected without adding a row.
6. Use the standard EICAR test string only in an isolated local test file; confirm the upload returns a malware rejection and is not downloadable.

Expected: the server normalizes the name, validates size/type/magic bytes, computes SHA-256, scans before storage, generates the object key, and authorizes list/download through tenant and ticket scope.

## 7. Knowledge Manager journey

Login as `elena.rostova@resolveiq.local`.

### 7.1 Article lifecycle

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

Login as `admin@resolveiq.local`.

### 8.1 Operations and governance

1. Select **Overview** and confirm the heading is **Operations overview**.
2. Confirm tenant users, active routing rules, AI invocations, ticket/workflow outbox pending/dead totals, security events and failed-workflow count come from APIs.
3. If a data source is unavailable, confirm the UI says `Unavailable` instead of displaying a sample number.
4. If a real failed workflow exists, click **Retry** and confirm it disappears only after the replay command is accepted. Do not manufacture a failure solely for this manual smoke test.
5. Select **AI governance** and verify totals, valid-output rate, guardrail blocks, estimated cost and recent sanitized model traces.
6. Confirm raw provider output, API keys and unredacted sensitive input are absent.

### 8.2 Routing and user administration

1. Select **Teams & routing** and confirm persisted teams, capacities, rules and SLA policies are listed.
2. Toggle one routing rule inactive, refresh, and confirm the state persists. Restore its original state and refresh again.
3. Select **Users & roles** and create a fictional staff user with a 12+ character temporary password and a non-customer role.
4. Confirm the new user appears in the tenant list.
5. Change that user's primary role and confirm readback after refresh.
6. Confirm the current administrator cannot change their own role from this table.

### 8.3 Administrator navigation coverage


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

Login as `marcus.vance@resolveiq.local`.

1. Confirm authentication succeeds with only the `TEAM_LEAD` role.
2. Open **Team queue** and verify only Marcus's authorized team is selectable.
3. Select a ticket and assign/reassign its team or agent. Refresh and confirm persistence.
4. Open **SLA risk** and confirm the authorized at-risk projection and SLA ordering.
5. Open **Knowledge** and confirm retrieval works without knowledge-publication controls.

Sign out and login as `auditor@resolveiq.local`.

1. Confirm the distinct sidebar contains **Security audit**, **Ticket evidence**, **Workflow audit**, and **AI governance**.
2. Confirm security events, all-tenant ticket evidence, workflows and sanitized model traces load from APIs.
3. Confirm assignment, retry, routing-toggle, user-creation, role-change, knowledge-publication, feedback and send controls are absent.
4. Use an API client for one auditor mutation request and confirm backend `403`; hidden UI alone is not authorization proof.

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

## 12. Honest UI boundary after Part 1

The Part 1 role workflows, queues, attachments, lifecycle, routing, governance and audit views are API-backed. These items remain outside the current UI and must not be claimed:

- password reset/recovery;
- notification-center behavior behind the bell icon;
- creating/editing routing rules, teams or SLA policies (administrators can inspect and activate/deactivate rules);
- approving a sanitized resolved case through the UI (approved cases are listed and used for retrieval);
- a manual vector repair button (the authenticated reindex endpoint and lifecycle seed command provide operational repair);
- synthetic creation of failed workflows for the replay screen;
- recording the final portfolio demonstration video.



## 13. Final manual test record

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
