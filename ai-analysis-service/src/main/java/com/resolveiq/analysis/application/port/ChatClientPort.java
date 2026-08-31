package com.resolveiq.analysis.application.port;

public interface ChatClientPort {
    ChatProviderResponse generateResponse(String systemPrompt, String userPrompt);
    String getModelName();
}
