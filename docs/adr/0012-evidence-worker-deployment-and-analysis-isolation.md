# ADR 0012: Evidence Worker Deployment Profile and Analysis Isolation

## Status
Accepted

## Context
Multimodal evidence processing (optical character recognition, document parsing via Tika, log pattern extraction, and audio/video frame sampling) involves high CPU and memory utilization and processes untrusted user uploads. Running these intensive workloads inside the core synchronous web applications creates thread starvation, memory spikes, and security blast-radius risks.

## Decision
We deploy the evidence analysis pipeline as an asynchronous background worker profile (`worker`) derived from the `ai-analysis-service` codebase.
1. The ticket-service quarantines raw uploads and verifies customer consent before emitting `resolveiq.evidence.processing_requested`.
2. Analysis workers consume processing jobs using database leases or partitioned Kafka consumer groups with strict resource limits (CPU/memory throttling).
3. Raw files are inspected only within sandboxed extraction routines without external network egress.
4. Extracted text and media undergo deterministic sanitization and PII/secret redaction.
5. Only sanitized artifacts and observations are emitted back via `resolveiq.evidence.processing_completed` or indexed into RAG.

## Consequences
- **Positive:** Isolates resource-heavy parsers from synchronous API endpoints; protects core services from decompression or CPU exhaustion attacks; limits external exposure of raw unredacted evidence.
- **Negative:** Adds deployment profile configuration; necessitates job leasing and reconciliation mechanisms for worker crashes.
- **Reversal Trigger:** If all evidence processing moves to dedicated serverless functions or sandboxed third-party SaaS endpoints.
