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
@RequestMapping("/api/v1/customer/tickets")
public class CustomerTicketController {

    private final TicketService ticketService;

    public CustomerTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody CreateTicketRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID customerId = parseRequiredUuid(userHeader, "X-User-Id");

        TicketResponse response = ticketService.createTicket(tenantId, customerId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getCustomerTickets(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID customerId = parseRequiredUuid(userHeader, "X-User-Id");

        List<TicketResponse> tickets = ticketService.listCustomerTickets(tenantId, customerId, page, size);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable(value = "id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID customerId = parseRequiredUuid(userHeader, "X-User-Id");

        TicketResponse ticket = ticketService.getCustomerTicketById(tenantId, customerId, ticketId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessageResponse> addCustomerMessage(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable(value = "id") UUID ticketId,
        @Valid @RequestBody AddMessageRequest request
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID customerId = parseRequiredUuid(userHeader, "X-User-Id");

        TicketMessageResponse response = ticketService.addMessage(tenantId, ticketId, customerId, "CUSTOMER", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<TicketMessageResponse>> getTicketMessages(
        @RequestHeader(value = "X-Tenant-Id") String tenantHeader,
        @RequestHeader(value = "X-User-Id") String userHeader,
        @PathVariable(value = "id") UUID ticketId
    ) {
        UUID tenantId = parseRequiredUuid(tenantHeader, "X-Tenant-Id");
        UUID customerId = parseRequiredUuid(userHeader, "X-User-Id");

        List<TicketMessageResponse> messages = ticketService.getCustomerTicketMessages(tenantId, customerId, ticketId);
        return ResponseEntity.ok(messages);
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
