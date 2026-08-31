package com.resolveiq.rag.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.resolveiq.rag.application.port.EmbeddingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.embedding-provider", havingValue = "openai-compatible")
public class OpenAiCompatibleEmbeddingAdapter implements EmbeddingPort {

    private final RestClient client;
    private final String model;
    private final int dimension;

    public OpenAiCompatibleEmbeddingAdapter(
        @Value("${resolveiq.ai.base-url:https://api.openai.com/v1}") String baseUrl,
        @Value("${resolveiq.ai.api-key}") String apiKey,
        @Value("${resolveiq.ai.embedding-model:text-embedding-3-small}") String model,
        @Value("${resolveiq.ai.embedding-dimension:1536}") int dimension
    ) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("mock")) {
            throw new IllegalStateException("A non-placeholder AI API key is required for embeddings");
        }
        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
        this.model = model;
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        JsonNode response = client.post().uri("/embeddings")
            .body(Map.of("model", model, "input", text == null ? "" : text, "dimensions", dimension))
            .retrieve().body(JsonNode.class);
        JsonNode values = response == null ? null : response.at("/data/0/embedding");
        if (values == null || !values.isArray() || values.size() != dimension) {
            throw new IllegalStateException("Embedding provider returned an unexpected dimension");
        }
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) values.get(i).asDouble();
            if (!Float.isFinite(vector[i])) throw new IllegalStateException("Embedding contains a non-finite value");
        }
        return vector;
    }

    @Override public int getDimension() { return dimension; }
    @Override public String getModelName() { return model; }
}
