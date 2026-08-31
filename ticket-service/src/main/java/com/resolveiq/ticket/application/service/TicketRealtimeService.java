package com.resolveiq.ticket.application.service;

import com.resolveiq.ticket.application.dto.TicketResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TicketRealtimeService {
    private final ConcurrentHashMap<UUID, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository memberships;

    public TicketRealtimeService(com.resolveiq.ticket.domain.repository.StaffTeamMembershipRepository memberships) {
        this.memberships = memberships;
    }

    public SseEmitter subscribe(UUID tenantId, UUID actorId, Set<String> roles) {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);
        UUID subscriptionId = UUID.randomUUID();
        subscriptions.put(subscriptionId, new Subscription(tenantId, actorId, roles, emitter));
        emitter.onCompletion(() -> subscriptions.remove(subscriptionId));
        emitter.onTimeout(() -> subscriptions.remove(subscriptionId));
        emitter.onError(error -> subscriptions.remove(subscriptionId));
        try {
            emitter.send(SseEmitter.event().name("connected").data(new StreamStatus(Instant.now(), "connected")));
        } catch (IOException error) {
            subscriptions.remove(subscriptionId);
            emitter.completeWithError(error);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void ticketChanged(TicketChangedEvent event) {
        TicketResponse ticket = event.ticket();
        subscriptions.forEach((id, subscription) -> {
            if (!subscription.tenantId().equals(ticket.tenantId()) || !canRead(subscription, ticket)) return;
            try {
                subscription.emitter().send(SseEmitter.event().name(event.eventType()).id(ticket.version() + "")
                    .data(new TicketStreamEvent(ticket.id(), ticket.ticketNumber(), ticket.status(), ticket.priority(),
                        ticket.teamId(), ticket.assignedAgentId(), ticket.updatedAt())));
            } catch (IOException error) {
                subscriptions.remove(id);
                safeComplete(subscription.emitter());
            }
        });
    }

    @Scheduled(fixedDelay = 25000)
    public void heartbeat() {
        subscriptions.forEach((id, subscription) -> {
            try {
                subscription.emitter().send(SseEmitter.event().name("heartbeat").data(Instant.now().toString()));
            } catch (IOException error) {
                subscriptions.remove(id);
                safeComplete(subscription.emitter());
            }
        });
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException ignored) {
            // The servlet container can already have completed an errored async
            // response. Removing the subscription is the required cleanup.
        }
    }

    private boolean canRead(Subscription subscription, TicketResponse ticket) {
        if (subscription.roles().contains("ADMIN") || subscription.roles().contains("AUDITOR")) return true;
        if (subscription.roles().contains("TEAM_LEAD") && ticket.teamId() != null) {
            return memberships.existsByTenantIdAndUserIdAndTeamIdAndActiveTrue(
                subscription.tenantId(), subscription.actorId(), ticket.teamId());
        }
        return subscription.roles().contains("AGENT") && subscription.actorId().equals(ticket.assignedAgentId());
    }

    private record Subscription(UUID tenantId, UUID actorId, Set<String> roles, SseEmitter emitter) {}
    private record StreamStatus(Instant at, String status) {}
    private record TicketStreamEvent(UUID ticketId, String ticketNumber, Object status, Object priority,
                                     UUID teamId, UUID assignedAgentId, Instant updatedAt) {}
}
