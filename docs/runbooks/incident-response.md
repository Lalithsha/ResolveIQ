# Runbook: Incident Response & Operational Triage

## Overview
This runbook guides operators through identifying, isolating, and resolving incidents across the ResolveIQ microservice platform.

---

## 1. High-Priority Alerts & Actions

### Alert: `AiOrchestrationDlqHighLag`
- **Symptom:** AI triage DLQ topic (`resolveiq.ticket.triage_failed`) message count exceeds threshold (> 10 items in 5m).
- **Diagnosis:**
  1. Inspect Kafka consumer group lag:
     ```bash
     docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group resolveiq-orchestration-service
     ```
  2. Query `workflow_instances` in `orchestration_schema` for status `FAILED`:
     ```sql
     SELECT id, ticket_id, current_step, updated_at FROM orchestration_schema.workflow_instances WHERE status = 'FAILED' ORDER BY updated_at DESC LIMIT 20;
     ```
- **Remediation:**
  1. Identify downstream failure (`ai-analysis-service`, `routing-service`, or `rag-service`).
  2. If upstream provider had an outage, trigger DLQ replay via [DLQ Replay Runbook](dlq-replay.md).

---

### Alert: `PostgreSqlVectorSearchLatencyHigh`
- **Symptom:** `rag-service` p95 retrieval latency > 500ms.
- **Diagnosis:**
  ```sql
  EXPLAIN ANALYZE SELECT id FROM rag_schema.knowledge_chunks ORDER BY embedding <=> '[0.1, ...]' LIMIT 5;
  ```
- **Remediation:**
  1. Re-index HNSW/IVFFlat index if row count increased significantly:
     ```sql
     REINDEX INDEX CONCURRENTLY rag_schema.idx_knowledge_chunks_embedding;
     ```

---

### Alert: `AuthenticationTokenReuseDetected`
- **Symptom:** Security audit event `REFRESH_REUSE_DETECTED` triggered.
- **Diagnosis:** A client presented an already-revoked refresh token.
- **Action:**
  1. All active refresh tokens for the user ID were automatically revoked by `AuthService`.
  2. Audit log entry recorded with originating IP address and User Agent.
  3. Contact user to confirm account security.
