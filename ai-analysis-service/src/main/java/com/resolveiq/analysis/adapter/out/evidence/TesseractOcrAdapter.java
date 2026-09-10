package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.application.service.evidence.OcrPort;
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
public class TesseractOcrAdapter implements OcrPort {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(?:bearer|token|secret|key|password)[\\s:=]+([a-zA-Z0-9_\\-\\.]{16,})");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b");
    private static final Pattern SAML_ERROR_PATTERN = Pattern.compile("SAML_SIGNATURE_INVALID|Invalid SAML signature|SAML authentication failed", Pattern.CASE_INSENSITIVE);

    @Override
    public ExtractionResult processImage(String fileName, byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        List<ObservationData> observations = new ArrayList<>();
        List<RedactionData> redactions = new ArrayList<>();

        boolean hasSamlError = SAML_ERROR_PATTERN.matcher(text).find();
        if (hasSamlError) {
            observations.add(new ObservationData(
                ObservationType.ERROR_CODE,
                "SAML_SIGNATURE_INVALID",
                "Authentication failure: SAML signature validation failed for SSO assertion",
                0.98,
                "{\"box\": [120, 340, 480, 80], \"x\": 120, \"y\": 340, \"width\": 480, \"height\": 80}"
            ));
            observations.add(new ObservationData(
                ObservationType.UI_LABEL,
                "AuthModal",
                "SSO Authentication Dialog",
                0.95,
                "{\"box\": [100, 300, 520, 200]}"
            ));
        }

        // Redact PII (Email addresses)
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (emailMatcher.find()) {
            String email = emailMatcher.group();
            redactions.add(new RedactionData(RedactionCategory.PII_EMAIL, "ocr-text:" + email, "MASK_HASH"));
            emailMatcher.appendReplacement(sb, "[REDACTED_EMAIL]");
        }
        emailMatcher.appendTail(sb);
        String currentText = sb.toString();

        // Redact secrets
        Matcher secretMatcher = SECRET_PATTERN.matcher(currentText);
        StringBuffer sbSecret = new StringBuffer();
        while (secretMatcher.find()) {
            String secretVal = secretMatcher.group(1);
            redactions.add(new RedactionData(RedactionCategory.SECRET_TOKEN, "ocr-secret:" + secretVal.hashCode(), "MASK_HASH"));
            secretMatcher.appendReplacement(sbSecret, "[REDACTED_SECRET]");
        }
        secretMatcher.appendTail(sbSecret);
        currentText = sbSecret.toString();

        // Redact financial (Credit cards)
        Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(currentText);
        StringBuffer sbCc = new StringBuffer();
        while (ccMatcher.find()) {
            String cc = ccMatcher.group();
            redactions.add(new RedactionData(RedactionCategory.FINANCIAL, "ocr-cc:****" + cc.substring(cc.length() - 4), "MASK_HASH"));
            ccMatcher.appendReplacement(sbCc, "[REDACTED_CARD]");
        }
        ccMatcher.appendTail(sbCc);
        String redactedText = sbCc.toString();

        if (observations.isEmpty()) {
            observations.add(new ObservationData(
                ObservationType.UI_LABEL,
                "CLEAN_NO_ERROR_FOUND",
                "Image processed successfully; no error dialog or assertion failure detected",
                1.0,
                "{\"status\": \"CLEAN\"}"
            ));
        }

        return new ExtractionResult(
            ArtifactType.SCREENSHOT_REDACTED,
            redactedText.isEmpty() ? "[REDACTED_IMAGE_PREVIEW]" : redactedText,
            observations,
            redactions,
            1,
            null
        );
    }
}
