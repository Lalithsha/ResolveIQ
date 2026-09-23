# ResolveIQ Live Benchmark Results

Generated on 22 September 2026. The machine-readable evidence is stored in
`live_retrieval_benchmark.json` and `live_action_benchmark.json`.

## Retrieval benchmark

The benchmark exercised the running Spring Boot retrieval API backed by
PostgreSQL and pgvector. It used 100 frozen synthetic queries mapped to 20
knowledge articles. Embeddings were produced locally by the trained
`sentence-transformers/all-MiniLM-L6-v2` model. Its normalized 384-dimensional
vectors were zero-padded to the existing 1,536-dimensional database schema;
zero-padding preserves cosine similarity.

| Strategy | Recall@5 | MRR | Server p95 | End-to-end p95 |
|---|---:|---:|---:|---:|
| PostgreSQL full-text search | 100% | 0.9037 | 2 ms | 15.70 ms |
| Trained vectors only | 100% | 0.9553 | 16 ms | 42.43 ms |
| Hybrid RRF | 100% | 0.9725 | 11 ms | 19.27 ms |

Hybrid retrieval improved MRR by **7.62%** over full-text search while retaining
100% Recall@5. This means relevant articles ranked higher, even though both
strategies placed a relevant article somewhere in the top five for every case.

This is a local benchmark on a small frozen synthetic corpus. It is evidence of
the implemented retrieval path and relative ranking quality, not production
traffic performance.

## Resolution-action benchmark

The benchmark exercised the implemented refund and account-unlock action,
validation, policy, provider-adapter, idempotency, and reconciliation code.

| Outcome | Observed result |
|---|---:|
| Invalid actions denied before approval | 100/100 (100%) |
| Successful executions reconciled to observed state | 100/100 (100%) |
| Duplicate retries completed without a repeated provider effect | 100/100 (100%) |

The payment and identity providers and persistence repositories were simulated.
The results prove deterministic behavior across the tested fault matrix; they do
not represent production provider reliability or measured human approval-time
savings.

## Reproduction

Create an isolated Python environment and install the local trained model. The
first installation/model load downloads dependencies and model weights; no API
key or paid service is required.

```bash
python3 -m venv /tmp/resolveiq-benchmark-venv
source /tmp/resolveiq-benchmark-venv/bin/activate
pip install -r evaluation/requirements-live-benchmark.txt
python evaluation/scripts/local_embedding_server.py --host 0.0.0.0
```

In another terminal, recreate only the RAG service against the benchmark
endpoint and run the frozen evaluation:

```bash
RESOLVEIQ_AI_EMBEDDING_PROVIDER=openai-compatible \
RESOLVEIQ_AI_EMBEDDING_MODEL=sentence-transformers/all-MiniLM-L6-v2 \
RESOLVEIQ_AI_EMBEDDING_DIMENSION=1536 \
RESOLVEIQ_AI_BASE_URL=http://host.docker.internal:18091/v1 \
RESOLVEIQ_AI_API_KEY=local-benchmark-key \
docker compose --profile app up -d --no-deps --force-recreate rag-service

python3 evaluation/scripts/run_live_retrieval_benchmark.py \
  --provider local-sentence-transformers \
  --model sentence-transformers/all-MiniLM-L6-v2

./mvnw -pl ai-orchestration-service -am \
  -Dtest=ResolutionActionBenchmarkTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Djacoco.skip=true \
  -Dresolveiq.benchmark.report="$PWD/evaluation/reports/live_action_benchmark.json" \
  test
```

Stop the local model server with `Ctrl+C`. Recreate `rag-service` without the
temporary environment overrides to return to the provider configured in your
normal `.env` file.
