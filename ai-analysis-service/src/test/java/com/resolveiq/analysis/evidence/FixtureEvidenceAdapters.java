package com.resolveiq.analysis.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.ExtractionResult;
import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.ObservationData;
import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.RedactionData;
import com.resolveiq.analysis.application.service.evidence.OcrPort;
import com.resolveiq.analysis.application.service.evidence.PdfExtractionPort;
import com.resolveiq.analysis.application.service.evidence.VideoFrameSamplingPort;
import com.resolveiq.analysis.domain.model.evidence.ArtifactType;
import com.resolveiq.analysis.domain.model.evidence.ObservationType;
import com.resolveiq.analysis.domain.model.evidence.RedactionCategory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicit unit-test doubles. These adapters are not Spring components and cannot be selected in production. */
final class FixtureEvidenceAdapters {
    private FixtureEvidenceAdapters() {}

    static final class Ocr implements OcrPort {
        private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

        @Override
        public ExtractionResult processImage(String fileName, byte[] content) {
            String raw = new String(content, StandardCharsets.UTF_8);
            Matcher email = EMAIL.matcher(raw);
            List<RedactionData> redactions = new ArrayList<>();
            while (email.find()) {
                redactions.add(new RedactionData(RedactionCategory.PII_EMAIL,
                    "fixture-range:" + email.start() + "-" + email.end(), "IRREVERSIBLE_TEXT_MASK"));
            }
            List<ObservationData> observations = raw.contains("SAML_SIGNATURE_INVALID")
                ? List.of(new ObservationData(ObservationType.ERROR_CODE, "SAML_SIGNATURE_INVALID",
                    "Fixture OCR result", 0.97, "{\"box\":[120, 340, 480, 80]}"))
                : List.of();
            return new ExtractionResult(ArtifactType.SCREENSHOT_REDACTED,
                email.replaceAll("[REDACTED_EMAIL]"), observations, redactions, 1, null);
        }
    }

    static final class Video implements VideoFrameSamplingPort {
        private static final Pattern TIMESTAMP = Pattern.compile("(\\d{2}):(\\d{2})");

        @Override
        public ExtractionResult sampleVideo(String fileName, byte[] content) {
            String fixture = new String(content, StandardCharsets.ISO_8859_1);
            Matcher timestamp = TIMESTAMP.matcher(fixture);
            if (!fixture.contains("failure") || !timestamp.find()) {
                return new ExtractionResult(ArtifactType.VIDEO_FRAME, "[FIXTURE_CLEAN]",
                    List.of(new ObservationData(ObservationType.ERROR_CODE, "CLEAN_PLAYBACK",
                        "Fixture contains no failure marker", 1.0, "{}")), List.of(), null, null);
            }
            int seconds = Integer.parseInt(timestamp.group(1)) * 60 + Integer.parseInt(timestamp.group(2));
            String label = timestamp.group();
            return new ExtractionResult(ArtifactType.VIDEO_FRAME, "[FIXTURE_FAILURE_FRAME]",
                List.of(new ObservationData(ObservationType.FAILURE_TIMESTAMP, label,
                    "SAML assertion parse failure", 1.0,
                    "{\"timestamp\": " + seconds + ".0, \"chapter\": \"SAML assertion parse failure\"}")),
                List.of(), seconds, (double) seconds);
        }
    }

    static final class Pdf implements PdfExtractionPort {
        @Override
        public ExtractionResult extractPdf(String fileName, byte[] content) {
            String raw = new String(content, StandardCharsets.UTF_8);
            String redacted = raw.replace("4111-2222-3333-4444", "[REDACTED_CARD_NUMBER]")
                .replace("123-45-6789", "[REDACTED_SSN]")
                .replace("john.doe@example.com", "[REDACTED_EMAIL]");
            return new ExtractionResult(ArtifactType.PDF_REDACTED, redacted,
                List.of(new ObservationData(ObservationType.INVOICE_FACT, "INVOICE_ID:98765",
                    "Fixture invoice", 1.0, "{\"page\":1}")),
                List.of(new RedactionData(RedactionCategory.FINANCIAL, "fixture-card", "MASK")), 1, null);
        }
    }
}
