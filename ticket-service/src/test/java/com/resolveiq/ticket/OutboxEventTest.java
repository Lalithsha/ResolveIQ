package com.resolveiq.ticket;

import com.resolveiq.ticket.domain.model.OutboxEvent;
import com.resolveiq.ticket.domain.model.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {
    @Test
    void retryExhaustionMovesTheEventToDeadState() {
        OutboxEvent event = new OutboxEvent("ticket", UUID.randomUUID(), "resolveiq.ticket.created", "{}");
        for (int attempt = 0; attempt < 4; attempt++) {
            event.markFailed("BROKER_UNAVAILABLE");
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.RETRY);
        }
        event.markFailed("BROKER_UNAVAILABLE");
        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
    }
}
