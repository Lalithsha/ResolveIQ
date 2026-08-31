package com.resolveiq.analysis.application.port;

public record ChatProviderResponse(
    String content,
    int inputTokens,
    int outputTokens,
    long estimatedCostMicros,
    String requestId
) {
    public ChatProviderResponse {
        content = content == null ? "" : content;
        inputTokens = Math.max(0, inputTokens);
        outputTokens = Math.max(0, outputTokens);
        estimatedCostMicros = Math.max(0, estimatedCostMicros);
    }
}
