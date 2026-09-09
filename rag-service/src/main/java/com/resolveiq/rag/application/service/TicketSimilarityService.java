package com.resolveiq.rag.application.service;

import com.resolveiq.rag.application.dto.TicketSimilarityRequest;
import com.resolveiq.rag.application.dto.TicketSimilarityResponse;
import com.resolveiq.rag.application.port.EmbeddingPort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TicketSimilarityService {

    private final EmbeddingPort embeddingPort;

    public TicketSimilarityService(EmbeddingPort embeddingPort) {
        this.embeddingPort = embeddingPort;
    }

    public TicketSimilarityResponse computeSimilarity(UUID tenantId, TicketSimilarityRequest request) {
        if (request.candidates() == null || request.candidates().isEmpty()) {
            return new TicketSimilarityResponse(request.ticketId(), List.of());
        }

        float[] queryVector = embeddingPort.embed(request.queryText());
        double minSimilarity = request.minSimilarity() != null ? request.minSimilarity() : 0.70;

        List<TicketSimilarityResponse.SimilarityMatch> matches = new ArrayList<>();

        for (TicketSimilarityRequest.CandidateTicket candidate : request.candidates()) {
            if (request.ticketId() != null && request.ticketId().equals(candidate.ticketId())) {
                continue;
            }

            float[] candidateVector = embeddingPort.embed(candidate.text() != null ? candidate.text() : "");
            double cosine = cosineSimilarity(queryVector, candidateVector);

            boolean fingerprintMatched = request.errorFingerprint() != null
                && request.errorFingerprint().equalsIgnoreCase(candidate.errorFingerprint());

            boolean categoryMatched = request.category() != null
                && request.category().equalsIgnoreCase(candidate.category());

            boolean productMatched = request.product() != null
                && request.product().equalsIgnoreCase(candidate.product());

            double score = cosine;

            // Apply fingerprint boost
            if (fingerprintMatched) {
                score = Math.min(1.0, score + 0.15);
            }

            // Incompatibility penalty if both products are defined and distinctly incompatible
            if (request.product() != null && candidate.product() != null && !productMatched) {
                score = Math.max(0.0, score - 0.25);
            }

            if (score >= minSimilarity) {
                String explanation = String.format(
                    "Semantic similarity: %.2f%s%s",
                    cosine,
                    fingerprintMatched ? ", error fingerprint matched" : "",
                    categoryMatched ? ", category matched" : ""
                );
                matches.add(new TicketSimilarityResponse.SimilarityMatch(
                    candidate.ticketId(),
                    Math.round(score * 1000.0) / 1000.0,
                    fingerprintMatched,
                    categoryMatched,
                    explanation
                ));
            }
        }

        matches.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));

        return new TicketSimilarityResponse(request.ticketId(), matches);
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length || v1.length == 0) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        if (normA <= 0.0 || normB <= 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
