#!/usr/bin/env python3
"""Benchmark the live Spring Boot/pgvector retrieval API.

Seeds the frozen knowledge corpus through public APIs, reindexes it with the
configured application embedding provider, and compares FTS_ONLY, VECTOR_ONLY,
and HYBRID_RRF using the same 100-query ground-truth dataset.
"""
from __future__ import annotations

import argparse
import json
import os
import platform
import statistics
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def request(base: str, method: str, path: str, token: str | None = None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        detail = error.read().decode(errors="replace")
        raise RuntimeError(f"{method} {path} returned {error.code}: {detail}") from error


def percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(len(ordered) * fraction))]


def ensure_corpus(base: str, token: str, articles: list[dict]) -> None:
    existing = {item["title"]: item for item in request(base, "GET", "/knowledge/articles", token)}
    for article in articles:
        document = existing.get(article["title"])
        if document is None:
            document = request(base, "POST", "/knowledge/articles", token, {
                "title": article["title"], "category": article["category"],
                "product": "ResolveIQ Benchmark", "language": "en",
                "content": article["content"],
                "summary": f"Frozen benchmark article {article['id']}"
            })
        versions = request(base, "GET", f"/knowledge/articles/{document['id']}/versions", token)
        if any(version["status"] == "PUBLISHED" for version in versions):
            continue
        review = next((version for version in versions if version["status"] == "IN_REVIEW"), None)
        if review is None:
            draft = next(version for version in versions if version["status"] == "DRAFT")
            review = request(base, "POST",
                f"/knowledge/articles/{document['id']}/versions/{draft['id']}/submit", token)
        request(base, "POST",
            f"/knowledge/articles/{document['id']}/versions/{review['id']}/publish", token,
            {"note": "Approved frozen live-retrieval benchmark corpus"})
    request(base, "POST", "/knowledge/admin/reindex-missing", token)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-root", default=os.getenv("RESOLVEIQ_BENCHMARK_API_ROOT", "http://localhost:18080/api/v1"))
    parser.add_argument("--email", default=os.getenv("RESOLVEIQ_BENCHMARK_EMAIL", "elena.rostova@resolveiq.local"))
    parser.add_argument("--password", default=os.getenv("RESOLVEIQ_BENCHMARK_PASSWORD", "ResolveIQ2026!"))
    parser.add_argument("--provider", required=True)
    parser.add_argument("--model", required=True)
    args = parser.parse_args()

    articles = json.loads((ROOT / "evaluation/datasets/knowledge_articles.json").read_text())
    cases = json.loads((ROOT / "evaluation/datasets/eval_ground_truth.json").read_text())
    article_titles = {article["id"]: article["title"] for article in articles}
    login = request(args.api_root, "POST", "/auth/login", body={"email": args.email, "password": args.password})
    token = login["accessToken"]
    ensure_corpus(args.api_root, token, articles)

    strategies = ("FTS_ONLY", "VECTOR_ONLY", "HYBRID_RRF")
    results = {}
    for strategy in strategies:
        hits = 0
        reciprocal_rank = 0.0
        server_latencies = []
        wall_latencies = []
        case_results = []
        for index, case in enumerate(cases, 1):
            started = time.perf_counter()
            response = request(args.api_root, "POST", "/retrieval/search", token, {
                "queryText": case["query"], "strategy": strategy, "topK": 5,
                "sourceTypes": ["KNOWLEDGE_ARTICLE"]
            })
            wall_ms = (time.perf_counter() - started) * 1000
            expected = {article_titles[item] for item in case["relevant_article_ids"]}
            titles = [citation["title"] for citation in response["citations"]]
            rank = next((position for position, title in enumerate(titles, 1) if title in expected), None)
            if rank:
                hits += 1
                reciprocal_rank += 1.0 / rank
            server_latencies.append(float(response["durationMs"]))
            wall_latencies.append(wall_ms)
            case_results.append({"caseId": index, "query": case["query"], "expected": sorted(expected),
                                 "retrieved": titles, "rank": rank,
                                 "serverLatencyMs": response["durationMs"], "wallLatencyMs": round(wall_ms, 2)})
        results[strategy] = {
            "recallAt5": hits / len(cases), "mrr": reciprocal_rank / len(cases),
            "serverLatencyP50Ms": statistics.median(server_latencies),
            "serverLatencyP95Ms": percentile(server_latencies, .95),
            "wallLatencyP50Ms": round(statistics.median(wall_latencies), 2),
            "wallLatencyP95Ms": round(percentile(wall_latencies, .95), 2),
            "cases": case_results
        }

    baseline = results["FTS_ONLY"]
    hybrid = results["HYBRID_RRF"]
    report = {
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "commit": subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True,
                                 capture_output=True, check=True).stdout.strip(),
        "workingTreeDirty": bool(subprocess.run(["git", "status", "--porcelain"], cwd=ROOT,
                                                  text=True, capture_output=True).stdout.strip()),
        "runtime": {"os": platform.platform(), "provider": args.provider, "model": args.model,
                    "applicationPath": "Spring Boot API + PostgreSQL/pgvector", "queries": len(cases),
                    "knowledgeArticles": len(articles)},
        "results": results,
        "hybridVsFts": {
            "recallAt5PercentagePointChange": round((hybrid["recallAt5"] - baseline["recallAt5"]) * 100, 2),
            "mrrPercentageChange": round((hybrid["mrr"] / baseline["mrr"] - 1) * 100, 2) if baseline["mrr"] else None,
            "serverP95LatencyRatio": round(hybrid["serverLatencyP95Ms"] / baseline["serverLatencyP95Ms"], 3)
                if baseline["serverLatencyP95Ms"] else None
        }
    }
    output = ROOT / "evaluation/reports/live_retrieval_benchmark.json"
    output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"report": str(output), "summary": {k: {x: v[x] for x in
          ("recallAt5", "mrr", "serverLatencyP95Ms", "wallLatencyP95Ms")} for k, v in results.items()},
          "hybridVsFts": report["hybridVsFts"]}, indent=2))


if __name__ == "__main__":
    main()
