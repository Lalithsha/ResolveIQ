package com.resolveiq.auth.application.service;

import com.resolveiq.auth.domain.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;
    private final String audience;

    public JwtTokenProvider(
        @Value("${resolveiq.jwt.secret:fictional_jwt_hmac_secret_key_minimum_256_bits_for_local_development_only_12345}") String secret,
        @Value("${resolveiq.jwt.expiration-ms:900000}") long expirationMs,
        @Value("${resolveiq.jwt.issuer:resolveiq-auth}") String issuer,
        @Value("${resolveiq.jwt.audience:resolveiq-api}") String audience
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateAccessToken(UUID userId, UUID tenantId, String email, Set<Role> roles) {
        return generateAccessToken(userId, tenantId, email, roles, null, Instant.now());
    }

    public String generateAccessToken(UUID userId, UUID tenantId, String email, Set<Role> roles, Set<String> explicitPermissions, Instant authTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        List<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toList());
        Set<String> permissions = explicitPermissions != null && !explicitPermissions.isEmpty()
            ? explicitPermissions
            : deriveDefaultPermissions(roles);

        Instant effectiveAuthTime = authTime != null ? authTime : Instant.now();

        return Jwts.builder()
            .issuer(issuer)
            .audience().add(audience).and()
            .subject(userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("email", email)
            .claim("roles", roleNames)
            .claim("permissions", new ArrayList<>(permissions))
            .claim("auth_time", effectiveAuthTime.getEpochSecond())
            .claim("token_type", "access")
            .issuedAt(now)
            .notBefore(new Date(now.getTime() - 5000))
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }

    public Set<String> deriveDefaultPermissions(Set<Role> roles) {
        if (roles == null) return Collections.emptySet();
        Set<String> permissions = new LinkedHashSet<>();
        for (Role role : roles) {
            switch (role) {
                case AGENT -> permissions.add("ACTION_APPROVE_LOW_RISK");
                case TEAM_LEAD -> {
                    permissions.add("INCIDENT_APPROVE");
                    permissions.add("INCIDENT_PUBLISH");
                    permissions.add("ACTION_APPROVE_LOW_RISK");
                    permissions.add("ACTION_APPROVE_FINANCIAL");
                    permissions.add("EVIDENCE_VIEW_ORIGINAL");
                    permissions.add("CONVERSATION_MERGE");
                    permissions.add("TICKET_ASSIGN");
                }
                case KNOWLEDGE_MANAGER -> permissions.add("KNOWLEDGE_RELEASE_APPROVE");
                case ADMIN -> {
                    permissions.add("INCIDENT_APPROVE");
                    permissions.add("INCIDENT_PUBLISH");
                    permissions.add("ACTION_APPROVE_LOW_RISK");
                    permissions.add("ACTION_APPROVE_FINANCIAL");
                    permissions.add("EVIDENCE_VIEW_ORIGINAL");
                    permissions.add("CONVERSATION_MERGE");
                    permissions.add("TICKET_ASSIGN");
                    permissions.add("KNOWLEDGE_RELEASE_APPROVE");
                }
                default -> {}
            }
        }
        return permissions;
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getAudience() != null && claims.getAudience().contains(audience)
                && "access".equals(claims.get("token_type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
