package com.resolveiq.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.resolveiq.rag.adapter.out.ai.DeterministicEmbeddingAdapter;
import com.resolveiq.rag.application.dto.TicketSimilarityRequest;
import com.resolveiq.rag.application.dto.TicketSimilarityResponse;
import com.resolveiq.rag.application.service.TicketSimilarityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class TicketSimilarityServiceTest {

    private TicketSimilarityService similarityService;

    @BeforeEach
    void setUp() {
        similarityService = new TicketSimilarityService(new DeterministicEmbeddingAdapter(384));
    }

    @Test
    @DisplayName("Should rank similar tickets higher and boost on matching error fingerprint")
    void testTicketSimilarityRanking() {
        UUID tenantId = UUID.randomUUID();
        UUID sourceTicketId = UUID.randomUUID();
        UUID similarTicketId = UUID.randomUUID();
        UUID unrelatedTicketId = UUID.randomUUID();

        TicketSimilarityRequest.CandidateTicket candidate1 = new TicketSimilarityRequest.CandidateTicket(
            similarTicketId,
            "Payment gateway timeout when charging credit card duplicate authorization",
            "BILLING",
            "Checkout API",
            "ERR_PAYMENT_GATEWAY_TIMEOUT"
        );

        TicketSimilarityRequest.CandidateTicket candidate2 = new TicketSimilarityRequest.CandidateTicket(
            unrelatedTicketId,
            "Cannot reset user password after multi-factor authentication setup",
            "SECURITY",
            "Auth Portal",
            "ERR_MFA_CODE_INVALID"
        );

        TicketSimilarityRequest request = new TicketSimilarityRequest(
            sourceTicketId,
            "Payment gateway timeout during duplicate checkout charge attempt",
            "BILLING",
            "Checkout API",
            "ERR_PAYMENT_GATEWAY_TIMEOUT",
            0.50,
            List.of(candidate1, candidate2)
        );

        TicketSimilarityResponse response = similarityService.computeSimilarity(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.matches()).isNotEmpty();
        assertThat(response.matches().get(0).ticketId()).isEqualTo(similarTicketId);
        assertThat(response.matches().get(0).fingerprintMatched()).isTrue();
    }
}
