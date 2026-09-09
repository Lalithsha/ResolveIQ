# ADR 0013: Canonical Action Approval Digest and Policy-Controlled Execution

## Status
Accepted

## Context
Allowing agents to trigger external mutations (such as duplicate-charge refunds or account unlocks) from the support workspace introduces security risks: prompt injection from customer text, parameter tampering, race conditions, replay attacks, and duplicate financial transactions. Model outputs must never directly invoke mutation APIs.

## Decision
We establish a policy-controlled human approval boundary bound to a canonical action approval digest:
1. AI may only propose typed actions and extract candidate arguments into normalized JSON schemas.
2. An authoritative policy engine evaluates deterministic rules (agent financial limits, recent identity verification, fraud flags).
3. Actions requiring approval compute a SHA-256 canonical digest over:
   - Tenant ID, Ticket ID, Proposal ID, Action Type
   - Normalized input attributes in sorted-key canonical JSON (minor integer currency units)
   - Authoritative provider state version and Policy version
   - Required approval roles and Expiration timestamp
4. Approvals are cryptographically and referentially bound to this exact digest. Any material alteration to inputs, policy, or underlying provider state invalidates the approval.
5. Execution requires an approved digest, fresh policy validation, and an idempotency key.
6. Execution queries provider idempotency state and performs immediate reconciliation, entering `EXECUTION_UNKNOWN` or `MANUAL_REVIEW` if an ambiguous result occurs rather than blindly retrying.

## Consequences
- **Positive:** Mathematically prevents prompt-injection attacks from bypassing policy; eliminates double-execution; ensures complete auditability.
- **Negative:** Adds validation overhead and requires strict canonical serialization across backend layers.
- **Reversal Trigger:** None; deterministic policy and approval bindings are mandatory invariants for governed operations.
