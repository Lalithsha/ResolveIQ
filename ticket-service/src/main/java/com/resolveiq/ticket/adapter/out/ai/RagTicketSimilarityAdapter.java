package com.resolveiq.ticket.adapter.out.ai;

import com.resolveiq.ticket.application.port.TicketSimilarityPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Component
public class RagTicketSimilarityAdapter implements TicketSimilarityPort {

    private static final Logger log = LoggerFactory.getLogger(RagTicketSimilarityAdapter.class);

    private final RestTemplate restTemplate;
    private final String ragBaseUrl;

    public RagTicketSimilarityAdapter(
        RestTemplateBuilder builder,
        @Value("${resolveiq.rag.base-url:http://localhost:8086}") String ragBaseUrl
    ) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
        this.ragBaseUrl = ragBaseUrl;
    }

    @Override
    public List<SimilarityResult> findSimilarTickets(
        UUID tenantId,
        UUID ticketId,
        String queryText,
        String category,
        String product,
        String errorFingerprint,
        Double minSimilarity,
        List<CandidateTicket> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        try {
            String url = ragBaseUrl + "/api/v1/retrieval/ticket-similarity";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());

            Map<String, Object> body = Map.of(
                "ticketId", ticketId != null ? ticketId.toString() : UUID.randomUUID().toString(),
                "queryText", queryText,
                "category", category != null ? category : "",
                "product", product != null ? product : "",
                "errorFingerprint", errorFingerprint != null ? errorFingerprint : "",
                "minSimilarity", minSimilarity != null ? minSimilarity : 0.70,
                "candidates", candidates.stream().map(c -> Map.of(
                    "ticketId", c.ticketId().toString(),
                    "text", c.text() != null ? c.text() : "",
                    "category", c.category() != null ? c.category() : "",
                    "product", c.product() != null ? c.product() : "",
                    "errorFingerprint", c.errorFingerprint() != null ? c.errorFingerprint() : ""
                )).toList()
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            Map<?, ?> response = restTemplate.postForObject(url, requestEntity, Map.class);

            if (response != null && response.containsKey("matches")) {
                List<?> rawMatches = (List<?>) response.get("matches");
                List<SimilarityResult> results = new ArrayList<>();
                for (Object item : rawMatches) {
                    if (item instanceof Map<?, ?> m) {
                        UUID matchTicketId = UUID.fromString(String.valueOf(m.get("ticketId")));
                        double score = ((Number) m.get("similarityScore")).doubleValue();
                        boolean fpMatch = Boolean.TRUE.equals(m.get("fingerprintMatched"));
                        boolean catMatch = Boolean.TRUE.equals(m.get("categoryMatched"));
                        String exp = String.valueOf(m.get("explanation"));
                        results.add(new SimilarityResult(matchTicketId, score, fpMatch, catMatch, exp));
                    }
                }
                return results;
            }
        } catch (Exception e) {
            log.debug("RAG service call failed or unavailable ({}), falling back to deterministic local similarity", e.getMessage());
        }

        return localDeterministicSimilarity(queryText, category, product, errorFingerprint, minSimilarity, candidates);
    }

    private List<SimilarityResult> localDeterministicSimilarity(
        String queryText,
        String category,
        String product,
        String errorFingerprint,
        Double minSimilarity,
        List<CandidateTicket> candidates
    ) {
        double threshold = minSimilarity != null ? minSimilarity : 0.70;
        List<SimilarityResult> results = new ArrayList<>();
        Set<String> queryTokens = tokenize(queryText);

        for (CandidateTicket c : candidates) {
            Set<String> candTokens = tokenize(c.text());
            double jaccard = jaccard(queryTokens, candTokens);

            boolean fpMatch = errorFingerprint != null && errorFingerprint.equalsIgnoreCase(c.errorFingerprint());
            boolean catMatch = category != null && category.equalsIgnoreCase(c.category());
            boolean prodMatch = product != null && product.equalsIgnoreCase(c.product());

            double score = jaccard;
            if (fpMatch) score = Math.min(1.0, score + 0.25);
            if (catMatch) score = Math.min(1.0, score + 0.10);
            if (product != null && c.product() != null && !prodMatch) score = Math.max(0.0, score - 0.30);

            if (score >= threshold) {
                results.add(new SimilarityResult(
                    c.ticketId(),
                    Math.round(score * 1000.0) / 1000.0,
                    fpMatch,
                    catMatch,
                    String.format("Deterministic lexical similarity %.2f%s", jaccard, fpMatch ? " with matching error code" : "")
                ));
            }
        }

        results.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
        return results;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        String[] words = text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+");
        Set<String> tokens = new HashSet<>();
        for (String w : words) {
            if (w.length() > 2) tokens.add(w);
        }
        return tokens;
    }

    private double jaccard(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return (double) intersection.size() / union.size();
    }
}
