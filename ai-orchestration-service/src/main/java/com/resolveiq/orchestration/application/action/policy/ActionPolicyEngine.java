package com.resolveiq.orchestration.application.action.policy;

import com.resolveiq.orchestration.domain.model.action.ActionType;
import com.resolveiq.orchestration.domain.model.action.PolicyDecision;
import com.resolveiq.orchestration.domain.model.action.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ActionPolicyEngine {

    public static final String POLICY_VERSION = "1.0";
    public static final long DEFAULT_AGENT_REFUND_LIMIT_CENTS = 5000L; // $50.00

    public record PolicyEvaluation(
            PolicyDecision decision,
            List<String> matchedRules,
            List<String> requiredPermissions,
            int requiredApprovalCount,
            Long financialLimitCents,
            List<String> reasonCodes
    ) {}

    public PolicyEvaluation evaluate(ActionType actionType, RiskLevel riskLevel, Map<String, Object> normalizedInput, boolean validationPassed, List<String> validationErrors) {
        List<String> matchedRules = new ArrayList<>();
        List<String> requiredPermissions = new ArrayList<>();
        List<String> reasonCodes = new ArrayList<>();

        if (!validationPassed) {
            return new PolicyEvaluation(
                    PolicyDecision.DENIED,
                    List.of("PRECONDITION_VALIDATION_FAILED"),
                    List.of(),
                    1,
                    null,
                    validationErrors
            );
        }

        switch (actionType) {
            case REFUND_DUPLICATE_CHARGE -> {
                long amountCents = 0L;
                if (normalizedInput.get("amountCents") instanceof Number num) {
                    amountCents = num.longValue();
                }

                if (amountCents <= DEFAULT_AGENT_REFUND_LIMIT_CENTS) {
                    matchedRules.add("RULE_REFUND_UNDER_AGENT_LIMIT");
                    requiredPermissions.add("ACTION_APPROVE_LOW_RISK");
                    return new PolicyEvaluation(
                            PolicyDecision.REQUIRES_APPROVAL,
                            matchedRules,
                            requiredPermissions,
                            1,
                            DEFAULT_AGENT_REFUND_LIMIT_CENTS,
                            List.of("STANDARD_AGENT_REFUND_APPROVAL_REQUIRED")
                    );
                } else {
                    matchedRules.add("RULE_REFUND_EXCEEDS_AGENT_LIMIT");
                    requiredPermissions.add("ACTION_APPROVE_FINANCIAL");
                    int approvalCount = riskLevel == RiskLevel.CRITICAL ? 2 : 1;
                    return new PolicyEvaluation(
                            PolicyDecision.REQUIRES_APPROVAL,
                            matchedRules,
                            requiredPermissions,
                            approvalCount,
                            DEFAULT_AGENT_REFUND_LIMIT_CENTS,
                            List.of("HIGH_VALUE_FINANCIAL_APPROVAL_REQUIRED")
                    );
                }
            }

            case UNLOCK_ACCOUNT -> {
                matchedRules.add("RULE_ACCOUNT_UNLOCK_VERIFIED_IDENTITY");
                requiredPermissions.add("ACTION_APPROVE_LOW_RISK");
                return new PolicyEvaluation(
                        PolicyDecision.REQUIRES_APPROVAL,
                        matchedRules,
                        requiredPermissions,
                        1,
                        null,
                        List.of("RECENT_IDENTITY_CHALLENGE_VERIFIED")
                );
            }

            default -> {
                return new PolicyEvaluation(
                        PolicyDecision.DENIED,
                        List.of("RULE_UNSUPPORTED_ACTION"),
                        List.of(),
                        1,
                        null,
                        List.of("ACTION_TYPE_NOT_PERMITTED")
                );
            }
        }
    }
}
