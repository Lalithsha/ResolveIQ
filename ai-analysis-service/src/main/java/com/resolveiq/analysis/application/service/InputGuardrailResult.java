package com.resolveiq.analysis.application.service;

public record InputGuardrailResult(
    String sanitizedSubject,
    String sanitizedDescription,
    String outcome,
    String findingsJson,
    boolean blocked,
    int estimatedInputTokens
) {}
