package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.ExtractionResult;
import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.ObservationData;
import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.RedactionData;
import com.resolveiq.analysis.application.service.evidence.PdfExtractionPort;
import com.resolveiq.analysis.domain.model.evidence.ArtifactType;
import com.resolveiq.analysis.domain.model.evidence.ObservationType;
import com.resolveiq.analysis.domain.model.evidence.RedactionCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdftotextExtractionAdapter implements PdfExtractionPort {
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern INVOICE = Pattern.compile("(?:Invoice\\s*#?\\s*|INV-)([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private final String binary;

    public PdftotextExtractionAdapter(@Value("${resolveiq.evidence.tools.pdftotext:pdftotext}") String binary) {
        this.binary = binary;
    }

    @Override
    public ExtractionResult extractPdf(String fileName, byte[] content) {
        Path input = null;
        try {
            input = Files.createTempFile("resolveiq-pdf-", ".pdf");
            Files.write(input, content);
            Process process = new ProcessBuilder(binary, "-layout", "-nopgbrk", input.toString(), "-")
                .redirectErrorStream(true).start();
            if (!process.waitFor(Duration.ofSeconds(60).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("PDF extraction timed out after 60 seconds");
            }
            String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) throw new IllegalStateException("PDF extraction failed");
            return sanitize(text);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("PDF_TOOL_UNAVAILABLE: pdftotext could not process the document", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PDF extraction interrupted", error);
        } finally {
            if (input != null) try { Files.deleteIfExists(input); } catch (java.io.IOException ignored) { }
        }
    }

    private ExtractionResult sanitize(String raw) {
        List<RedactionData> redactions = new ArrayList<>();
        String redacted = redact(raw, CARD, "[REDACTED_CARD_NUMBER]", RedactionCategory.FINANCIAL, redactions);
        redacted = redact(redacted, SSN, "[REDACTED_SSN]", RedactionCategory.SECRET_TOKEN, redactions);
        redacted = redact(redacted, EMAIL, "[REDACTED_EMAIL]", RedactionCategory.PII_EMAIL, redactions);
        List<ObservationData> observations = new ArrayList<>();
        Matcher invoice = INVOICE.matcher(raw);
        if (invoice.find()) observations.add(new ObservationData(ObservationType.INVOICE_FACT,
            "INVOICE_ID:" + invoice.group(1), "Invoice reference extracted from PDF text", 1.0, "{\"page\":1}"));
        return new ExtractionResult(ArtifactType.PDF_REDACTED, redacted, observations, redactions, 1, null);
    }

    private String redact(String value, Pattern pattern, String replacement, RedactionCategory category,
                          List<RedactionData> records) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            records.add(new RedactionData(category, "pdf-text-range:" + matcher.start() + "-" + matcher.end(),
                "IRREVERSIBLE_TEXT_MASK"));
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
