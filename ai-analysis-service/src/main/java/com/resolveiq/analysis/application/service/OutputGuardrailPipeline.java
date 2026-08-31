package com.resolveiq.analysis.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OutputGuardrailPipeline {
    private static final Set<String> REQUIRED = Set.of(
        "intent", "category", "sentiment", "sentimentConfidence", "urgency", "urgencyConfidence",
        "language", "redactedEntities", "policyFlags"
    );
    private final ObjectMapper objectMapper;

    public OutputGuardrailPipeline(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedAnalysis validate(String rawOutput) {
        try {
            JsonNode node = objectMapper.readTree(rawOutput);
            if (node == null || !node.isObject()) throw new IllegalArgumentException("Analysis must be an object");
            for (String field : REQUIRED) if (!node.hasNonNull(field)) throw new IllegalArgumentException("Missing field: " + field);
            String intent = node.get("intent").asText();
            String category = node.get("category").asText();
            String sentiment = node.get("sentiment").asText();
            String urgency = node.get("urgency").asText();
            String language = node.get("language").asText();
            if (!intent.matches("[a-z][a-z0-9_]{1,99}")) throw new IllegalArgumentException("Invalid intent");
            if (!Set.of("BILLING", "ACCOUNT", "TECHNICAL", "DELIVERY", "GENERAL").contains(category)) throw new IllegalArgumentException("Invalid category");
            if (!Set.of("POSITIVE", "NEUTRAL", "NEGATIVE").contains(sentiment)) throw new IllegalArgumentException("Invalid sentiment");
            if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(urgency)) throw new IllegalArgumentException("Invalid urgency");
            if (!validConfidence(node.get("sentimentConfidence")) || !validConfidence(node.get("urgencyConfidence"))) throw new IllegalArgumentException("Invalid confidence");
            if (!language.matches("[a-z]{2}(-[A-Z]{2})?")) throw new IllegalArgumentException("Invalid language");
            if (!node.get("redactedEntities").isObject() || !node.get("policyFlags").isObject()) throw new IllegalArgumentException("Invalid safety fields");
            return new ValidatedAnalysis(intent, category, sentiment, node.get("sentimentConfidence").asDouble(), urgency,
                node.get("urgencyConfidence").asDouble(), language, node.get("redactedEntities").toString(),
                node.get("policyFlags").toString(), "VALID");
        } catch (Exception invalid) {
            return fallback("INVALID_PROVIDER_OUTPUT");
        }
    }

    public ValidatedAnalysis blockedFallback() {
        return fallback("INPUT_GUARDRAIL_BLOCKED");
    }

    public ValidatedAnalysis budgetFallback(String reason) {
        return fallback(reason);
    }

    private ValidatedAnalysis fallback(String reason) {
        return new ValidatedAnalysis("general_inquiry", "GENERAL", "NEUTRAL", 0.35, "MEDIUM", 0.35,
            "en", "{}", "{\"fallback\":true,\"reason\":\"" + reason + "\"}", "FALLBACK_" + reason);
    }

    private boolean validConfidence(JsonNode value) {
        return value.isNumber() && value.asDouble() >= 0 && value.asDouble() <= 1;
    }
}
