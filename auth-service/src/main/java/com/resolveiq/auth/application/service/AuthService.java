package com.resolveiq.auth.application.service;

import com.resolveiq.auth.application.dto.*;
import com.resolveiq.auth.domain.model.*;
import com.resolveiq.auth.domain.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityAuditEventRepository auditEventRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordService passwordService;
    private final long refreshExpirationMs;

    public AuthService(
        UserRepository userRepository,
        TenantRepository tenantRepository,
        RefreshTokenRepository refreshTokenRepository,
        SecurityAuditEventRepository auditEventRepository,
        JwtTokenProvider jwtTokenProvider,
        PasswordService passwordService,
        @Value("${resolveiq.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditEventRepository = auditEventRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordService = passwordService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String normalizedEmail = request.email().toLowerCase().trim();
        Optional<User> userOpt = request.tenantId() != null
            ? userRepository.findByTenantIdAndNormalizedEmail(request.tenantId(), normalizedEmail)
            : userRepository.findByNormalizedEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            recordAudit(request.tenantId() != null ? request.tenantId() : UUID.fromString("00000000-0000-0000-0000-000000000000"), null, "LOGIN_FAILED", "USER_NOT_FOUND", ipAddress, userAgent);
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userOpt.get();

        if (user.isLocked()) {
            recordAudit(user.getTenantId(), user.getId(), "LOGIN_BLOCKED", "ACCOUNT_LOCKED", ipAddress, userAgent);
            throw new IllegalStateException("Account temporarily locked due to excessive failed attempts. Try again later.");
        }

        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            recordAudit(user.getTenantId(), user.getId(), "LOGIN_FAILED", "BAD_CREDENTIALS", ipAddress, userAgent);
            throw new IllegalArgumentException("Invalid email or password");
        }

        user.resetFailedAttempts();
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getEmail(), user.getRoles());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
            user.getId(),
            tokenHash,
            Instant.now().plusMillis(refreshExpirationMs),
            ipAddress,
            userAgent
        );
        refreshTokenRepository.save(refreshToken);

        recordAudit(user.getTenantId(), user.getId(), "LOGIN_SUCCESS", "SUCCESS", ipAddress, userAgent);

        return new AuthResponse(
            accessToken,
            rawRefreshToken,
            "Bearer",
            jwtTokenProvider.getExpirationMs(),
            user.getId(),
            user.getTenantId(),
            user.getEmail(),
            user.getFullName(),
            user.getRoles()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        UUID tenantId = request.tenantId() != null ? request.tenantId() : UUID.randomUUID();

        // Ensure default tenant exists if not present
        if (!tenantRepository.existsById(tenantId)) {
            Tenant tenant = new Tenant(tenantId, "Default Tenant", "default.resolveiq.local", "ACTIVE");
            tenantRepository.save(tenant);
        }

        String normalizedEmail = request.email().toLowerCase().trim();
        if (userRepository.existsByTenantIdAndNormalizedEmail(tenantId, normalizedEmail)) {
            throw new IllegalArgumentException("User with email " + request.email() + " already exists in this tenant");
        }

        Set<Role> roles = request.roles() != null && !request.roles().isEmpty()
            ? request.roles()
            : Set.of(Role.CUSTOMER);

        User user = new User(
            UUID.randomUUID(),
            tenantId,
            request.email(),
            passwordService.encode(request.password()),
            request.fullName(),
            roles
        );
        userRepository.save(user);

        recordAudit(tenantId, user.getId(), "USER_REGISTERED", "SUCCESS", ipAddress, userAgent);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getEmail(), user.getRoles());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
            user.getId(),
            tokenHash,
            Instant.now().plusMillis(refreshExpirationMs),
            ipAddress,
            userAgent
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken,
            rawRefreshToken,
            "Bearer",
            jwtTokenProvider.getExpirationMs(),
            user.getId(),
            user.getTenantId(),
            user.getEmail(),
            user.getFullName(),
            user.getRoles()
        );
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request, String ipAddress, String userAgent) {
        String tokenHash = hashToken(request.refreshToken());
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        RefreshToken currentToken = tokenOpt.get();

        // Token Reuse Detection: If already revoked, invalidate all user sessions
        if (currentToken.isRevoked()) {
            refreshTokenRepository.deleteByUserId(currentToken.getUserId());
            recordAudit(UUID.fromString("00000000-0000-0000-0000-000000000000"), currentToken.getUserId(), "REFRESH_REUSE_DETECTED", "ALL_SESSIONS_REVOKED", ipAddress, userAgent);
            throw new SecurityException("Revoked refresh token presented. Security alert: All user sessions invalidated.");
        }

        if (currentToken.isExpired()) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = userRepository.findById(currentToken.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Rotate token
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken(
            user.getId(),
            newTokenHash,
            Instant.now().plusMillis(refreshExpirationMs),
            ipAddress,
            userAgent
        );
        refreshTokenRepository.save(newRefreshToken);

        currentToken.revoke(newRefreshToken.getId());
        refreshTokenRepository.save(currentToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getEmail(), user.getRoles());

        recordAudit(user.getTenantId(), user.getId(), "TOKEN_REFRESHED", "SUCCESS", ipAddress, userAgent);

        return new AuthResponse(
            newAccessToken,
            newRawRefreshToken,
            "Bearer",
            jwtTokenProvider.getExpirationMs(),
            user.getId(),
            user.getTenantId(),
            user.getEmail(),
            user.getFullName(),
            user.getRoles()
        );
    }

    @Transactional
    public void logout(UUID userId) {
        if (userId != null) {
            refreshTokenRepository.deleteByUserId(userId);
        }
    }

    private void recordAudit(UUID tenantId, UUID userId, String eventType, String status, String ipAddress, String userAgent) {
        SecurityAuditEvent event = new SecurityAuditEvent(tenantId, userId, eventType, status, ipAddress, userAgent);
        auditEventRepository.save(event);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
