#!/usr/bin/env bash
set -euo pipefail

echo "========================================================"
echo " Running Secret Scan on ResolveIQ Codebase"
echo "========================================================"

SECRET_PATTERNS=(
    "BEGIN PRIVATE KEY"
    "BEGIN RSA PRIVATE KEY"
    "AIzaSy"
    "ghp_[0-9a-zA-Z]{36}"
    "sk-[a-zA-Z0-9]{32,}"
    "AKIA[0-9A-Z]{16}"
    "eyJhbGciOi"
)

FOUND=0

for pattern in "${SECRET_PATTERNS[@]}"; do
    if grep -rInE --exclude="scan-secrets.sh" --exclude=".env" --exclude=".env.*" --exclude-dir={.git,node_modules,target,dist,.idea,.vscode} "$pattern" . ; then
        echo "WARNING: Potential secret found matching pattern: $pattern"
        FOUND=1
    fi
done

if [ "$FOUND" -eq 0 ]; then
    echo "✔ Secret scan clean! No private keys, AWS credentials, or hardcoded tokens detected."
else
    echo "✖ Secret scan failed. Please inspect matching lines above."
    exit 1
fi
