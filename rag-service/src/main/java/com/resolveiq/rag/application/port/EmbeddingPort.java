package com.resolveiq.rag.application.port;

public interface EmbeddingPort {
    float[] embed(String text);
    int getDimension();
    String getModelName();
}
