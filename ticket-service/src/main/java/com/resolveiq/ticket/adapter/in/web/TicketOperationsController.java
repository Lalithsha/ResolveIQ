package com.resolveiq.ticket.adapter.in.web;

import com.resolveiq.ticket.domain.repository.OutboxEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ticket-operations")
public class TicketOperationsController {
    private final OutboxEventRepository outbox;

    public TicketOperationsController(OutboxEventRepository outbox) { this.outbox = outbox; }

    @GetMapping("/outbox-summary")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<Map<String, Long>> outboxSummary(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        Map<String, Long> result = new LinkedHashMap<>(Map.of("PENDING", 0L, "RETRY", 0L, "DEAD", 0L, "PUBLISHED", 0L));
        outbox.countByStatusForTenant(tenantId).forEach(row -> result.put(String.valueOf(row[0]), ((Number) row[1]).longValue()));
        return ResponseEntity.ok(result);
    }
}
