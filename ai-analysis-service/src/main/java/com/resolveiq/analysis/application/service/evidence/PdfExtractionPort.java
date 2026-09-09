package com.resolveiq.analysis.application.service.evidence;

public interface PdfExtractionPort {
    EvidenceExtractionModels.ExtractionResult extractPdf(String fileName, byte[] content);
}
