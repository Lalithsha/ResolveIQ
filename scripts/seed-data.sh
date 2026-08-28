#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "========================================================"
echo " Seeding ResolveIQ PostgreSQL Schemas & Benchmark Fixtures"
echo "========================================================"

if command -v docker >/dev/null 2>&1 && docker compose ps --services 2>/dev/null | grep -q "postgres"; then
    echo "Applying seed fixtures to running PostgreSQL container..."
    docker compose exec -T postgres psql -U resolveiq -d resolveiq_db < "${SCRIPT_DIR}/seed-data.sql"
    echo "✔ Applied SQL seed fixtures into all 6 schemas"
else
    echo "ℹ PostgreSQL container not running; SQL seed script prepared at ${SCRIPT_DIR}/seed-data.sql"
fi

echo "Running AI evaluation runner benchmark..."
python3 "${ROOT_DIR}/evaluation/scripts/run_eval.py"

echo "========================================================"
echo "✔ ResolveIQ Seed and Evaluation Complete!"
echo "========================================================"
