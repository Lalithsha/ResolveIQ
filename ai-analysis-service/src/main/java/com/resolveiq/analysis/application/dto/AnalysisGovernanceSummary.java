package com.resolveiq.analysis.application.dto;

import java.util.List;

public record AnalysisGovernanceSummary(
    long totalInvocations,
    long validInvocations,
    long blockedInvocations,
    long fallbackInvocations,
    long inputTokens,
    long outputTokens,
    long estimatedCostMicros,
    List<AnalysisTraceResponse> recentTraces
) {}
