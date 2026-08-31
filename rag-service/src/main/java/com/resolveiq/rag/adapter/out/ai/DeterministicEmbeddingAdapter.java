package com.resolveiq.rag.adapter.out.ai;

import com.resolveiq.rag.application.port.EmbeddingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.embedding-provider", havingValue = "deterministic", matchIfMissing = true)
public class DeterministicEmbeddingAdapter implements EmbeddingPort {

    private final int dimension;

    public DeterministicEmbeddingAdapter(@Value("${resolveiq.ai.embedding-dimension:1536}") int dimension) {
        if (dimension <= 0) throw new IllegalArgumentException("Embedding dimension must be positive");
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimension];
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        for (String token : normalized.split("\\W+")) {
            if (token.isBlank()) continue;
            byte[] digest = sha256(token);
            int position = Math.floorMod(ByteBuffer.wrap(digest, 0, 4).getInt(), dimension);
            vector[position] += 1.0f;
        }
        double norm = 0;
        for (float value : vector) norm += value * value;
        if (norm > 0) {
            float divisor = (float) Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) vector[i] /= divisor;
        }
        return vector;
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override public int getDimension() { return dimension; }
    @Override public String getModelName() { return "resolveiq-deterministic-embedding-v1"; }
}
