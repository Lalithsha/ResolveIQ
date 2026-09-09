package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.domain.model.action.RiskLevel;
import java.util.List;

public record ValidationResult(
        boolean isValid,
        RiskLevel riskLevel,
        List<String> reasonCodes,
        String details
) {
    public static ValidationResult valid(RiskLevel riskLevel) {
        return new ValidationResult(true, riskLevel, List.of(), "Validation successful");
    }

    public static ValidationResult invalid(String reasonCode, String details) {
        return new ValidationResult(false, RiskLevel.LOW, List.of(reasonCode), details);
    }
}
