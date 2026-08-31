package com.resolveiq.orchestration.application.service;

import com.resolveiq.contracts.event.TicketEvents;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowOutboxPublisherServiceTest {

    @Test
    void publishesCompletedTriageToTheConsumerContractTopic() {
        assertThat(WorkflowOutboxPublisherService.resolveTopicForEventType(TicketEvents.TICKET_TRIAGE_COMPLETED))
            .isEqualTo(TicketEvents.TICKET_TRIAGE_COMPLETED);
    }

    @Test
    void publishesFailedTriageToTheConsumerContractTopic() {
        assertThat(WorkflowOutboxPublisherService.resolveTopicForEventType(TicketEvents.TICKET_TRIAGE_FAILED))
            .isEqualTo(TicketEvents.TICKET_TRIAGE_FAILED);
    }
}
