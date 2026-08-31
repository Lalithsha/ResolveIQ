#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "========================================================"
echo " Seeding ResolveIQ PostgreSQL Schemas & Benchmark Fixtures"
echo "========================================================"

if command -v docker >/dev/null 2>&1 && docker compose ps --services 2>/dev/null | grep -q "postgres"; then
    echo "Applying seed fixtures to running PostgreSQL container..."
    docker compose exec -T postgres psql -U "${POSTGRES_USER:-resolveiq_app}" -d "${POSTGRES_DB:-resolveiq}" < "${SCRIPT_DIR}/seed-data.sql"
    echo "Waiting for the gateway before lifecycle-based knowledge ingestion..."
    GATEWAY_ROOT="${RESOLVEIQ_SEED_API_ROOT:-http://localhost:8080/api/v1}"
    GATEWAY_ROOT="${GATEWAY_ROOT%/api/v1}"
    for attempt in $(seq 1 30); do
        if curl --fail --silent "${GATEWAY_ROOT}/actuator/health" -o /dev/null 2>/dev/null; then break; fi
        [[ "${attempt}" -eq 30 ]] && { echo "Gateway did not become reachable for knowledge ingestion" >&2; exit 1; }
        sleep 2
    done
    "${SCRIPT_DIR}/seed-knowledge-via-api.sh"
    echo "✔ Applied relational fixtures and lifecycle-indexed knowledge"
else
    echo "ℹ PostgreSQL container not running; SQL seed script prepared at ${SCRIPT_DIR}/seed-data.sql"
fi

echo "Running AI evaluation runner benchmark..."
python3 "${ROOT_DIR}/evaluation/scripts/run_eval.py"

echo "========================================================"
echo "✔ ResolveIQ Seed and Evaluation Complete!"
echo "========================================================"
