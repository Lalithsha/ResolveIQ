# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of ResolveIQ seriously. If you discover a security issue or vulnerability, please follow these guidelines:

1. **Do not create a public GitHub issue** for undisclosed security vulnerabilities.
2. Email the maintainers privately at `security@resolveiq.local` with:
   - A description of the vulnerability and potential impact.
   - Exact steps or proof-of-concept scripts to reproduce the behavior.
   - Affected components (Gateway, Auth Service, Ticket Service, RAG Service, etc.).
   - Any suggested mitigations or patches.

## Security Controls in ResolveIQ

- **No Auto-Send / Human-in-the-Loop:** AI-generated suggestions are never sent to external customers without explicit agent confirmation.
- **Tenant Isolation:** All database queries and events are partitioned by `tenantId`.
- **Token Security:** Refresh tokens are hashed using cryptographic hashes; access tokens are short-lived.
- **Input Sanitization:** Ticket bodies and attachments are sanitized against prompt injection and cross-site scripting (XSS).
- **Redacted Telemetry:** OpenTelemetry traces and application logs must never log passwords, bearer tokens, or full prompt bodies.
