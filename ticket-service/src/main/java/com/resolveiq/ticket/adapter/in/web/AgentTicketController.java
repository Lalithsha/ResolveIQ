package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.ticket.application.dto.*;
import com.resolveiq.ticket.application.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent/tickets")
public class AgentTicketController {

    private final TicketService ticketService;

    public AgentTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
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
        @PathVariable("id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        TicketResponse ticket = ticketService.getTicketById(tenantId, ticketId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @PathVariable("id") UUID ticketId,
        @RequestBody AssignTicketRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        TicketResponse ticket = ticketService.assignTicket(tenantId, ticketId, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");

        TicketResponse ticket = ticketService.updateStatus(tenantId, ticketId, agentId, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessageResponse> addAgentReply(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody AddMessageRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");

        TicketMessageResponse response = ticketService.addMessage(tenantId, ticketId, agentId, "AGENT", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/suggestions")
    public ResponseEntity<List<AiSuggestionResponse>> getSuggestions(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @PathVariable("id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        List<AiSuggestionResponse> suggestions = ticketService.getTicketSuggestions(tenantId, ticketId);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> recordFeedback(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody SuggestionFeedbackRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID agentId = parseRequiredUuid(userHeader, "X-User-Id");

        ticketService.recordFeedback(tenantId, ticketId, agentId, request);
        return ResponseEntity.noContent().build();
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
