# ADR 0006: Service and Data Ownership Boundaries

## Status
Accepted

## Context
Microservice architectures frequently suffer from tight coupling when services directly read or write foreign databases or share common JPA entities.

## Decision
Each microservice is the sole owner of its database schema:
- `auth-service` -> `auth_schema`
- `ticket-service` -> `ticket_schema`
- `ai-orchestration-service` -> `orchestration_schema`
- `ai-analysis-service` -> `analysis_schema`
- `routing-service` -> `routing_schema`
- `rag-service` -> `rag_schema`

Direct cross-schema queries or foreign keys between service schemas are strictly prohibited. Inter-service data sharing occurs via versioned Kafka events or bounded REST APIs. `common-contracts` contains only event envelopes, problem DTOs, and tracing utilities—no JPA entities or database repositories.

## Consequences
- **Positive:** High autonomy, isolated schema evolution via independent Flyway migrations, clear bounded contexts.
- **Negative:** Eventual consistency across read models.
- **Reversal Trigger:** None.
