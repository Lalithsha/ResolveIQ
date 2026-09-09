package com.resolveiq.ticket.adapter.in.web.omnichannel;

import com.resolveiq.security.TrustedPrincipal;
import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.*;
import com.resolveiq.ticket.application.service.omnichannel.OmnichannelService;
import com.resolveiq.ticket.domain.model.omnichannel.ConversationMergeRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
        UUID tenantId = getTenantId(principal, tenantHeader);
        return ResponseEntity.ok(omnichannelService.getConversation(tenantId, id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<TimelineResponse> getTimeline(
        @PathVariable UUID id,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolvePrincipal(principal, tenantHeader, userHeader, rolesHeader);
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
        TrustedPrincipal effectivePrincipal = resolvePrincipal(principal, tenantHeader, userHeader, rolesHeader);
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
        TrustedPrincipal effectivePrincipal = resolvePrincipal(principal, tenantHeader, userHeader, rolesHeader);
        HandoffRequest req = request != null ? request : new HandoffRequest("Customer requested human agent");
        return ResponseEntity.ok(omnichannelService.requestHandoff(effectivePrincipal.tenantId(), id, effectivePrincipal, req));
    }

    @PostMapping("/{id}/handoff/assign")
    public ResponseEntity<ConversationResponse> assignHandoff(
        @PathVariable UUID id,
        @RequestBody HandoffAssignRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader
    ) {
        UUID tenantId = getTenantId(principal, tenantHeader);
        return ResponseEntity.ok(omnichannelService.assignHandoff(tenantId, id, request.agentId()));
    }

    @PostMapping("/{id}/merge")
    public ResponseEntity<ConversationMergeRecord> mergeConversations(
        @PathVariable UUID id,
        @RequestBody MergeConversationRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolvePrincipal(principal, tenantHeader, userHeader, rolesHeader);
        ConversationMergeRecord record = omnichannelService.mergeConversations(
            effectivePrincipal.tenantId(), id, request.targetConversationId(), effectivePrincipal.userId(), request.reason()
        );
        return ResponseEntity.ok(record);
    }

    @PostMapping("/split")
    public ResponseEntity<ConversationMergeRecord> splitConversations(
        @RequestBody SplitConversationRequest request,
        @AuthenticationPrincipal TrustedPrincipal principal,
        @RequestHeader(value = "X-Tenant-Id", required = false) UUID tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) UUID userHeader,
        @RequestHeader(value = "X-Roles", required = false) String rolesHeader
    ) {
        TrustedPrincipal effectivePrincipal = resolvePrincipal(principal, tenantHeader, userHeader, rolesHeader);
        ConversationMergeRecord record = omnichannelService.splitConversations(
            effectivePrincipal.tenantId(), request.mergeRecordId(), effectivePrincipal.userId()
        );
        return ResponseEntity.ok(record);
    }

    private UUID getTenantId(TrustedPrincipal principal, UUID tenantHeader) {
        if (principal != null && principal.tenantId() != null) return principal.tenantId();
        if (tenantHeader != null) return tenantHeader;
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private TrustedPrincipal resolvePrincipal(TrustedPrincipal authPrincipal, UUID tenantHeader, UUID userHeader, String rolesHeader) {
        if (authPrincipal != null) return authPrincipal;
        UUID tenantId = tenantHeader != null ? tenantHeader : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = userHeader != null ? userHeader : UUID.fromString("00000000-0000-0000-0000-000000000002");
        Set<String> roles = rolesHeader != null ? Set.of(rolesHeader.split(",")) : Set.of("AGENT");
        Set<String> permissions = Set.of("CONVERSATION_MERGE", "TICKET_READ", "TICKET_WRITE");
        return new TrustedPrincipal(userId, tenantId, roles, "DIRECT", permissions, Instant.now());
    }
}
