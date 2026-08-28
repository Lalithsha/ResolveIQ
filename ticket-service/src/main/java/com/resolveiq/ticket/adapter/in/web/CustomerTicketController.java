package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.contracts.tracing.CorrelationContext;
import com.resolveiq.ticket.application.dto.*;
import com.resolveiq.ticket.application.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/tickets")
public class CustomerTicketController {

    private final TicketService ticketService;

    public CustomerTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @Valid @RequestBody CreateTicketRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID customerId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        TicketResponse response = ticketService.createTicket(tenantId, customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getCustomerTickets(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID customerId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        List<TicketResponse> tickets = ticketService.listCustomerTickets(tenantId, customerId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable("id") UUID ticketId
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        TicketResponse ticket = ticketService.getTicketById(tenantId, ticketId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessageResponse> addCustomerMessage(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @RequestHeader(value = "X-User-Id", required = false) String userHeader,
        @PathVariable("id") UUID ticketId,
        @Valid @RequestBody AddMessageRequest request
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID customerId = userHeader != null ? UUID.fromString(userHeader) : UUID.randomUUID();

        TicketMessageResponse response = ticketService.addMessage(tenantId, ticketId, customerId, "CUSTOMER", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<TicketMessageResponse>> getTicketMessages(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable("id") UUID ticketId
    ) {
        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<TicketMessageResponse> messages = ticketService.getTicketMessages(tenantId, ticketId);
        return ResponseEntity.ok(messages);
    }
}
