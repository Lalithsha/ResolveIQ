# ResolveIQ Part 2 Verification & Gate Acceptance Suite

This directory contains the automated acceptance harness, test runners, and gate specifications for **ResolveIQ Part 2** according to the requirements in `RESOLVEIQ_PART2_IMPLEMENTATION_PLAN.md` (Sections 22 & 23).

---

## 1. Gate Architecture (G0 – G5)

| Gate | Title | Target Subsystems | Key Invariants Verified |
|---|---|---|---|
| **G0** | Security & Integrity | `auth-service`, `common-security`, all services | Auditors are strictly read-only (all mutations rejected with 403); step-up auth within 5 min required for high-risk mutations; immutable `auth_time` carried through refresh token rotations; zero fallback tenant IDs (fail-closed); tenant feature flags default off. |
| **G1** | Incident Radar | `ticket-service` | 15-minute sliding window detector with configurable thresholds (default 10 tickets, 8 distinct customers); safe customer banners (only `PUBLISHED` customer updates visible, zero leak of internal drafts/proposals); unlinking tickets recomputes customer impact. |
| **G2** | Resolution Actions | `ai-orchestration-service` | Two-person rule (proposer cannot approve own high-risk action); 5-minute step-up authentication validity; idempotency check returning HTTP 409 conflict when request input changes; execution decoupled from database transaction. |
| **G3** | Omnichannel Continuity | `ticket-service`, `frontend` | Verification challenge tokens strictly removed from API responses and server logs; MailboxSimulator test access; max 5 verification attempts before invalidating challenge; single canonical ticket-message store. |
| **G4** | Real Evidence Pipeline | `ai-analysis-service` | Binary byte preservation (Base64 storage for 100% exact byte round-trip); magic-byte verification (PNG, PDF, prohibited archives); truthful extraction adapters (no fabricated SAML/00:42 for clean files); tombstone deletion. |
| **G5** | Knowledge Flywheel | `rag-service`, `ticket-service` | Centralized `TicketStatus.RESOLVED` transitions into versioned `ResolutionAttempt`s; outcome scoring (+100 YES, +20 PARTLY, -100 NO); evaluation runner over benchmark queries testing Recall@5 >= 0.85 & MRR >= 0.75 (degraded candidates fail); genuine `KnowledgeDocument` & `KnowledgeChunk` indexing upon release; real rollback reverting RAG search. |

---

## 2. Prerequisites

- **Java Development Kit:** JDK 21+ (verified with OpenJDK 21/26)
- **Maven:** Maven 3.9+ (use included `./mvnw`)
- **Node.js:** Node.js 18+ and npm
- **Docker:** Docker & Docker Compose (optional for local mock/isolated stack, verified via unit/integration test harnesses)

---

## 3. Running the Verification Gates

Execute the master gate runner from the project root:

```bash
chmod +x verification/part2/run-gates.sh
./verification/part2/run-gates.sh
```

The runner will:
1. Validate environment prerequisites and git commit SHA.
2. Execute automated test suites mapped directly to G0 through G5.
3. Validate frontend builds and type contracts.
4. Output a machine-readable results summary to `verification/part2/part2_gate_results.json`.
5. Exit with code `0` only if all gates pass.

---

## 4. Output Artifacts

- **Summary JSON:** `verification/part2/part2_gate_results.json`
- **Verification Report:** Artifact `RESOLVEIQ_PART2_VERIFICATION_REPORT.md`
