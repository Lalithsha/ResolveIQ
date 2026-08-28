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
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestParam(value = "teamId", required = false) UUID teamId
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<TicketResponse> tickets = teamId != null
            ? ticketService.listTeamTickets(tenantId, teamId)
            : ticketService.listAllTickets(tenantId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketDetails(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable("id") UUID ticketId
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        TicketResponse ticket = ticketService.getTicketById(tenantId, ticketId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignTicket(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable("id") UUID ticketId,
        @RequestBody AssignTicketRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        TicketResponse ticket = ticketService.assignTicket(tenantId, ticketId, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID agentId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        TicketResponse ticket = ticketService.updateStatus(tenantId, ticketId, agentId, request);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessageResponse> addAgentReply(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody AddMessageRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID agentId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        TicketMessageResponse response = ticketService.addMessage(tenantId, ticketId, agentId, "AGENT", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/suggestions")
    public ResponseEntity<List<AiSuggestionResponse>> getSuggestions(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable("id") UUID ticketId
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<AiSuggestionResponse> suggestions = ticketService.getTicketSuggestions(tenantId, ticketId);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> recordFeedback(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody SuggestionFeedbackRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID agentId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        ticketService.recordFeedback(tenantId, ticketId, agentId, request);
        return ResponseEntity.noContent().build();
    }
}
