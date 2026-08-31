package com.resolveiq.analysis.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputGuardrailPipelineTest {
    private final OutputGuardrailPipeline pipeline = new OutputGuardrailPipeline(new ObjectMapper());

    @Test
    void acceptsOnlyTheStructuredClassificationContract() {
        ValidatedAnalysis result = pipeline.validate("""
            {"intent":"billing_issue","category":"BILLING","sentiment":"NEGATIVE","sentimentConfidence":0.9,
             "urgency":"HIGH","urgencyConfidence":0.8,"language":"en","redactedEntities":{},"policyFlags":{}}
            """);
        assertThat(result.outcome()).isEqualTo("VALID");
        assertThat(result.category()).isEqualTo("BILLING");
    }

    @Test
    void returnsAnExplicitLowConfidenceFallbackForInvalidProviderOutput() {
        ValidatedAnalysis result = pipeline.validate("not-json");
        assertThat(result.outcome()).isEqualTo("FALLBACK_INVALID_PROVIDER_OUTPUT");
        assertThat(result.urgencyConfidence()).isEqualTo(0.35);
    }
}
