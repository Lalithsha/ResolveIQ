package com.resolveiq.ticket.adapter.in.web.omnichannel;

import com.resolveiq.security.TrustedPrincipal;
import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.*;
import com.resolveiq.ticket.application.service.omnichannel.OmnichannelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerChannelController {

    private final OmnichannelService omnichannelService;

    public CustomerChannelController(OmnichannelService omnichannelService) {
        this.omnichannelService = omnichannelService;
    }

    @PostMapping("/channel-identities/email/challenge")
    public ResponseEntity<EmailChallengeResponse> requestEmailChallenge(
        @RequestBody EmailChallengeRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolveCustomerPrincipal(principal, tenantHeader, userHeader);
        EmailChallengeResponse resp = omnichannelService.requestEmailChallenge(
            effectivePrincipal.tenantId(), effectivePrincipal.userId(), request.email()
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/channel-identities/email/verify")
    public ResponseEntity<EmailVerifyResponse> verifyEmailChallenge(
        @RequestBody EmailVerifyRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolveCustomerPrincipal(principal, tenantHeader, userHeader);
        EmailVerifyResponse resp = omnichannelService.verifyEmailChallenge(
            effectivePrincipal.tenantId(), effectivePrincipal.userId(), request.email(), request.token()
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/channel-identities")
    public ResponseEntity<List<ChannelIdentityResponse>> listIdentities(
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolveCustomerPrincipal(principal, tenantHeader, userHeader);
        return ResponseEntity.ok(omnichannelService.getCustomerIdentities(effectivePrincipal.tenantId(), effectivePrincipal.userId()));
    }

    @GetMapping("/channel-preferences")
    public ResponseEntity<CustomerPreferencesResponse> getPreferences(
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolveCustomerPrincipal(principal, tenantHeader, userHeader);
        return ResponseEntity.ok(omnichannelService.getPreferences(effectivePrincipal.tenantId(), effectivePrincipal.userId()));
    }

    @PatchMapping("/channel-preferences")
    public ResponseEntity<CustomerPreferencesResponse> updatePreferences(
        @RequestBody ChannelPreferenceUpdateRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolveCustomerPrincipal(principal, tenantHeader, userHeader);
        return ResponseEntity.ok(omnichannelService.updatePreferences(effectivePrincipal.tenantId(), effectivePrincipal.userId(), request));
    }

    private TrustedPrincipal resolveCustomerPrincipal(TrustedPrincipal authPrincipal, UUID tenantHeader, UUID userHeader) {
        if (authPrincipal != null) {
            if (authPrincipal.roles().contains("AUDITOR")) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Auditors are read-only and cannot manage channels"
                );
            }
            return authPrincipal;
        }
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
