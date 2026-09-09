package com.resolveiq.analysis.application.service.evidence;

public interface CsvSanitizationPort {
    EvidenceExtractionModels.ExtractionResult sanitizeCsv(String fileName, byte[] content);
}
