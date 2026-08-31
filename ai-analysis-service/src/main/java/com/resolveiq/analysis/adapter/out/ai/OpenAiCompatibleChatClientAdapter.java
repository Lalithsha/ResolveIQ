package com.resolveiq.analysis.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.resolveiq.analysis.application.port.ChatClientPort;
import com.resolveiq.analysis.application.port.ChatProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.chat-provider", havingValue = "openai-compatible")
public class OpenAiCompatibleChatClientAdapter implements ChatClientPort {

    private final RestClient client;
    private final String model;
    private final long inputCostPerMillionMicros;
    private final long outputCostPerMillionMicros;

    public OpenAiCompatibleChatClientAdapter(
        @Value("${resolveiq.ai.base-url:https://api.openai.com/v1}") String baseUrl,
        @Value("${resolveiq.ai.api-key}") String apiKey,
        @Value("${resolveiq.ai.chat-model:gpt-4o-mini}") String model,
        @Value("${resolveiq.ai.cost.input-per-million-micros:150000}") long inputCostPerMillionMicros,
        @Value("${resolveiq.ai.cost.output-per-million-micros:600000}") long outputCostPerMillionMicros
    ) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("mock")) {
            throw new IllegalStateException("A non-placeholder AI API key is required for openai-compatible chat");
        }
        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
        this.model = model;
        this.inputCostPerMillionMicros = inputCostPerMillionMicros;
        this.outputCostPerMillionMicros = outputCostPerMillionMicros;
    }

    @Override
    public ChatProviderResponse generateResponse(String systemPrompt, String userPrompt) {
        JsonNode response = client.post()
            .uri("/chat/completions")
            .body(Map.of(
                "model", model,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                )
            ))
            .retrieve()
            .body(JsonNode.class);
        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || !content.isTextual() || content.asText().isBlank()) {
            throw new IllegalStateException("Chat provider returned no message content");
        }
        int inputTokens = response.path("usage").path("prompt_tokens").asInt(0);
        int outputTokens = response.path("usage").path("completion_tokens").asInt(0);
        long cost = (inputTokens * inputCostPerMillionMicros + outputTokens * outputCostPerMillionMicros) / 1_000_000;
        return new ChatProviderResponse(content.asText(), inputTokens, outputTokens, cost, response.path("id").asText(null));
    }

    @Override
    public String getModelName() {
        return model;
    }
}
