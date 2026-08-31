package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.ticket.application.dto.*;
import com.resolveiq.ticket.application.service.TicketService;
import com.resolveiq.ticket.application.service.TicketQueueService;
import com.resolveiq.ticket.application.service.TicketRealtimeService;
import com.resolveiq.ticket.domain.model.TicketPriority;
import com.resolveiq.ticket.domain.model.TicketStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent/tickets")
public class AgentTicketController {

    private final TicketService ticketService;
    private final TicketQueueService ticketQueueService;
    private final TicketRealtimeService realtimeService;

    public AgentTicketController(TicketService ticketService, TicketQueueService ticketQueueService,
                                 TicketRealtimeService realtimeService) {
        this.ticketService = ticketService;
        this.ticketQueueService = ticketQueueService;
        this.realtimeService = realtimeService;
    }

    @GetMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader
    ) {
        return realtimeService.subscribe(
            parseRequiredUuid(tenantHeader, "X-Tenant-Id"),
            parseRequiredUuid(userHeader, "X-User-Id"),
            parseRoles(rolesHeader));
    }

    @GetMapping("/queue")
    public ResponseEntity<TicketQueueResponse> queue(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader,
        @RequestParam(defaultValue = "mine") String scope,
        @RequestParam(required = false) UUID teamId,
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) TicketPriority priority,
        @RequestParam(required = false, name = "query") String queryText,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "desc") String direction,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ticketQueueService.search(
            parseRequiredUuid(tenantHeader, "X-Tenant-Id"),
            parseRequiredUuid(userHeader, "X-User-Id"),
            parseRoles(rolesHeader), scope, teamId, status, priority, queryText, sort, direction, page, size));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listAllTickets(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader,
        @RequestParam(value = "teamId", required = false) UUID teamId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");
        boolean admin = java.util.Arrays.asList(rolesHeader.split(",")).contains("ADMIN");
        List<TicketResponse> tickets = admin
            ? (teamId != null ? ticketService.listTeamTickets(tenantId, teamId) : ticketService.listAllTickets(tenantId))
            : ticketService.listAssignedTickets(tenantId, agentId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketDetails(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader,
        @PathVariable(value = "id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        TicketResponse ticket = ticketService.getTicketById(tenantId, ticketId);
        ensureReadable(ticket, parseRequiredUuid(userHeader, "X-User-Id"), parseRoles(rolesHeader));
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/{id}/context")
    public ResponseEntity<AgentTicketContextResponse> getTicketContext(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader,
        @PathVariable(value = "id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        TicketResponse ticket = ticketService.getTicketById(tenantId, ticketId);
        ensureReadable(ticket, parseRequiredUuid(userHeader, "X-User-Id"), parseRoles(rolesHeader));
        return ResponseEntity.ok(new AgentTicketContextResponse(
            ticket, ticketService.getTicketMessages(tenantId, ticketId), ticketService.getTicketSuggestions(tenantId, ticketId)));
    }

    @PostMapping("/{id}/assign")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEAM_LEAD','ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader,
        @PathVariable(value = "id") UUID ticketId,
        @RequestBody AssignTicketRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID actorId = parseRequiredUuid(userHeader, "X-User-Id");
        Set<String> roles = parseRoles(rolesHeader);
        TicketResponse existing = ticketService.getTicketById(tenantId, ticketId);
        ensureReadable(existing, actorId, roles);
        if (!roles.contains("ADMIN")) {
            ticketService.assertTeamAccess(tenantId, actorId, existing.teamId());
            if (!java.util.Objects.equals(existing.teamId(), request.teamId())) {
                throw new SecurityException("Team leads cannot move tickets outside their team");
            }
        }
        TicketResponse ticket = ticketService.assignTicket(tenantId, ticketId, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','ADMIN')")
    public ResponseEntity<TicketResponse> updateTicketStatus(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable(value = "id") UUID ticketId,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");
        ensureReadable(ticketService.getTicketById(tenantId, ticketId), agentId,
            parseRolesFromSecurityContext());

        TicketResponse ticket = ticketService.updateStatus(tenantId, ticketId, agentId, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/messages")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','ADMIN')")
    public ResponseEntity<TicketMessageResponse> addAgentReply(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable(value = "id") UUID ticketId,
        @Valid @RequestBody AddMessageRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");
        ensureReadable(ticketService.getTicketById(tenantId, ticketId), agentId,
            parseRolesFromSecurityContext());

        TicketMessageResponse response = ticketService.addMessage(tenantId, ticketId, agentId, "AGENT", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/suggestions")
    public ResponseEntity<List<AiSuggestionResponse>> getSuggestions(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "X-Roles") String rolesHeader,
        @PathVariable(value = "id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        ensureReadable(ticketService.getTicketById(tenantId, ticketId),
            parseRequiredUuid(userHeader, "X-User-Id"), parseRoles(rolesHeader));
        List<AiSuggestionResponse> suggestions = ticketService.getTicketSuggestions(tenantId, ticketId);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/{id}/feedback")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('AGENT','TEAM_LEAD','ADMIN')")
    public ResponseEntity<Void> recordFeedback(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable(value = "id") UUID ticketId,
        @Valid @RequestBody SuggestionFeedbackRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");
        ensureReadable(ticketService.getTicketById(tenantId, ticketId), agentId,
            parseRolesFromSecurityContext());

        ticketService.recordFeedback(tenantId, ticketId, agentId, request);
        return ResponseEntity.noContent().build();
    }

    private Set<String> parseRoles(String header) {
        if (header == null || header.isBlank()) return Set.of();
        return Arrays.stream(header.split(",")).map(String::trim).filter(value -> !value.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> parseRolesFromSecurityContext() {
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return Set.of();
        return authentication.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .map(value -> value.startsWith("ROLE_") ? value.substring(5) : value)
            .collect(Collectors.toUnmodifiableSet());
    }

    private void ensureReadable(TicketResponse ticket, UUID actorId, Set<String> roles) {
        ticketService.assertStaffCanRead(ticket, actorId, roles);
    }

    private UUID parseRequiredUuid(String header, String name) {
        if (header == null || header.isBlank()) {
            throw new SecurityException("Missing mandatory identity header: " + name);
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid UUID in header: " + name);
        }
    }
}
