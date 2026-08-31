package com.resolveiq.analysis.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.resolveiq.analysis.application.port.ChatClientPort;
import com.resolveiq.analysis.application.port.ChatProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.chat-provider", havingValue = "gemini")
public class GeminiChatClientAdapter implements ChatClientPort {
    private final RestClient client;
    private final String apiKey;
    private final String model;
    private final long inputCostPerMillionMicros;
    private final long outputCostPerMillionMicros;

    public GeminiChatClientAdapter(
        @Value("${resolveiq.ai.gemini-base-url:https://generativelanguage.googleapis.com}") String baseUrl,
        @Value("${resolveiq.ai.api-key}") String apiKey,
        @Value("${resolveiq.ai.chat-model:gemini-2.0-flash}") String model,
        @Value("${resolveiq.ai.cost.input-per-million-micros:100000}") long inputCostPerMillionMicros,
        @Value("${resolveiq.ai.cost.output-per-million-micros:400000}") long outputCostPerMillionMicros
    ) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("mock")) throw new IllegalStateException("A real Gemini API key is required");
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey; this.model = model;
        this.inputCostPerMillionMicros = inputCostPerMillionMicros;
        this.outputCostPerMillionMicros = outputCostPerMillionMicros;
    }

    @Override
    public ChatProviderResponse generateResponse(String systemPrompt, String userPrompt) {
        JsonNode response = client.post()
            .uri(builder -> builder.path("/v1beta/models/{model}:generateContent").queryParam("key", apiKey).build(model))
            .body(Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0, "responseMimeType", "application/json")
            )).retrieve().body(JsonNode.class);
        JsonNode content = response == null ? null : response.at("/candidates/0/content/parts/0/text");
        if (content == null || !content.isTextual() || content.asText().isBlank()) throw new IllegalStateException("Gemini returned no content");
        int inputTokens = response.path("usageMetadata").path("promptTokenCount").asInt(0);
        int outputTokens = response.path("usageMetadata").path("candidatesTokenCount").asInt(0);
        long cost = (inputTokens * inputCostPerMillionMicros + outputTokens * outputCostPerMillionMicros) / 1_000_000;
        return new ChatProviderResponse(content.asText(), inputTokens, outputTokens, cost, response.path("responseId").asText(null));
    }

    @Override public String getModelName() { return model; }
}
