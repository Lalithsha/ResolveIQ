package com.resolveiq.analysis.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.analysis.application.dto.TicketAnalysisRequest;
import com.resolveiq.analysis.application.dto.TicketAnalysisResponse;
import com.resolveiq.analysis.application.port.ChatClientPort;
import com.resolveiq.analysis.domain.model.AnalysisResult;
import com.resolveiq.analysis.domain.repository.AnalysisResultRepository;
import com.resolveiq.analysis.domain.repository.PromptVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
public class TicketAnalysisService {

    private final AnalysisResultRepository analysisRepository;
    private final PromptVersionRepository promptRepository;
    private final ChatClientPort chatClient;
    private final ObjectMapper objectMapper;

    public TicketAnalysisService(
        AnalysisResultRepository analysisRepository,
        PromptVersionRepository promptRepository,
        ChatClientPort chatClient,
        ObjectMapper objectMapper
    ) {
        this.analysisRepository = analysisRepository;
        this.promptRepository = promptRepository;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TicketAnalysisResponse analyzeTicket(TicketAnalysisRequest request) {
        long startTime = System.currentTimeMillis();

        String promptVersion = "triage-v1.0";
        String systemPrompt = """
            You are ResolveIQ AI Support Classifier.
            Analyze the support ticket and output valid JSON with keys:
            intent, category, sentiment, sentimentConfidence, urgency, urgencyConfidence, language, redactedEntities, policyFlags.
            """;

        // Prompt injection defense: sanitize delimiters
        String sanitizedSubject = sanitizeInput(request.subject());
        String sanitizedDesc = sanitizeInput(request.description());
        String userPrompt = String.format("Subject: %s\nDescription: %s", sanitizedSubject, sanitizedDesc);

        String rawOutput = chatClient.generateResponse(systemPrompt, userPrompt);
        long latencyMs = System.currentTimeMillis() - startTime;

        String intent = "general_inquiry";
        String category = "TECHNICAL";
        String sentiment = "NEUTRAL";
        double sentimentConf = 0.8;
        String urgency = "MEDIUM";
        double urgencyConf = 0.8;
        String language = "en";
        String redactedEntities = "{}";
        String policyFlags = "{}";

        try {
            JsonNode node = objectMapper.readTree(rawOutput);
            if (node.has("intent")) intent = node.get("intent").asText();
            if (node.has("category")) category = node.get("category").asText();
            if (node.has("sentiment")) sentiment = node.get("sentiment").asText();
            if (node.has("sentimentConfidence")) sentimentConf = node.get("sentimentConfidence").asDouble();
            if (node.has("urgency")) urgency = node.get("urgency").asText();
            if (node.has("urgencyConfidence")) urgencyConf = node.get("urgencyConfidence").asDouble();
            if (node.has("language")) language = node.get("language").asText();
            if (node.has("redactedEntities")) redactedEntities = node.get("redactedEntities").toString();
            if (node.has("policyFlags")) policyFlags = node.get("policyFlags").toString();
        } catch (Exception ignored) {
            // Safe fallback
        }

        String rawHash = computeHash(rawOutput);

        AnalysisResult result = new AnalysisResult(
            request.ticketId(),
            request.tenantId() != null ? request.tenantId() : UUID.fromString("00000000-0000-0000-0000-000000000001"),
            intent,
            category,
            sentiment,
            sentimentConf,
            urgency,
            urgencyConf,
            language,
            redactedEntities,
            policyFlags,
            chatClient.getModelName(),
            promptVersion,
            rawHash,
            "VALID",
            latencyMs,
            rawOutput.length() / 4
        );
        analysisRepository.save(result);

        return new TicketAnalysisResponse(
            result.getId(),
            result.getTicketId(),
            result.getTenantId(),
            result.getIntent(),
            result.getCategory(),
            result.getSentiment(),
            result.getSentimentConfidence(),
            result.getUrgency(),
            result.getUrgencyConfidence(),
            result.getLanguage(),
            result.getModelName(),
            result.getPromptVersion(),
            result.getValidationOutcome(),
            result.getLatencyMs(),
            result.getTokensUsed()
        );
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Strip common prompt injection tokens and system override tags
        return input.replace("```", "")
                    .replace("<|system|>", "")
                    .replace("<|assistant|>", "")
                    .replace("SYSTEM_OVERRIDE", "")
                    .trim();
    }

    private String computeHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }
}
