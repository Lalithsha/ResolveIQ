#!/usr/bin/env python3
"""Backfill local deterministic pgvector embeddings for seeded content.

The SQL fixture intentionally stays provider-neutral. This helper is used only
by the local deterministic demo profile; production ingestion must call the
configured EmbeddingPort through rag-service instead of using this adapter.
"""

from __future__ import annotations

import hashlib
import math
import os
import re
import subprocess
import sys
from uuid import UUID


DIMENSION = 1536
TABLES = (
    "rag_schema.knowledge_chunks",
    "rag_schema.resolved_case_chunks",
)


def psql(*args: str, input_text: str | None = None) -> str:
    command = [
        "docker",
        "compose",
        "exec",
        "-T",
        "postgres",
        "psql",
        "-X",
        "-q",
        "-t",
        "-A",
        "-v",
        "ON_ERROR_STOP=1",
        "-U",
        os.environ.get("POSTGRES_USER", "resolveiq_app"),
        "-d",
        os.environ.get("POSTGRES_DB", "resolveiq"),
        *args,
    ]
    completed = subprocess.run(
        command,
        input=input_text,
        text=True,
        capture_output=True,
        check=False,
    )
    if completed.returncode != 0:
        sys.stderr.write(completed.stderr)
        raise SystemExit(completed.returncode)
    return completed.stdout


def embedding(text: str) -> list[float]:
    vector = [0.0] * DIMENSION
    for token in re.split(r"\W+", text.lower().strip()):
        if not token:
            continue
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        signed_int = int.from_bytes(digest[:4], byteorder="big", signed=True)
        vector[signed_int % DIMENSION] += 1.0
    norm = math.sqrt(sum(value * value for value in vector))
    return [value / norm for value in vector] if norm else vector


def vector_literal(values: list[float]) -> str:
    return "[" + ",".join(format(value, ".9g") for value in values) + "]"


def main() -> None:
    table_sql = ", ".join(f"'{table}'" for table in TABLES)
    rows = psql(
        "-F",
        "\t",
        "-c",
        f"""
            SELECT table_name, id, tenant_id,
                   encode(convert_to(content, 'UTF8'), 'hex')
            FROM (
                SELECT 'rag_schema.knowledge_chunks' AS table_name, id, tenant_id, content
                FROM rag_schema.knowledge_chunks WHERE embedding IS NULL
                UNION ALL
                SELECT 'rag_schema.resolved_case_chunks' AS table_name, id, tenant_id, content
                FROM rag_schema.resolved_case_chunks WHERE embedding IS NULL
            ) missing
            WHERE table_name IN ({table_sql})
            ORDER BY table_name, id
        """,
    )

    updates: list[str] = []
    for line in rows.splitlines():
        if not line.strip():
            continue
        table_name, row_id, tenant_id, content_hex = line.split("\t", 3)
        if table_name not in TABLES:
            raise ValueError(f"Unexpected table returned by database: {table_name}")
        UUID(row_id)
        UUID(tenant_id)
        content = bytes.fromhex(content_hex).decode("utf-8")
        literal = vector_literal(embedding(content))
        updates.append(
            f"UPDATE {table_name} SET embedding = '{literal}'::vector, "
            "embedding_model = 'resolveiq-deterministic-embedding-v1' "
            f"WHERE id = '{row_id}' AND tenant_id = '{tenant_id}';"
        )

    if not updates:
        print("No missing deterministic embeddings found.")
        return

    psql(input_text="BEGIN;\n" + "\n".join(updates) + "\nCOMMIT;\n")
    print(f"Backfilled {len(updates)} deterministic embedding(s).")


if __name__ == "__main__":
    main()
