# Contributing to ResolveIQ

Thank you for contributing to **ResolveIQ**!

## Architecture and Design Rules

1. **Deterministic Business Rules:** LLMs assist and suggest; they do not enforce ticket-state transitions, routing eligibility, or SLA deadlines.
2. **Human Approval Boundary:** No AI-generated draft may be sent directly to customers without explicit agent review.
3. **No Distributed Transactions:** Use local transactions, transactional outbox pattern, and idempotent event consumers.
4. **Single Source of Truth:** Consult `RESOLVEIQ_IMPLEMENTATION_BLUEPRINT.md` before making architectural or UX decisions.

## Development Workflow

### Git Conventions
- Default branch: `main`
- Branch naming: `feat/...`, `fix/...`, `docs/...`, `chore/...`
- Conventional Commits:
  - `feat(rag): add hybrid resolved-case retrieval`
  - `fix(auth): correct refresh token rotation race condition`
  - `chore(deps): update testcontainers version`

### Prerequisites
- Java 21
- Docker & Docker Compose
- Node.js 20+ & npm

### Local Build & Test
```bash
# Verify backend modules
./mvnw clean verify

# Start local infrastructure
docker compose up -d postgres kafka minio

# Run frontend tests
npm --prefix frontend ci
npm --prefix frontend test
```

## Pull Request Checklist
- [ ] Code follows hexagonal package conventions (`domain`, `application`, `adapter`).
- [ ] Tests added for state machines, authorization, and idempotency.
- [ ] All sensitive fields redacted from logs and traces.
- [ ] Zero lint/checkstyle warnings.
- [ ] Pull request template completed with risk assessment and rollback plan.
