package com.resolveiq.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.resolveiq.auth.application.dto.AuthResponse;
import com.resolveiq.auth.application.dto.LoginRequest;
import com.resolveiq.auth.application.dto.RegisterRequest;
import com.resolveiq.auth.application.dto.TokenRefreshRequest;
import com.resolveiq.auth.application.service.AuthService;
import com.resolveiq.auth.application.service.JwtTokenProvider;
import com.resolveiq.auth.application.service.PasswordService;
import com.resolveiq.auth.domain.model.RefreshToken;
import com.resolveiq.auth.domain.model.Role;
import com.resolveiq.auth.domain.model.User;
import com.resolveiq.auth.domain.repository.RefreshTokenRepository;
import com.resolveiq.auth.domain.repository.SecurityAuditEventRepository;
import com.resolveiq.auth.domain.repository.TenantRepository;
import com.resolveiq.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private SecurityAuditEventRepository auditEventRepository;

    private JwtTokenProvider jwtTokenProvider;
    private PasswordService passwordService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
            "fictional_jwt_hmac_secret_key_minimum_256_bits_for_local_development_only_12345",
            900000
        );
        passwordService = new PasswordService();
        authService = new AuthService(
            userRepository,
            tenantRepository,
            refreshTokenRepository,
            auditEventRepository,
            jwtTokenProvider,
            passwordService,
            604800000
        );
    }

    @Test
    @DisplayName("Should successfully login user with correct credentials")
    void testSuccessfulLogin() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String rawPassword = "ValidPassword123!";
        String encodedPassword = passwordService.encode(rawPassword);

        User user = new User(userId, tenantId, "agent@resolveiq.local", encodedPassword, "Agent Smith", Set.of(Role.AGENT));

        when(userRepository.findByTenantIdAndNormalizedEmail(tenantId, "agent@resolveiq.local"))
            .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest(tenantId, "agent@resolveiq.local", rawPassword);
        AuthResponse response = authService.login(request, "127.0.0.1", "TestAgent");

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.accessToken()).isNotEmpty();
        assertThat(response.refreshToken()).isNotEmpty();
        assertThat(response.roles()).contains(Role.AGENT);

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should fail login and lock account after 5 failed attempts")
    void testAccountLockoutAfterFiveFailedAttempts() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String rawPassword = "CorrectPassword123!";
        String encodedPassword = passwordService.encode(rawPassword);

        User user = new User(userId, tenantId, "agent@resolveiq.local", encodedPassword, "Agent Smith", Set.of(Role.AGENT));

        when(userRepository.findByTenantIdAndNormalizedEmail(tenantId, "agent@resolveiq.local"))
            .thenReturn(Optional.of(user));

        LoginRequest badRequest = new LoginRequest(tenantId, "agent@resolveiq.local", "WrongPassword");

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(badRequest, "127.0.0.1", "TestAgent"))
                .isInstanceOf(IllegalArgumentException.class);
            assertThat(user.isLocked()).isFalse();
        }

        // 5th attempt locks the account
        assertThatThrownBy(() -> authService.login(badRequest, "127.0.0.1", "TestAgent"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(user.isLocked()).isTrue();

        // Subsequent attempt blocked due to lock
        assertThatThrownBy(() -> authService.login(badRequest, "127.0.0.1", "TestAgent"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Account temporarily locked");
    }

    @Test
    @DisplayName("Should rotate refresh token and revoke old token")
    void testRefreshTokenRotation() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = new User(userId, tenantId, "user@test.com", "hash", "Test User", Set.of(Role.CUSTOMER));

        RefreshToken oldToken = new RefreshToken(
            userId,
            "old_token_hash",
            Instant.now().plusSeconds(3600),
            "127.0.0.1",
            "TestAgent"
        );

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        TokenRefreshRequest request = new TokenRefreshRequest("valid_raw_token");
        AuthResponse response = authService.refreshToken(request, "127.0.0.1", "TestAgent");

        assertThat(response).isNotNull();
        assertThat(oldToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should detect reuse of revoked token and invalidate all sessions")
    void testTokenReuseDetection() {
        UUID userId = UUID.randomUUID();
        RefreshToken revokedToken = new RefreshToken(
            userId,
            "revoked_hash",
            Instant.now().plusSeconds(3600),
            "127.0.0.1",
            "TestAgent"
        );
        revokedToken.revoke(UUID.randomUUID());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        TokenRefreshRequest request = new TokenRefreshRequest("reused_token");

        assertThatThrownBy(() -> authService.refreshToken(request, "127.0.0.1", "TestAgent"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Revoked refresh token presented");

        verify(refreshTokenRepository, times(1)).deleteByUserId(userId);
    }
}
