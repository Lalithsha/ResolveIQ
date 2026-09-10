#!/usr/bin/env python3
"""Fail-closed prerequisites for Part 2 acceptance suites.

Unit tests are useful evidence, but they cannot stand in for real provider, browser,
media, retrieval, coverage, or container evidence.
"""
import argparse
import json
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]


def require(condition: bool, message: str, failures: list[str]) -> None:
    if not condition:
        failures.append(message)


def check_media(failures: list[str]) -> None:
    for binary in ("tesseract", "ffmpeg", "ffprobe", "pdftotext"):
        require(shutil.which(binary) is not None, f"required real-media tool is missing: {binary}", failures)
    manifest_path = ROOT / "verification/part2/fixtures/manifest.json"
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except Exception as error:
        failures.append(f"fixture manifest is unreadable: {error}")
        return
    items = manifest.get("fixtures", [])
    counts = {"image": 0, "pdf": 0, "text": 0, "media": 0, "malicious": 0}
    for item in items:
        path = manifest_path.parent / item.get("path", "")
        kind = str(item.get("kind", item.get("type", ""))).lower()
        if any(token in kind for token in ("image", "screenshot")): counts["image"] += 1
        if "pdf" in kind: counts["pdf"] += 1
        if any(token in kind for token in ("text", "log", "csv", "har")): counts["text"] += 1
        if any(token in kind for token in ("audio", "video")): counts["media"] += 1
        if any(token in kind for token in ("malicious", "malware")): counts["malicious"] += 1
        if path.is_file() and path.suffix.lower() == ".png":
            require(path.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"), f"not an actual PNG: {path}", failures)
        if path.is_file() and path.suffix.lower() == ".pdf":
            require(path.read_bytes().startswith(b"%PDF-"), f"not an actual PDF: {path}", failures)
        if path.is_file() and path.suffix.lower() == ".mp4":
            data = path.read_bytes()[:32]
            require(len(data) >= 12 and b"ftyp" in data, f"not an actual MP4: {path}", failures)
    minimums = {"image": 10, "pdf": 10, "text": 10, "media": 5, "malicious": 1}
    for kind, minimum in minimums.items():
        require(counts[kind] >= minimum, f"{kind} corpus has {counts[kind]} fixtures; requires >= {minimum}", failures)


def check_retrieval(failures: list[str]) -> None:
    report_path = ROOT / "verification/part2/reports/real_retrieval.json"
    try:
        report = json.loads(report_path.read_text(encoding="utf-8"))
        require(report.get("providerMode") not in (None, "deterministic", "keyword", "fixture"),
                "retrieval report does not identify a real provider mode", failures)
        require(len(report.get("cases", [])) >= 50, "retrieval report requires >=50 persisted per-case results", failures)
        require(report.get("recallAt5", 0) >= 0.85, "Recall@5 is below 0.85", failures)
        require(report.get("mrr", 0) >= 0.75, "MRR is below 0.75", failures)
        require(report.get("latencyP95Ratio", 99) <= 1.20, "p95 latency ratio exceeds 1.20", failures)
        require(report.get("safetyPassed") is True, "retrieval safety cases did not all pass", failures)
    except Exception as error:
        failures.append(f"real retrieval report is missing/unreadable: {error}")


def check_final_environment(failures: list[str]) -> None:
    version = subprocess.run(["java", "-version"], capture_output=True, text=True).stderr.splitlines()
    require(bool(version) and ('version "21' in version[0] or 'openjdk version "21' in version[0]),
            "FINAL-P2 must run on supported Java 21", failures)
    docker = subprocess.run(["docker", "info"], capture_output=True, text=True)
    require(docker.returncode == 0, "Docker daemon is unavailable", failures)
    require((ROOT / "verification/part2/reports/live_ui.json").is_file(),
            "live browser E2E report is missing", failures)
    require(any(ROOT.glob("*/target/site/jacoco/jacoco.xml")),
            "JaCoCo coverage XML is missing", failures)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--suite", required=True)
    args = parser.parse_args()
    suite = args.suite.upper()
    failures: list[str] = []
    if suite in ("FINAL-P2", "REAL_MEDIA"): check_media(failures)
    if suite in ("FINAL-P2", "REAL_RETRIEVAL"): check_retrieval(failures)
    if suite == "FINAL-P2": check_final_environment(failures)
    if failures:
        for failure in failures:
            print(f"BLOCKED_EXTERNAL: {failure}", file=sys.stderr)
        return 1
    print(f"Prerequisites verified for {args.suite}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
