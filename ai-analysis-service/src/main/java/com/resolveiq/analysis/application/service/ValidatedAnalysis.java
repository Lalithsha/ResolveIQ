package com.resolveiq.analysis.application.service;

public record ValidatedAnalysis(
    String intent,
    String category,
    String sentiment,
    double sentimentConfidence,
    String urgency,
    double urgencyConfidence,
    String language,
    String redactedEntities,
    String policyFlags,
    String outcome
) {}
