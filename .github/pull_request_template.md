## Summary
<!-- Provide a brief description of the problem solved and the approach taken. -->

## Changes Included
- 

## Architectural Compliance
- [ ] No cross-schema database access introduced.
- [ ] AI model calls are decoupled from database transactions.
- [ ] Human-in-the-loop approval preserved (zero auto-sends).
- [ ] All telemetry/logs redacted of secrets and PII.

## Test Coverage & Verification
- [ ] Unit tests pass (`./mvnw clean test`)
- [ ] Frontend builds cleanly (`npm --prefix frontend run build`)
- [ ] Secret scan passes (`./scripts/scan-secrets.sh`)

## Risk & Rollback Plan
- **Risk Level:** Low / Medium / High
- **Rollback:** Revert commit / forward-fix migration
