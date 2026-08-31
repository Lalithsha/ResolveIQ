package com.resolveiq.rag.adapter.out.ai;

import com.resolveiq.rag.application.port.EmbeddingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "resolveiq.ai.embedding-provider", havingValue = "mock")
public class MockEmbeddingAdapter implements EmbeddingPort {

    private final String modelName;
    private final int dimension;

    public MockEmbeddingAdapter(
        @Value("${resolveiq.ai.embedding-model:mock-embedding-v1}") String modelName,
        @Value("${resolveiq.ai.embedding-dimension:1536}") int dimension
    ) {
        this.modelName = modelName;
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }

        // Deterministic pseudo-embedding from text hash seed
        long seed = hashSeed(text);
        Random random = new Random(seed);

        double norm = 0.0;
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) (random.nextGaussian());
            norm += vector[i] * vector[i];
        }

        // L2 normalize
        float invNorm = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < dimension; i++) {
            vector[i] *= invNorm;
        }

        return vector;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    private long hashSeed(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (bytes[i] & 0xff);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            return text.hashCode();
        }
    }
}
