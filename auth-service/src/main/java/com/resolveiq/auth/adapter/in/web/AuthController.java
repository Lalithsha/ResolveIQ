package com.resolveiq.auth.adapter.in.web;

import com.resolveiq.auth.application.dto.*;
import com.resolveiq.auth.application.service.AuthService;
import com.resolveiq.auth.domain.model.User;
import com.resolveiq.auth.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final boolean secureCookies;

    public AuthController(AuthService authService, UserRepository userRepository,
                          @Value("${resolveiq.cookies.secure:false}") boolean secureCookies) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return withRefreshCookie(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.register(request, ipAddress, userAgent);
        return withRefreshCookie(response, HttpStatus.CREATED);
    }

    @PostMapping("/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> createUser(
        @Valid @RequestBody AdminCreateUserRequest request,
        HttpServletRequest httpRequest,
        Authentication authentication
    ) {
        Claims claims = (Claims) authentication.getDetails();
        UUID authenticatedTenant = UUID.fromString(claims.get("tenantId", String.class));
        if (!authenticatedTenant.equals(request.tenantId())) {
            throw new SecurityException("Administrators may create users only in their own tenant");
        }
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        UserProfileDto response = authService.createUserByAdmin(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @CookieValue(name = "resolveiq_refresh") String refreshToken,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.refreshToken(new TokenRefreshRequest(refreshToken), ipAddress, userAgent);
        return withRefreshCookie(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId) {
        if (userId != null) {
            authService.logout(UUID.fromString(userId));
        }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
            .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(@AuthenticationPrincipal String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return ResponseEntity.ok(new UserProfileDto(
            user.getId(),
            user.getTenantId(),
            user.getEmail(),
            user.getFullName(),
            user.getRoles()
        ));
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse response, HttpStatus status) {
        return ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken(), 7 * 24 * 60 * 60).toString())
            .body(response);
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from("resolveiq_refresh", value)
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(maxAgeSeconds)
            .build();
    }
}
