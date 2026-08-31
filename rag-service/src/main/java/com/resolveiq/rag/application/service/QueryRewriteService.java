package com.resolveiq.rag.application.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic, explainable query normalization. This improves recall without
 * allowing a model to silently change tenant or metadata filters.
 */
@Service
public class QueryRewriteService {
    private static final Map<String, String> PHRASE_REWRITES = Map.of(
        "double charged", "duplicate charge",
        "charged twice", "duplicate charge",
        "sign in", "login",
        "log in", "login",
        "money back", "refund"
    );

    public String rewrite(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query text is required");
        }
        String rewritten = Normalizer.normalize(query, Normalizer.Form.NFKC)
            .replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : PHRASE_REWRITES.entrySet()) {
            rewritten = rewritten.replace(entry.getKey(), entry.getValue());
        }
        return rewritten;
    }
}
