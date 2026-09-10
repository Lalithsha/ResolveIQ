package com.resolveiq.orchestration.adapter.in.web;

import com.resolveiq.orchestration.application.dto.ResolutionActionDtos.*;
import com.resolveiq.orchestration.application.service.ResolutionActionService;
import com.resolveiq.security.TrustedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Resolution Actions", description = "Policy-controlled resolution actions, approvals and idempotent executions")
public class ResolutionActionController {

    private final ResolutionActionService actionService;

    public ResolutionActionController(ResolutionActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping("/api/v1/tickets/{ticketId}/resolution-actions/propose")
    @Operation(summary = "Propose a resolution action for a ticket")
    public ResponseEntity<ActionProposalResponse> proposeAction(
            @PathVariable UUID ticketId,
            @RequestBody ProposeActionRequest request,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        validateStaffMutation(principal);
        UUID tenantId = requireAuthenticatedTenant(principal);
        ActionProposalResponse response = actionService.proposeAction(tenantId, ticketId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/tickets/{ticketId}/resolution-actions")
    @Operation(summary = "List all resolution action proposals for a ticket")
    public ResponseEntity<List<ActionProposalResponse>> listActionsForTicket(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        UUID tenantId = requireAuthenticatedTenant(principal);
        return ResponseEntity.ok(actionService.getProposalsForTicket(tenantId, ticketId));
    }

    @GetMapping("/api/v1/resolution-actions/{id}")
    @Operation(summary = "Get action proposal by ID")
    public ResponseEntity<ActionProposalResponse> getProposal(
            @PathVariable UUID id,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        UUID tenantId = requireAuthenticatedTenant(principal);
        return ResponseEntity.ok(actionService.getProposal(tenantId, id));
    }

    @GetMapping("/api/v1/resolution-actions/{id}/executions")
    @Operation(summary = "Get executions for action proposal")
    public ResponseEntity<List<ActionExecutionResponse>> getExecutions(
            @PathVariable UUID id,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        UUID tenantId = requireAuthenticatedTenant(principal);
        return ResponseEntity.ok(actionService.getExecutionsForProposal(tenantId, id));
    }

    @PostMapping("/api/v1/resolution-actions/{id}/approve")
    @Operation(summary = "Approve a resolution action with exact canonical digest")
    public ResponseEntity<ActionProposalResponse> approveAction(
            @PathVariable UUID id,
            @RequestBody ApproveActionRequest request,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        validateStaffMutation(principal);
        UUID tenantId = requireAuthenticatedTenant(principal);
        return ResponseEntity.ok(actionService.approveAction(tenantId, id, request, principal));
    }

    @PostMapping("/api/v1/resolution-actions/{id}/reject")
    @Operation(summary = "Reject a resolution action")
    public ResponseEntity<ActionProposalResponse> rejectAction(
            @PathVariable UUID id,
            @RequestBody RejectActionRequest request,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        validateStaffMutation(principal);
        UUID tenantId = requireAuthenticatedTenant(principal);
        return ResponseEntity.ok(actionService.rejectAction(tenantId, id, request, principal));
    }

    @PostMapping("/api/v1/resolution-actions/{id}/execute")
    @Operation(summary = "Execute an approved resolution action idempotently")
    public ResponseEntity<ActionExecutionResponse> executeAction(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody ExecuteActionRequest request,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        validateStaffMutation(principal);
        UUID tenantId = requireAuthenticatedTenant(principal);

        String effectiveKey = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey()
                : idempotencyKeyHeader;

        ExecuteActionRequest effectiveRequest = new ExecuteActionRequest(
                request.approvedDigest(),
                request.expectedVersion(),
                effectiveKey
        );

        ActionExecutionResponse response = actionService.executeAction(tenantId, id, effectiveRequest, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/api/v1/resolution-actions/{id}/compensate")
    @Operation(summary = "Attempt compensation for an action (truthful non-compensatable feedback)")
    public ResponseEntity<CompensationResponse> compensateAction(
            @PathVariable UUID id,
            @AuthenticationPrincipal TrustedPrincipal principal) {
        validateStaffMutation(principal);
        UUID tenantId = requireAuthenticatedTenant(principal);
        return ResponseEntity.ok(actionService.compensateAction(tenantId, id));
    }

    private UUID requireAuthenticatedTenant(TrustedPrincipal principal) {
        if (principal == null || principal.tenantId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required: missing trusted principal");
        }
        return principal.tenantId();
    }

    private void validateStaffMutation(TrustedPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        if (principal.hasRole("AUDITOR")) {
            throw new org.springframework.security.access.AccessDeniedException("Auditor role is read-only");
        }
        if (principal.hasRole("CUSTOMER")) {
            throw new org.springframework.security.access.AccessDeniedException("Customers cannot perform resolution action operations");
        }
    }
}
