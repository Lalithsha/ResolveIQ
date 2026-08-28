# ADR 0009: Model and Provider Abstraction

## Status
Accepted

## Context
ResolveIQ must avoid hard vendor lock-in to specific proprietary AI providers (e.g. OpenAI, Anthropic, Gemini) and support local development/offline environments using Ollama or mock test providers without modifying business orchestration logic.

## Decision
1. Define clean Java domain ports: `ChatClientPort`, `EmbeddingPort`, `StructuredClassifierPort`.
2. Implement provider-specific adapters (`OllamaChatAdapter`, `OpenAiChatAdapter`, `MockChatAdapter`) behind configuration switches.
3. Business services (`ai-analysis-service`, `ai-orchestration-service`, `rag-service`) interact solely through ports and immutable domain DTOs.
4. If a primary provider encounters a 5xx error or rate limit, the fallback strategy degrades gracefully to deterministic rule-based triage.

## Consequences
- **Positive:** Provider independence, testability without external API bills or API keys, deterministic local development.
- **Negative:** Provider-specific custom features must be normalized across common port interfaces.
- **Reversal Trigger:** None.
