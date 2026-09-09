package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.application.service.evidence.LogAnalysisPort;
import com.resolveiq.analysis.domain.model.evidence.ArtifactType;
import com.resolveiq.analysis.domain.model.evidence.ObservationType;
import com.resolveiq.analysis.domain.model.evidence.RedactionCategory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicLogAnalysisAdapter implements LogAnalysisPort {

    private static final Pattern BEARER_TOKEN = Pattern.compile("Bearer\\s+[a-zA-Z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY = Pattern.compile("(sk-live-[a-zA-Z0-9]+|api[_-]?key\\s*[:=]\\s*[a-zA-Z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD = Pattern.compile("(password\\s*[:=]\\s*\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("([a-zA-Z0-9_\\.]+(?:Exception|Error):.*?at\\s+[a-zA-Z0-9_\\.\\$]+\\([a-zA-Z0-9_]+\\.java:\\d+\\))", Pattern.DOTALL);

    @Override
    public ExtractionResult processLog(String fileName, byte[] content) {
        String raw = new String(content, StandardCharsets.UTF_8);
        List<ObservationData> observations = new ArrayList<>();
        List<RedactionData> redactions = new ArrayList<>();

        // Group stack traces
        Matcher stackMatcher = STACK_TRACE_PATTERN.matcher(raw);
        while (stackMatcher.find()) {
            String stack = stackMatcher.group(1).trim();
            // Extract top frame
            String topFrame = stack.lines().filter(l -> l.contains("at ")).findFirst().orElse(stack);
            String errorType = stack.lines().findFirst().orElse("UnknownException").split(":")[0].trim();
            observations.add(new ObservationData(
                ObservationType.STACK_FINGERPRINT,
                errorType + "@" + topFrame.trim(),
                "Grouped stack trace: " + errorType,
                0.99,
                "{\"topFrame\": \"" + topFrame.trim().replace("\"", "'") + "\"}"
            ));
        }

        // If no full stack trace was parsed with regex, check for common exception patterns
        if (observations.isEmpty() && raw.contains("Exception")) {
            for (String line : raw.split("\n")) {
                if (line.contains("Exception") || line.contains("Error")) {
                    observations.add(new ObservationData(
                        ObservationType.STACK_FINGERPRINT,
                        line.trim(),
                        "Grouped exception: " + line.trim(),
                        0.92,
                        "{\"line\": \"" + line.trim().replace("\"", "'") + "\"}"
                    ));
                    break;
                }
            }
        }

        // Redact Bearer tokens
        String sanitized = raw;
        Matcher bearerMatcher = BEARER_TOKEN.matcher(sanitized);
        if (bearerMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.SECRET_TOKEN, "auth-header", "MASK_HASH"));
            sanitized = bearerMatcher.replaceAll("Bearer [REDACTED_SECRET]");
        }

        // Redact API keys
        Matcher keyMatcher = API_KEY.matcher(sanitized);
        if (keyMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.SECRET_TOKEN, "credentials", "MASK_HASH"));
            sanitized = keyMatcher.replaceAll("api_key=[REDACTED_SECRET]");
        }

        // Redact passwords
        Matcher pwdMatcher = PASSWORD.matcher(sanitized);
        if (pwdMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.SECRET_TOKEN, "password-field", "MASK_HASH"));
            sanitized = pwdMatcher.replaceAll("password=[REDACTED_SECRET]");
        }

        // Redact emails
        Matcher emailMatcher = EMAIL.matcher(sanitized);
        if (emailMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.PII_EMAIL, "log-body", "MASK_HASH"));
            sanitized = emailMatcher.replaceAll("[REDACTED_EMAIL]");
        }

        return new ExtractionResult(
            ArtifactType.LOG_REDACTED,
            sanitized,
            observations,
            redactions,
            null,
            null
        );
    }
}
