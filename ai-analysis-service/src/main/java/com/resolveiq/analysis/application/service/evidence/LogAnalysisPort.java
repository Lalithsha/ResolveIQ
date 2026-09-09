package com.resolveiq.analysis.application.service.evidence;

public interface LogAnalysisPort {
    EvidenceExtractionModels.ExtractionResult processLog(String fileName, byte[] content);
}
