package com.resolveiq.ticket.adapter.in.web.omnichannel;

import com.resolveiq.security.TrustedPrincipal;
import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.DevelopmentMailboxChallengeResponse;
import com.resolveiq.ticket.application.service.omnichannel.OmnichannelService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Local test-mailbox access. This controller is never registered in production. */
@RestController
@Profile({"docker", "local"})
@RequestMapping("/api/v1/customer/dev-mailbox")
public class DevelopmentMailboxController {

    private final OmnichannelService omnichannelService;

    public DevelopmentMailboxController(OmnichannelService omnichannelService) {
        this.omnichannelService = omnichannelService;
    }

    @GetMapping("/email/challenge")
    public ResponseEntity<DevelopmentMailboxChallengeResponse> getEmailChallenge(
        @RequestParam String email,
        @AuthenticationPrincipal TrustedPrincipal principal
    ) {
        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required"
            );
        }
        if (!principal.roles().contains("CUSTOMER")) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Local mailbox challenges may only be read by the customer who requested them"
            );
        }
        return ResponseEntity.ok(omnichannelService.getDevelopmentMailboxChallenge(
            principal.tenantId(), principal.userId(), email
        ));
    }
}
