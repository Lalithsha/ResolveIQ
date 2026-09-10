package com.resolveiq.analysis.evidence;

import com.resolveiq.analysis.adapter.out.evidence.*;
import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.domain.model.evidence.ArtifactType;
import com.resolveiq.analysis.domain.model.evidence.ObservationType;
import com.resolveiq.analysis.domain.model.evidence.RedactionCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EvidencePipelineTest {

    @Test
    @DisplayName("OCR golden test extracts SAML_SIGNATURE_INVALID and bounding box [120, 340, 480, 80]")
    void ocrGoldenTestExtractsSamlSignatureInvalidAndBoundingBox() {
        TesseractOcrAdapter adapter = new TesseractOcrAdapter();
        String fixtureText = "Error 401: Invalid SAML signature detected for user alice@example.com. Code: SAML_SIGNATURE_INVALID.";
        byte[] content = fixtureText.getBytes(StandardCharsets.UTF_8);

        ExtractionResult result = adapter.processImage("saml_error_screenshot.png", content);

        assertThat(result.artifactType()).isEqualTo(ArtifactType.SCREENSHOT_REDACTED);
        // Redaction verification: email should be redacted
        assertThat(result.redactedContent()).contains("[REDACTED_EMAIL]");
        assertThat(result.redactedContent()).doesNotContain("alice@example.com");
        assertThat(result.redactions()).anyMatch(r -> r.category() == RedactionCategory.PII_EMAIL);

        // Golden verification: error code SAML_SIGNATURE_INVALID and bounding box
        ObservationData samlObs = result.observations().stream()
            .filter(o -> o.codeOrKey().equals("SAML_SIGNATURE_INVALID"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected SAML_SIGNATURE_INVALID observation"));

        assertThat(samlObs.type()).isEqualTo(ObservationType.ERROR_CODE);
        assertThat(samlObs.sourceCoordinates()).contains("[120, 340, 480, 80]");
        assertThat(samlObs.confidence()).isGreaterThanOrEqualTo(0.95);
    }

    @Test
    @DisplayName("Log golden test groups stack trace into fingerprint and redacts secret tokens")
    void logGoldenTestGroupsStackTraceAndRedactsSecrets() {
        DeterministicLogAnalysisAdapter adapter = new DeterministicLogAnalysisAdapter();
        String logFixture = """
            2026-09-10 12:00:00 [ERROR] Request failed with Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test
            java.lang.NullPointerException: SSO session expired
                at com.resolveiq.auth.handler(SsoHandler.java:42)
                at com.resolveiq.auth.filter(AuthFilter.java:100)
            2026-09-10 12:00:01 [DEBUG] connecting with password: superSecretPassword123
            """;

        ExtractionResult result = adapter.processLog("application.log", logFixture.getBytes(StandardCharsets.UTF_8));

        assertThat(result.artifactType()).isEqualTo(ArtifactType.LOG_REDACTED);
        // Redaction verification: Bearer token and password redacted
        assertThat(result.redactedContent()).doesNotContain("eyJhbGciOiJIUzI1NiJ9.test");
        assertThat(result.redactedContent()).doesNotContain("superSecretPassword123");
        assertThat(result.redactedContent()).contains("Bearer [REDACTED_SECRET]");
        assertThat(result.redactedContent()).contains("password=[REDACTED_SECRET]");

        // Observation verification: Stack fingerprint
        assertThat(result.observations()).anyMatch(o ->
            o.type() == ObservationType.STACK_FINGERPRINT &&
            o.codeOrKey().contains("NullPointerException") &&
            o.sourceCoordinates().contains("SsoHandler.java:42")
        );
    }

    @Test
    @DisplayName("CSV golden test neutralizes formula injection characters (=, +, -, @)")
    void csvGoldenTestNeutralizesFormulaInjection() {
        DeterministicCsvSanitizationAdapter adapter = new DeterministicCsvSanitizationAdapter();
        String csvFixture = """
            id,username,command,balance
            1,alice,=cmd|' /C calc'!A0,100
            2,bob,+SUM(A1:A10),200
            3,charlie,-200,300
            4,dave,@IMPORTXML("http://evil.com"),400
            """;

        ExtractionResult result = adapter.sanitizeCsv("users.csv", csvFixture.getBytes(StandardCharsets.UTF_8));

        assertThat(result.artifactType()).isEqualTo(ArtifactType.CSV_SANITIZED);
        assertThat(result.redactedContent()).contains("'=cmd|' /C calc'!A0");
        assertThat(result.redactedContent()).contains("'+SUM(A1:A10)");
        assertThat(result.redactedContent()).contains("'-200");
        assertThat(result.redactedContent()).contains("'@IMPORTXML(\"http://evil.com\")");
        assertThat(result.redactions()).isNotEmpty();
    }

    @Test
    @DisplayName("PDF golden test extracts invoice facts and redacts financial PII")
    void pdfGoldenTestExtractsFactsAndRedactsCreditCards() {
        DeterministicPdfExtractionAdapter adapter = new DeterministicPdfExtractionAdapter();
        String pdfFixture = """
            INVOICE #INV-98765
            Bill to: John Doe (john.doe@example.com)
            Card: 4111-2222-3333-4444
            SSN: 123-45-6789
            Amount: $450.00
            """;

        ExtractionResult result = adapter.extractPdf("invoice.pdf", pdfFixture.getBytes(StandardCharsets.UTF_8));

        assertThat(result.artifactType()).isEqualTo(ArtifactType.PDF_REDACTED);
        assertThat(result.redactedContent()).doesNotContain("4111-2222-3333-4444");
        assertThat(result.redactedContent()).doesNotContain("123-45-6789");
        assertThat(result.redactedContent()).contains("[REDACTED_CARD_NUMBER]");
        assertThat(result.redactedContent()).contains("[REDACTED_SSN]");

        assertThat(result.observations()).anyMatch(o ->
            o.type() == ObservationType.INVOICE_FACT && o.codeOrKey().contains("98765")
        );
    }

    @Test
    @DisplayName("Video golden test identifies failure chapter at exact timestamp 00:42 (42.0 seconds)")
    void videoGoldenTestIdentifiesFailureTimestampAt42Seconds() {
        DeterministicVideoAnalysisAdapter adapter = new DeterministicVideoAnalysisAdapter();
        byte[] videoDummyBytes = ("\0\0\0\u0018ftyp" + " SAML assertion parse failure at 00:42").getBytes(StandardCharsets.ISO_8859_1);

        ExtractionResult result = adapter.sampleVideo("screen_recording.mp4", videoDummyBytes);

        assertThat(result.artifactType()).isEqualTo(ArtifactType.VIDEO_FRAME);
        assertThat(result.timestampSeconds()).isEqualTo(42.0);
        assertThat(result.pageOrFrame()).isEqualTo(42);

        ObservationData failureObs = result.observations().stream()
            .filter(o -> o.type() == ObservationType.FAILURE_TIMESTAMP)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected FAILURE_TIMESTAMP observation"));

        assertThat(failureObs.codeOrKey()).isEqualTo("00:42");
        assertThat(failureObs.sourceCoordinates()).contains("\"timestamp\": 42.0");
        assertThat(failureObs.sourceCoordinates()).contains("\"chapter\": \"SAML assertion parse failure\"");
    }

    @Test
    @DisplayName("C4 Acceptance: Filename independence - failure found in clean.mp4, clean video has no error even in saml_timestamp_42.mp4")
    void filenameIndependenceTest() {
        DeterministicVideoAnalysisAdapter adapter = new DeterministicVideoAnalysisAdapter();

        // 1. Failure recording renamed to clean.mp4 -> failure must still be found!
        byte[] failureBytes = ("\0\0\0\u0018ftyp" + " SAML assertion parse failure at 01:15").getBytes(StandardCharsets.ISO_8859_1);
        ExtractionResult failResult = adapter.sampleVideo("clean.mp4", failureBytes);
        assertThat(failResult.timestampSeconds()).isEqualTo(75.0);
        assertThat(failResult.observations()).anyMatch(o -> o.type() == ObservationType.FAILURE_TIMESTAMP && o.codeOrKey().equals("01:15"));

        // 2. Clean recording renamed to saml_timestamp_42.mp4 -> NO error may be invented!
        byte[] cleanBytes = new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 0x01, 0x02, 0x03};
        ExtractionResult cleanResult = adapter.sampleVideo("saml_timestamp_42.mp4", cleanBytes);
        assertThat(cleanResult.timestampSeconds()).isNull();
        assertThat(cleanResult.observations()).noneMatch(o -> o.type() == ObservationType.FAILURE_TIMESTAMP);
        assertThat(cleanResult.observations()).anyMatch(o -> o.codeOrKey().equals("CLEAN_PLAYBACK"));
    }
}
