# ADR 0010: Deferring Kubernetes for Portfolio and Staging Environments

## Status
Accepted

## Context
Deploying and managing a distributed microservice system with Kafka, PostgreSQL, MinIO, and OpenTelemetry in Kubernetes introduces substantial infrastructure complexity (Helm charts, Ingress controllers, Persistent Volume Claims, Cert-Manager) that can distract from validating core domain logic, asynchronous reliability, and AI evaluation metrics.

## Decision
1. Standardize Stage A (Local Portfolio) and Stage B (Staging) on **Docker Compose**.
2. Encapsulate all services, network configurations, database volumes, and health checks in `compose.yaml`.
3. Defer Kubernetes (Stage C) until all services, integration tests, and evaluation pipelines achieve 100% stability.

## Consequences
- **Positive:** Fast one-command local onboarding (`docker compose up`), reproducible test runs in CI, lower staging cloud costs.
- **Negative:** Manual scaling across multiple nodes until Kubernetes manifests are introduced in Stage C.
- **Reversal Trigger:** Transitioning to multi-node production deployment with automated horizontal autoscaling.
