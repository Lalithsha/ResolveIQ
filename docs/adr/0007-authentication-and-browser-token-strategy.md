# ADR 0007: Authentication and Browser Token Strategy

## Status
Accepted

## Context
ResolveIQ must provide secure authentication across web clients, support token refresh, mitigate XSS/CSRF token theft, and ensure revocation capability without creating a database bottleneck on every API call.

## Decision
1. **Access Tokens:** Short-lived JWTs (15 minutes) signed with HMAC-SHA256 containing `userId`, `tenantId`, and `roles`. Verified statelessly by the API Gateway and downstream services.
2. **Refresh Tokens:** Long-lived opaque tokens (7 days) stored as cryptographic SHA-256 hashes in `auth_schema.refresh_tokens`. Sent in HTTP-only, SameSite=Strict secure cookies.
3. **Rotation & Reuse Detection:** Every refresh request issues a new refresh token and revokes the old one. If an already revoked token is presented, all sessions for that user family are immediately revoked (automatic lockout).

## Consequences
- **Positive:** High performance for API requests; secure token storage preventing JavaScript token theft; robust breach protection.
- **Negative:** Requires stateful refresh endpoint.
- **Reversal Trigger:** Migration to external OAuth2/OIDC identity provider (e.g. Keycloak, Auth0).
