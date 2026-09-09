package com.resolveiq.analysis.application.service.evidence;

public interface VideoFrameSamplingPort {
    EvidenceExtractionModels.ExtractionResult sampleVideo(String fileName, byte[] content);
}
