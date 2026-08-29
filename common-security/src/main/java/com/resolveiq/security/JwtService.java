package com.resolveiq.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final String audience;

    public JwtService(@Value("${resolveiq.jwt.secret:fictional_jwt_hmac_secret_key_minimum_256_bits_for_local_development_only_12345}") String secret,
                      @Value("${resolveiq.jwt.issuer:resolveiq-auth}") String issuer,
                      @Value("${resolveiq.jwt.audience:resolveiq-api}") String audience) {
        if (secret.length() < 32) throw new IllegalStateException("JWT secret must contain at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
    }

    public Claims parseAndValidate(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!issuer.equals(claims.getIssuer()) || claims.getAudience() == null || !claims.getAudience().contains(audience)) {
            throw new SecurityException("Invalid token issuer or audience");
        }
        String type = claims.get("token_type", String.class);
        if (!Set.of("access", "service").contains(type)) throw new SecurityException("Invalid token type");
        return claims;
    }

    public String serviceToken(String serviceName, UUID tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(issuer).audience().add(audience).and()
            .subject("service:" + serviceName)
            .claim("tenantId", tenantId.toString())
            .claim("roles", List.of("SYSTEM"))
            .claim("token_type", "service")
            .issuedAt(Date.from(now)).notBefore(Date.from(now.minusSeconds(5)))
            .expiration(Date.from(now.plusSeconds(60)))
            .signWith(key).compact();
    }
}
