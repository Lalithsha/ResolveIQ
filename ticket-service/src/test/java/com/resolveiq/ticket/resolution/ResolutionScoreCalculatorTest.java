package com.resolveiq.ticket.resolution;

import com.resolveiq.ticket.application.service.resolution.ResolutionScoreCalculator;
import com.resolveiq.ticket.domain.model.resolution.OutcomeRating;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolutionScoreCalculatorTest {

    @Test
    @DisplayName("Latest YES yields score +100")
    void yesYields100() {
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.YES, false, false, false);
        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("YES with reconciled action clamps at +100")
    void yesWithActionClampsAt100() {
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.YES, false, false, true);
        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("Latest PARTLY yields score +20")
    void partlyYields20() {
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.PARTLY, false, false, false);
        assertThat(score).isEqualTo(20);
    }

    @Test
    @DisplayName("Latest NO is capped at -50")
    void noCappedAtMinus50() {
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.NO, false, false, false);
        assertThat(score).isLessThanOrEqualTo(-50);
    }

    @Test
    @DisplayName("Explicit reopen caps score at -50 even if earlier was YES")
    void reopenCapsAtMinus50() {
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.YES, true, false, false);
        assertThat(score).isLessThanOrEqualTo(-50);
    }

    @Test
    @DisplayName("Confirmed repeat contact applies -50 penalty")
    void repeatContactPenalty() {
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.YES, false, true, false);
        assertThat(score).isEqualTo(50); // 100 - 50 = 50
    }
}
