# ADR 0005: Event Delivery Semantics, Transactional Outbox, and Consumer Idempotency

## Status
Accepted

## Context
Distributed state changes across `ticket-service`, `ai-orchestration-service`, and `rag-service` must not result in message loss or duplicate state transitions due to broker restarts, network partitions, or consumer crashes. Two-phase commit (2PC) / distributed transactions are prohibited.

## Decision
1. Producers use the **Transactional Outbox Pattern**: domain entities and their outbox event records are saved within the same local database transaction. A background publisher polls the `outbox_events` table and pushes to Kafka.
2. Consumers enforce **Idempotency**: each consumer stores received `eventId` in a `processed_events` table in the same transaction as its state change.
3. Ticket events are partitioned on Kafka using `ticketId` as key to ensure in-order delivery per ticket aggregate.
4. Failed events transition through retry topics to a dead-letter queue (DLQ) with audited manual replay endpoints.

## Consequences
- **Positive:** Guaranteed at-least-once delivery, exactly-once processing semantics at application level, robust failure recovery.
- **Negative:** Additional tables and database writes for outbox and processed event tracking.
- **Reversal Trigger:** N/A (Standard distributed systems invariant).
