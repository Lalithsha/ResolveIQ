package com.resolveiq.analysis.application.port;

public interface ChatClientPort {
    String generateResponse(String systemPrompt, String userPrompt);
    String getModelName();
}
