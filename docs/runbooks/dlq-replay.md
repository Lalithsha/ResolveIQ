# Runbook: Dead Letter Queue (DLQ) Audited Replay

## Objective
Safely reprocess failed AI triage events that were routed to the Dead Letter Queue without causing duplicate state transitions or message corruption.

---

## 1. Safety Invariants
1. **Idempotency Check:** Target consumers must check `processed_events` table before applying state updates.
2. **Correlation Context:** Replayed messages maintain original `correlation_id` with an appended header `X-Replay-Count`.
3. **Audit Trail:** Every replay event is logged with operator identity, timestamp, and reason.

---

## 2. Replay Procedure

### Step 1: Inspect DLQ Messages
```bash
# Query dead outbox events from orchestration service
SELECT id, aggregate_id, event_type, retry_count, created_at 
FROM orchestration_schema.outbox_events 
WHERE status = 'DEAD';
```

### Step 2: Trigger Retry for Specific Failed Workflow
```bash
curl -X POST http://localhost:8083/api/v1/workflows/{workflowId}/retry \
  -H "Content-Type: application/json" \
  -H "X-Operator-Id: ops-admin-01" \
  -d '{"reason": "Downstream AI service recovered"}'
```

### Step 3: Monitor Recovery
Verify that the workflow step completes and emits `TicketTriageCompleted.v1`.
