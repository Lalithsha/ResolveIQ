package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.application.action.policy.ActionPolicyEngine;
import com.resolveiq.orchestration.domain.model.action.ActionType;
import com.resolveiq.orchestration.domain.model.action.PolicyDecision;
import com.resolveiq.orchestration.domain.model.action.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActionPolicyEngineTest {

    private ActionPolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        policyEngine = new ActionPolicyEngine();
    }

    @Test
    @DisplayName("Refund <= 5000 cents requires single low-risk agent approval")
    void refundUnderAgentLimitRequiresLowRiskApproval() {
        var eval = policyEngine.evaluate(
                ActionType.REFUND_DUPLICATE_CHARGE,
                RiskLevel.LOW,
                Map.of("amountCents", 4500L, "currency", "USD"),
                true,
                List.of()
        );

        assertThat(eval.decision()).isEqualTo(PolicyDecision.REQUIRES_APPROVAL);
        assertThat(eval.requiredApprovalCount()).isEqualTo(1);
        assertThat(eval.requiredPermissions()).contains("ACTION_APPROVE_LOW_RISK");
        assertThat(eval.matchedRules()).contains("RULE_REFUND_UNDER_AGENT_LIMIT");
    }

    @Test
    @DisplayName("Refund > 5000 cents requires financial approval permission")
    void refundOverAgentLimitRequiresFinancialApproval() {
        var eval = policyEngine.evaluate(
                ActionType.REFUND_DUPLICATE_CHARGE,
                RiskLevel.HIGH,
                Map.of("amountCents", 12000L, "currency", "USD"),
                true,
                List.of()
        );

        assertThat(eval.decision()).isEqualTo(PolicyDecision.REQUIRES_APPROVAL);
        assertThat(eval.requiredPermissions()).contains("ACTION_APPROVE_FINANCIAL");
        assertThat(eval.matchedRules()).contains("RULE_REFUND_EXCEEDS_AGENT_LIMIT");
    }

    @Test
    @DisplayName("Unlock account requires verified identity rule match")
    void unlockAccountRequiresVerifiedIdentityRule() {
        var eval = policyEngine.evaluate(
                ActionType.UNLOCK_ACCOUNT,
                RiskLevel.LOW,
                Map.of("userId", "1234"),
                true,
                List.of()
        );

        assertThat(eval.decision()).isEqualTo(PolicyDecision.REQUIRES_APPROVAL);
        assertThat(eval.requiredPermissions()).contains("ACTION_APPROVE_LOW_RISK");
        assertThat(eval.matchedRules()).contains("RULE_ACCOUNT_UNLOCK_VERIFIED_IDENTITY");
    }

    @Test
    @DisplayName("Validation failure denies action immediately regardless of confidence")
    void validationFailureDeniesActionImmediately() {
        var eval = policyEngine.evaluate(
                ActionType.REFUND_DUPLICATE_CHARGE,
                RiskLevel.HIGH,
                Map.of("amountCents", 5000L),
                false,
                List.of("DUPLICATE_RELATION_UNVERIFIED")
        );

        assertThat(eval.decision()).isEqualTo(PolicyDecision.DENIED);
        assertThat(eval.reasonCodes()).contains("DUPLICATE_RELATION_UNVERIFIED");
    }
}
