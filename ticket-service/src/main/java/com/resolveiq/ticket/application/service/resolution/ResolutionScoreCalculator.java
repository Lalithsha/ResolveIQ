package com.resolveiq.ticket.application.service.resolution;

import com.resolveiq.ticket.domain.model.resolution.OutcomeRating;

public final class ResolutionScoreCalculator {

    private ResolutionScoreCalculator() {}

    public static int calculateScoreV1(OutcomeRating latestRating, boolean hasExplicitReopen,
                                       boolean hasConfirmedRepeat, boolean hasReconciledAction) {
        int score = 0;

        if (latestRating == OutcomeRating.YES) {
            score += 100;
            if (hasReconciledAction) {
                score += 10;
            }
        } else if (latestRating == OutcomeRating.PARTLY) {
            score += 20;
        } else if (latestRating == OutcomeRating.NO) {
            score -= 100;
        }

        if (hasExplicitReopen) {
            score -= 100;
        }

        if (hasConfirmedRepeat) {
            score -= 50;
        }

        // Clamp to [-100, 100]
        score = Math.max(-100, Math.min(100, score));

        // Rule: A current No or explicit reopen caps the score at -50
        if (latestRating == OutcomeRating.NO || hasExplicitReopen) {
            score = Math.min(-50, score);
        }

        return score;
    }
}
