package com.resolveiq.analysis.application.service.evidence;

import com.resolveiq.analysis.domain.model.evidence.ArtifactType;
import com.resolveiq.analysis.domain.model.evidence.ObservationType;
import com.resolveiq.analysis.domain.model.evidence.RedactionCategory;

import java.util.List;

public class EvidenceExtractionModels {

    public record ObservationData(
        ObservationType type,
        String codeOrKey,
        String summary,
        double confidence,
        String sourceCoordinates
    ) {}

    public record RedactionData(
        RedactionCategory category,
        String location,
        String maskMethod
    ) {}

    public record ExtractionResult(
        ArtifactType artifactType,
        String redactedContent,
        List<ObservationData> observations,
        List<RedactionData> redactions,
        Integer pageOrFrame,
        Double timestampSeconds
    ) {}
}
