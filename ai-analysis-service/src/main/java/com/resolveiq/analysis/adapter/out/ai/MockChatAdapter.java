package com.resolveiq.analysis.adapter.out.ai;

import com.resolveiq.analysis.application.port.ChatClientPort;
import com.resolveiq.analysis.application.port.ChatProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.chat-provider", havingValue = "mock")
public class MockChatAdapter implements ChatClientPort {

    private final String modelName;

    public MockChatAdapter(@Value("${resolveiq.ai.chat-model:mock-model-v1}") String modelName) {
        this.modelName = modelName;
    }

    @Override
    public ChatProviderResponse generateResponse(String systemPrompt, String userPrompt) {
        String lower = userPrompt.toLowerCase();

        String intent = "general_inquiry";
        String category = "TECHNICAL";
        String sentiment = "NEUTRAL";
        double sentimentConf = 0.85;
        String urgency = "MEDIUM";
        double urgencyConf = 0.80;

        if (lower.contains("charge") || lower.contains("billing") || lower.contains("refund") || lower.contains("invoice")) {
            intent = "billing_dispute";
            category = "BILLING";
            sentiment = "NEGATIVE";
            sentimentConf = 0.92;
            urgency = "HIGH";
            urgencyConf = 0.90;
        } else if (lower.contains("sso") || lower.contains("login") || lower.contains("password") || lower.contains("auth")) {
            intent = "authentication_issue";
            category = "TECHNICAL";
            sentiment = "NEGATIVE";
            sentimentConf = 0.88;
            urgency = "CRITICAL";
            urgencyConf = 0.95;
        } else if (lower.contains("package") || lower.contains("delivery") || lower.contains("order")) {
            intent = "delivery_inquiry";
            category = "DELIVERY";
            sentiment = "NEUTRAL";
            sentimentConf = 0.80;
            urgency = "MEDIUM";
            urgencyConf = 0.85;
        }

        String content = String.format("""
            {
              "intent": "%s",
              "category": "%s",
              "sentiment": "%s",
              "sentimentConfidence": %.2f,
              "urgency": "%s",
              "urgencyConfidence": %.2f,
              "language": "en",
              "redactedEntities": {},
              "policyFlags": {"potentialAbuse": false}
            }
            """, intent, category, sentiment, sentimentConf, urgency, urgencyConf);
        return new ChatProviderResponse(content, Math.max(1, (systemPrompt.length() + userPrompt.length()) / 4),
            Math.max(1, content.length() / 4), 0, "mock");
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
