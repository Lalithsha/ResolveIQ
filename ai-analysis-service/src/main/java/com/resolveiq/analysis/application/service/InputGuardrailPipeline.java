package com.resolveiq.analysis.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class InputGuardrailPipeline {
    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern PAYMENT = Pattern.compile("(?<!\\d)(?:\\d[ -]*?){13,19}(?!\\d)");
    private static final Pattern JWT = Pattern.compile("(?i)(?:bearer\\s+)?eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");
    private static final Pattern AWS_KEY = Pattern.compile("\\b(?:AKIA|ASIA)[A-Z0-9]{16}\\b");
    private static final Pattern API_SECRET = Pattern.compile("(?i)\\b(api[_ -]?key|secret|password)\\s*[:=]\\s*['\"]?[A-Za-z0-9_./+\\-=]{8,}");
    private static final List<String> INJECTION_MARKERS = List.of(
        "ignore previous", "ignore all instructions", "system prompt", "developer message",
        "<|system|>", "<|assistant|>", "system_override", "jailbreak", "do anything now"
    );

    private final ObjectMapper objectMapper;
    private final int maxCharacters;
    private final int maxInputTokens;

    public InputGuardrailPipeline(
        ObjectMapper objectMapper,
        @Value("${resolveiq.ai.guardrails.max-characters:12000}") int maxCharacters,
        @Value("${resolveiq.ai.budget.max-input-tokens:4000}") int maxInputTokens
    ) {
        this.objectMapper = objectMapper;
        this.maxCharacters = maxCharacters;
        this.maxInputTokens = maxInputTokens;
    }

    public InputGuardrailResult inspect(String subject, String description) {
        String cleanSubject = normalize(subject);
        String cleanDescription = normalize(description);
        List<Map<String, String>> findings = new ArrayList<>();
        String combined = cleanSubject + "\n" + cleanDescription;
        int estimatedTokens = Math.max(1, (combined.length() + 3) / 4);
        boolean blocked = combined.length() > maxCharacters || estimatedTokens > maxInputTokens;
        if (combined.length() > maxCharacters) add(findings, "INPUT_TOO_LARGE", "BLOCK", "Ticket content exceeds the character budget");
        if (estimatedTokens > maxInputTokens) add(findings, "TOKEN_BUDGET_EXCEEDED", "BLOCK", "Estimated input tokens exceed the configured budget");

        String lowered = combined.toLowerCase(Locale.ROOT);
        for (String marker : INJECTION_MARKERS) {
            if (lowered.contains(marker)) {
                add(findings, "PROMPT_INJECTION_PATTERN", "WARN", "Untrusted instruction-like text was neutralized");
                cleanSubject = neutralize(cleanSubject);
                cleanDescription = neutralize(cleanDescription);
                break;
            }
        }

        Redaction subjectRedaction = redact(cleanSubject);
        Redaction descriptionRedaction = redact(cleanDescription);
        cleanSubject = subjectRedaction.value();
        cleanDescription = descriptionRedaction.value();
        subjectRedaction.codes().forEach(code -> add(findings, code, "REDACT", "Sensitive input was redacted before provider invocation"));
        descriptionRedaction.codes().forEach(code -> {
            if (findings.stream().noneMatch(item -> code.equals(item.get("code")))) add(findings, code, "REDACT", "Sensitive input was redacted before provider invocation");
        });

        String outcome = blocked ? "BLOCKED" : findings.isEmpty() ? "PASSED" : "SANITIZED";
        return new InputGuardrailResult(cleanSubject, cleanDescription, outcome, toJson(findings), blocked, estimatedTokens);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", " ")
            .replace("```", "")
            .trim();
    }

    private String neutralize(String value) {
        String result = value;
        for (String marker : INJECTION_MARKERS) result = result.replaceAll("(?i)" + Pattern.quote(marker), "[UNTRUSTED_INSTRUCTION]");
        return result;
    }

    private Redaction redact(String value) {
        List<String> codes = new ArrayList<>();
        String redacted = replace(value, EMAIL, "[REDACTED_EMAIL]", "EMAIL", codes);
        redacted = replace(redacted, PAYMENT, "[REDACTED_PAYMENT_NUMBER]", "PAYMENT_NUMBER", codes);
        redacted = replace(redacted, JWT, "[REDACTED_TOKEN]", "AUTH_TOKEN", codes);
        redacted = replace(redacted, AWS_KEY, "[REDACTED_CLOUD_KEY]", "CLOUD_KEY", codes);
        redacted = replace(redacted, API_SECRET, "[REDACTED_SECRET]", "SECRET", codes);
        return new Redaction(redacted, codes.stream().distinct().toList());
    }

    private String replace(String value, Pattern pattern, String replacement, String code, List<String> codes) {
        if (pattern.matcher(value).find()) codes.add(code);
        return pattern.matcher(value).replaceAll(replacement);
    }

    private void add(List<Map<String, String>> findings, String code, String action, String message) {
        Map<String, String> finding = new LinkedHashMap<>();
        finding.put("code", code); finding.put("action", action); finding.put("message", message);
        findings.add(finding);
    }

    private String toJson(List<Map<String, String>> findings) {
        try { return objectMapper.writeValueAsString(findings); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to serialize guardrail findings", exception); }
    }

    private record Redaction(String value, List<String> codes) {}
}
