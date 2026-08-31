package com.resolveiq.analysis.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.analysis.application.port.ChatClientPort;
import com.resolveiq.analysis.application.port.ChatProviderResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.chat-provider", havingValue = "deterministic", matchIfMissing = true)
public class DeterministicChatClientAdapter implements ChatClientPort {

    private final ObjectMapper objectMapper;

    public DeterministicChatClientAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatProviderResponse generateResponse(String systemPrompt, String userPrompt) {
        String normalized = userPrompt == null ? "" : userPrompt.toLowerCase(Locale.ROOT);
        String intent = normalized.contains("charged") || normalized.contains("payment") ? "billing_issue"
            : normalized.contains("password") || normalized.contains("login") ? "account_access"
            : normalized.contains("error") || normalized.contains("broken") ? "technical_issue"
            : "general_inquiry";
        String category = switch (intent) {
            case "billing_issue" -> "BILLING";
            case "account_access" -> "ACCOUNT";
            case "technical_issue" -> "TECHNICAL";
            default -> "GENERAL";
        };
        String urgency = normalized.contains("urgent") || normalized.contains("blocked") ? "HIGH" : "MEDIUM";
        try {
            String content = objectMapper.writeValueAsString(Map.of(
                "intent", intent,
                "category", category,
                "sentiment", normalized.contains("angry") ? "NEGATIVE" : "NEUTRAL",
                "sentimentConfidence", 0.82,
                "urgency", urgency,
                "urgencyConfidence", 0.84,
                "language", "en",
                "redactedEntities", Map.of(),
                "policyFlags", Map.of(),
                "providerMode", "DETERMINISTIC"
            ));
            return new ChatProviderResponse(content, estimate(systemPrompt + userPrompt), estimate(content), 0, "deterministic");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize deterministic analysis", e);
        }
    }

    private int estimate(String text) { return Math.max(1, (text == null ? 0 : text.length() + 3) / 4); }

    @Override
    public String getModelName() {
        return "resolveiq-deterministic-chat-v1";
    }
}
