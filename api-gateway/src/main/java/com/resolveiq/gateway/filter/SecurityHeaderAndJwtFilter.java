package com.resolveiq.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SecurityHeaderAndJwtFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeaderAndJwtFilter.class);

    private final SecretKey key;
    private final String issuer;
    private final String audience;

    public SecurityHeaderAndJwtFilter(
        @Value("${resolveiq.jwt.secret:fictional_jwt_hmac_secret_key_minimum_256_bits_for_local_development_only_12345}") String secret,
        @Value("${resolveiq.jwt.issuer:resolveiq-auth}") String issuer,
        @Value("${resolveiq.jwt.audience:resolveiq-api}") String audience
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Ensure Correlation ID exists
        String rawCorrelationId = request.getHeaders().getFirst("X-Correlation-Id");
        String correlationId = (StringUtils.hasText(rawCorrelationId) && rawCorrelationId != null && rawCorrelationId.length() <= 64 && rawCorrelationId.matches("[A-Za-z0-9._-]+"))
            ? rawCorrelationId
            : UUID.randomUUID().toString();

        // 2. Strip any inbound client-spoofed internal identity headers
        ServerHttpRequest.Builder requestBuilder = request.mutate()
            .headers(httpHeaders -> {
                httpHeaders.remove("X-Tenant-Id");
                httpHeaders.remove("X-User-Id");
                httpHeaders.remove("X-Roles");
                httpHeaders.remove("X-Internal-Caller");
            })
            .header("X-Correlation-Id", correlationId);

        // 3. Check if path is public/permit-all
        if (isPublicPath(path)) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // 4. Validate JWT Bearer token for protected routes
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            return returnUnauthorized(exchange, "Missing or malformed Authorization header", correlationId);
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);
            if (claims.getAudience() == null || !claims.getAudience().contains(audience)
                || !"access".equals(claims.get("token_type", String.class))) {
                return returnUnauthorized(exchange, "Invalid JWT audience or token type", correlationId);
            }
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            String rolesStr = roles != null ? String.join(",", roles) : "";

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(tenantId)) {
                return returnUnauthorized(exchange, "Invalid JWT claims: missing subject or tenantId", correlationId);
            }

            // Inject trusted verified headers downstream to internal services
            requestBuilder
                .header("X-Tenant-Id", tenantId)
                .header("X-User-Id", userId)
                .header("X-Roles", rolesStr)
                .header("X-Internal-Caller", "api-gateway");

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return returnUnauthorized(exchange, "Invalid or expired JWT token", correlationId);
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/refresh")
            || path.startsWith("/actuator")
            || path.startsWith("/favicon.ico");
    }

    private Mono<Void> returnUnauthorized(ServerWebExchange exchange, String detail, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format("""
            {
              "type": "about:blank",
              "title": "Unauthorized",
              "status": 401,
              "detail": "%s",
              "errorCode": "UNAUTHORIZED",
              "correlationId": "%s",
              "timestamp": "%s"
            }
            """, detail, correlationId, Instant.now());

        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
