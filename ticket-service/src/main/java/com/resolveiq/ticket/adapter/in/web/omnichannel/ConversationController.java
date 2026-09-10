package com.resolveiq.ticket.adapter.in.web.omnichannel;

import com.resolveiq.security.TrustedPrincipal;
import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.*;
import com.resolveiq.ticket.application.service.omnichannel.OmnichannelService;
import com.resolveiq.ticket.domain.model.omnichannel.ConversationMergeRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final OmnichannelService omnichannelService;

    public ConversationController(OmnichannelService omnichannelService) {
        this.omnichannelService = omnichannelService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(
        @PathVariable UUID id,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        return ResponseEntity.ok(omnichannelService.getConversation(effectivePrincipal.tenantId(), id, effectivePrincipal));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<TimelineResponse> getTimeline(
        @PathVariable UUID id,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        return ResponseEntity.ok(omnichannelService.getTimeline(effectivePrincipal.tenantId(), id, effectivePrincipal));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<TimelineMessageItem> addMessage(
        @PathVariable UUID id,
        @RequestBody AddConversationMessageRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        TimelineMessageItem item = omnichannelService.addMessage(effectivePrincipal.tenantId(), id, effectivePrincipal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PostMapping("/{id}/handoff")
    public ResponseEntity<HandoffResponse> requestHandoff(
        @PathVariable UUID id,
        @RequestBody(required = false) HandoffRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        HandoffRequest req = request != null ? request : new HandoffRequest("Customer requested human agent");
        return ResponseEntity.ok(omnichannelService.requestHandoff(effectivePrincipal.tenantId(), id, effectivePrincipal, req));
    }

    @PostMapping("/{id}/handoff/assign")
    @PreAuthorize("hasAuthority('PERM_TICKET_ASSIGN')")
    public ResponseEntity<ConversationResponse> assignHandoff(
        @PathVariable UUID id,
        @RequestBody HandoffAssignRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        return ResponseEntity.ok(omnichannelService.assignHandoff(effectivePrincipal.tenantId(), id, request.agentId()));
    }

    @PostMapping("/{id}/merge")
    @PreAuthorize("hasAuthority('PERM_CONVERSATION_MERGE')")
    public ResponseEntity<ConversationMergeRecord> mergeConversations(
        @PathVariable UUID id,
        @RequestBody MergeConversationRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        ConversationMergeRecord record = omnichannelService.mergeConversations(
            effectivePrincipal.tenantId(), id, request.targetConversationId(), effectivePrincipal.userId(), request.reason()
        );
        return ResponseEntity.ok(record);
    }

    @PostMapping("/split")
    @PreAuthorize("hasAuthority('PERM_CONVERSATION_MERGE')")
    public ResponseEntity<ConversationMergeRecord> splitConversations(
        @RequestBody SplitConversationRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = requirePrincipal(principal);
        ConversationMergeRecord record = omnichannelService.splitConversations(
            effectivePrincipal.tenantId(), request.mergeRecordId(), effectivePrincipal.userId()
        );
        return ResponseEntity.ok(record);
    }

    private TrustedPrincipal requirePrincipal(TrustedPrincipal principal) {
        if (principal == null || principal.userId() == null || principal.tenantId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
