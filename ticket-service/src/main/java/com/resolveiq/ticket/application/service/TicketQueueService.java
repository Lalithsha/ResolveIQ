package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.application.dto.TicketQueueResponse;
import com.resolveiq.ticket.application.dto.TicketResponse;
import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketPriority;
import com.resolveiq.ticket.domain.model.TicketStatus;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketQueueService {
    private static final Set<String> SORT_FIELDS = Set.of(
        "createdAt", "updatedAt", "priority", "firstResponseDueAt", "resolutionDueAt");
    private final TicketRepository tickets;
    private final com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository memberships;

    public TicketQueueService(TicketRepository tickets, com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository memberships) {
        this.tickets = tickets;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public TicketQueueResponse search(UUID tenantId, UUID actorId, Set<String> roles, String scope, UUID teamId,
                                      TicketStatus status, TicketPriority priority, String queryText,
                                      String sortField, String direction, int page, int size) {
        String normalizedScope = scope == null || scope.isBlank() ? "mine" : scope.toLowerCase(Locale.ROOT);
        boolean admin = roles.contains("ADMIN");
        boolean teamLead = roles.contains("TEAM_LEAD");
        boolean auditor = roles.contains("AUDITOR");
        if (!Set.of("mine", "team", "all", "sla-risk").contains(normalizedScope)) {
            throw new IllegalArgumentException("Unsupported queue scope");
        }
        if (("team".equals(normalizedScope) || "sla-risk".equals(normalizedScope)) && !(admin || auditor)) {
            if (teamId == null || !memberships.existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(tenantId, actorId, teamId)) {
                throw new SecurityException("This role cannot access the requested team queue");
            }
        }
        if ("all".equals(normalizedScope) && !(admin || auditor)) {
            throw new SecurityException("Only administrators and auditors can access the tenant-wide queue");
        }

        String safeSort = SORT_FIELDS.contains(sortField) ? sortField : "createdAt";
        Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        Specification<Ticket> specification = (root, ignored, builder) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));
            if ("mine".equals(normalizedScope) || ("sla-risk".equals(normalizedScope) && !(admin || teamLead || auditor))) {
                predicates.add(builder.equal(root.get("assignedAgentId"), actorId));
            }
            if (("team".equals(normalizedScope) || "sla-risk".equals(normalizedScope)) && teamId != null) {
                predicates.add(builder.equal(root.get("teamId"), teamId));
            }
            if ("sla-risk".equals(normalizedScope)) {
                predicates.add(builder.not(root.get("status").in(TicketStatus.RESOLVED, TicketStatus.CLOSED)));
                Predicate responseRisk = builder.lessThanOrEqualTo(root.get("firstResponseDueAt"), Instant.now().plus(2, ChronoUnit.HOURS));
                Predicate resolutionRisk = builder.lessThanOrEqualTo(root.get("resolutionDueAt"), Instant.now().plus(4, ChronoUnit.HOURS));
                predicates.add(builder.or(responseRisk, resolutionRisk));
            }
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (priority != null) predicates.add(builder.equal(root.get("priority"), priority));
            if (queryText != null && !queryText.isBlank()) {
                String pattern = "%" + queryText.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("ticketNumber")), pattern),
                    builder.like(builder.lower(root.get("subject")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<Ticket> result = tickets.findAll(specification, PageRequest.of(safePage, safeSize, Sort.by(safeDirection, safeSort)));
        return new TicketQueueResponse(
            result.getContent().stream().map(TicketResponse::fromEntity).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
