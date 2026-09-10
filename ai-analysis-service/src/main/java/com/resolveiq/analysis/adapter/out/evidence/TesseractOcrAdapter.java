package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.application.service.evidence.OcrPort;
import com.resolveiq.analysis.domain.model.evidence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

@Component
public class TesseractOcrAdapter implements OcrPort {
    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SECRET = Pattern.compile("(?i)(?:bearer|token|secret|key|password)[\\s:=]+([a-zA-Z0-9_\\-\\.]{12,})");
    private static final Pattern CARD = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b");
    private static final Pattern SAML = Pattern.compile("SAML_SIGNATURE_INVALID|Invalid SAML signature|SAML authentication failed", Pattern.CASE_INSENSITIVE);
    private final String binary;

    public TesseractOcrAdapter(@Value("${resolveiq.evidence.tools.tesseract:tesseract}") String binary) { this.binary = binary; }

    @Override public ExtractionResult processImage(String fileName, byte[] content) {
        Path input = null;
        try {
            input = Files.createTempFile("resolveiq-ocr-", fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".jpg") ? ".jpg" : ".png");
            Files.write(input, content, StandardOpenOption.TRUNCATE_EXISTING);
            Process process = new ProcessBuilder(binary, input.toString(), "stdout", "tsv").redirectErrorStream(true).start();
            if (!process.waitFor(Duration.ofSeconds(120).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly(); throw new IllegalStateException("OCR timed out after 120 seconds");
            }
            String tsv = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) throw new IllegalStateException("OCR failed: " + (tsv.length() > 500 ? tsv.substring(0, 500) : tsv));
            return fromTsv(tsv);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("OCR_UNAVAILABLE: tesseract could not process the image", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("OCR interrupted", error);
        } finally { if (input != null) try { Files.deleteIfExists(input); } catch (java.io.IOException ignored) { } }
    }

    private ExtractionResult fromTsv(String tsv) {
        StringBuilder text = new StringBuilder(); int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=0,maxY=0,count=0; double confidenceSum=0;
        for (String line : tsv.split("\\R")) {
            String[] c=line.split("\\t",-1); if(c.length<12 || "text".equals(c[11]) || c[11].isBlank()) continue;
            text.append(c[11]).append(' ');
            try { int x=Integer.parseInt(c[6]),y=Integer.parseInt(c[7]),w=Integer.parseInt(c[8]),h=Integer.parseInt(c[9]); double conf=Double.parseDouble(c[10]);
                minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x+w);maxY=Math.max(maxY,y+h);if(conf>=0){confidenceSum+=conf;count++;} } catch(NumberFormatException ignored) { }
        }
        String raw=text.toString().trim(); List<RedactionData> redactions=new ArrayList<>();
        String redacted=redact(raw,EMAIL,"[REDACTED_EMAIL]",RedactionCategory.PII_EMAIL,redactions);
        redacted=redact(redacted,SECRET,"[REDACTED_SECRET]",RedactionCategory.SECRET_TOKEN,redactions);
        redacted=redact(redacted,CARD,"[REDACTED_CARD]",RedactionCategory.FINANCIAL,redactions);
        List<ObservationData> observations=new ArrayList<>();
        if(SAML.matcher(raw).find()) { double confidence=count==0?0:Math.min(1,confidenceSum/count/100);
            String coordinates=minX==Integer.MAX_VALUE?"{}":String.format(Locale.ROOT,"{\"box\":[%d,%d,%d,%d]}",minX,minY,maxX-minX,maxY-minY);
            observations.add(new ObservationData(ObservationType.ERROR_CODE,"SAML_SIGNATURE_INVALID","OCR detected a SAML signature validation error",confidence,coordinates)); }
        return new ExtractionResult(ArtifactType.SCREENSHOT_REDACTED,redacted,observations,redactions,1,null);
    }

    private String redact(String value, Pattern pattern, String replacement, RedactionCategory category, List<RedactionData> records) {
        Matcher matcher=pattern.matcher(value);StringBuffer output=new StringBuffer();while(matcher.find()){records.add(new RedactionData(category,"ocr-range:"+matcher.start()+"-"+matcher.end(),"IRREVERSIBLE_TEXT_MASK"));matcher.appendReplacement(output,replacement);}matcher.appendTail(output);return output.toString();
    }
}
