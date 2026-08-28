# ADR 0001: Asynchronous AI Processing

## Status
Accepted

## Context
Ticket creation and message handling are customer-facing operations that require sub-second latency (p95 ≤ 500 ms). Large language model inferences, embedding computations, and vector searches often take 2 to 10+ seconds and are subject to transient provider latency spikes or rate limiting. Holding HTTP requests open during AI analysis harms availability, exhausts thread pools, and leads to poor customer experience.

## Decision
We decouple customer ticket ingestion from AI processing using Kafka events and transactional outbox. The `ticket-service` persists the ticket, publishes a `TicketCreated.v1` outbox event, and immediately returns a 201 Created response to the caller. The `ai-orchestration-service` consumes the event asynchronously, orchestrates classification and hybrid retrieval, and emits a `TicketTriageCompleted.v1` event when finished.

## Consequences
- **Positive:** Fast HTTP response times; resilience against third-party AI outages; independent scaling of AI workers.
- **Negative:** UI must handle asynchronous state transitions (e.g. `aiTriageStatus: PENDING`).
- **Reversal Trigger:** If all AI models achieve sub-50ms deterministic local inference with 99.99% availability.
