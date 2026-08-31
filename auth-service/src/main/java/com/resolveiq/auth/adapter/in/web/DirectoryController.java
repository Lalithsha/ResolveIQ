package com.resolveiq.auth.adapter.in.web;

import com.resolveiq.auth.application.dto.AdminUserPageResponse;
import com.resolveiq.auth.application.dto.UpdateRolesRequest;
import com.resolveiq.auth.application.dto.UserProfileDto;
import com.resolveiq.auth.application.service.AuthService;
import com.resolveiq.auth.domain.model.SecurityAuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class DirectoryController {
    private final AuthService authService;

    public DirectoryController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/v1/directory/users/{id}")
    @PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','KNOWLEDGE_MANAGER','ADMIN','AUDITOR')")
    public ResponseEntity<UserProfileDto> user(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(authService.findDirectoryUser(tenantId, id));
    }

    @GetMapping("/api/v1/admin/users")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<AdminUserPageResponse> users(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(authService.listUsers(tenantId, page, size));
    }

    @PatchMapping("/api/v1/admin/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> updateRoles(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID operatorId,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRolesRequest request,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(authService.updateRoles(
            tenantId, id, operatorId, request.roles(), servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent")));
    }

    @GetMapping("/api/v1/audit/security-events")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<Page<SecurityAuditEvent>> auditEvents(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(authService.listAuditEvents(tenantId, page, size));
    }
}
