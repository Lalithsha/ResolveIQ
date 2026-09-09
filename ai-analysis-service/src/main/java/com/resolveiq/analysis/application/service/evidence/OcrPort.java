package com.resolveiq.analysis.application.service.evidence;

public interface OcrPort {
    EvidenceExtractionModels.ExtractionResult processImage(String fileName, byte[] content);
}
