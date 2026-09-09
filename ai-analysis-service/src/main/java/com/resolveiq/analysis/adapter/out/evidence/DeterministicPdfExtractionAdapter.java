package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.application.service.evidence.PdfExtractionPort;
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
public class DeterministicPdfExtractionAdapter implements PdfExtractionPort {

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?:Invoice\\s*#?\\s*|INV-)([A-Z0-9\\-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public ExtractionResult extractPdf(String fileName, byte[] content) {
        String raw = new String(content, StandardCharsets.UTF_8);
        List<ObservationData> observations = new ArrayList<>();
        List<RedactionData> redactions = new ArrayList<>();

        // Extract invoice facts
        Matcher invMatcher = INVOICE_NUMBER_PATTERN.matcher(raw);
        if (invMatcher.find()) {
            String invNum = invMatcher.group(1);
            observations.add(new ObservationData(
                ObservationType.INVOICE_FACT,
                "INVOICE_ID:" + invNum,
                "Extracted invoice reference: " + invNum,
                0.98,
                "{\"page\": 1}"
            ));
        }

        // Redact Credit Cards
        String sanitized = raw;
        Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(sanitized);
        if (ccMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.FINANCIAL, "pdf-page-1:payment", "MASK_HASH"));
            sanitized = ccMatcher.replaceAll("[REDACTED_CARD_NUMBER]");
        }

        // Redact SSNs
        Matcher ssnMatcher = SSN_PATTERN.matcher(sanitized);
        if (ssnMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.SECRET_TOKEN, "pdf-page-1:id", "MASK_HASH"));
            sanitized = ssnMatcher.replaceAll("[REDACTED_SSN]");
        }

        // Redact Emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(sanitized);
        if (emailMatcher.find()) {
            redactions.add(new RedactionData(RedactionCategory.PII_EMAIL, "pdf-page-1:contact", "MASK_HASH"));
            sanitized = emailMatcher.replaceAll("[REDACTED_EMAIL]");
        }

        if (observations.isEmpty()) {
            observations.add(new ObservationData(
                ObservationType.INVOICE_FACT,
                "PDF_DOCUMENT",
                "Processed PDF document text",
                0.95,
                "{\"page\": 1}"
            ));
        }

        return new ExtractionResult(
            ArtifactType.PDF_REDACTED,
            sanitized,
            observations,
            redactions,
            1,
            null
        );
    }
}
