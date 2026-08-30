package com.resolveiq.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
        @org.springframework.lang.NonNull HttpServletRequest request,
        @org.springframework.lang.NonNull HttpServletResponse response,
        @org.springframework.lang.NonNull FilterChain chain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        final TrustedPrincipal principal;
        final List<SimpleGrantedAuthority> authorities;
        try {
            Claims claims = jwtService.parseAndValidate(authorization.substring(7));
            UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
            String subject = claims.getSubject();
            String authenticationType = claims.get("token_type", String.class);
            UUID userId = "service".equals(authenticationType) ? new UUID(0, 0) : UUID.fromString(subject);
            List<?> rawRoles = claims.get("roles", List.class);
            Set<String> roles = new LinkedHashSet<>();
            if (rawRoles != null) rawRoles.forEach(role -> roles.add(String.valueOf(role)));
            principal = new TrustedPrincipal(userId, tenantId, Set.copyOf(roles), authenticationType);
            authorities = roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        } catch (Exception invalidToken) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired access token");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, authorities)
        );
        chain.doFilter(new VerifiedIdentityRequest(request, principal), response);
    }

    private static final class VerifiedIdentityRequest extends HttpServletRequestWrapper {
        private final Map<String, String> identity;
        private VerifiedIdentityRequest(HttpServletRequest request, TrustedPrincipal principal) {
            super(request);
            this.identity = Map.of(
                "x-tenant-id", principal.tenantId().toString(),
                "x-user-id", principal.userId().toString(),
                "x-roles", String.join(",", principal.roles()),
                "x-internal-caller", "verified-jwt"
            );
        }
        @Override public String getHeader(String name) {
            return identity.getOrDefault(name.toLowerCase(Locale.ROOT), super.getHeader(name));
        }
        @Override public Enumeration<String> getHeaders(String name) {
            String value = identity.get(name.toLowerCase(Locale.ROOT));
            return value == null ? super.getHeaders(name) : Collections.enumeration(List.of(value));
        }
    }
}
