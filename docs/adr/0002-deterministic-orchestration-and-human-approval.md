# ADR 0002: Deterministic Orchestration and Human-in-the-Loop Approval

## Status
Accepted

## Context
Fully autonomous LLM agents are non-deterministic and risk generating hallucinated policy claims, incorrect routing, or unauthorized customer commitments (such as refunds or cancellations). In a customer-support context, business operations must adhere to strict deterministic business logic and auditability.

## Decision
1. Business logic (ticket state transitions, routing eligibility, SLA clock calculations) is strictly deterministic and implemented in Java/Spring services.
2. AI models are used solely to assist: extracting structured facts (intent, sentiment, urgency), retrieving knowledge, and generating suggested response drafts.
3. Every AI-generated draft requires explicit human review and approval by an authorized support agent before being dispatched to the customer. Zero auto-send paths exist.

## Consequences
- **Positive:** Guaranteed policy compliance, zero hallucinated auto-responses, complete audit trail for compliance.
- **Negative:** Human agent review is required for all customer responses.
- **Reversal Trigger:** Specific low-risk intents may receive auto-reply only after statistical qualification on >10,000 cases with 99.9% measured accuracy.
