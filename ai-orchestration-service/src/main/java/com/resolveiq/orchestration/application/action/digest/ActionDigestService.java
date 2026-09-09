package com.resolveiq.orchestration.application.action.digest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class ActionDigestService {

    private final ObjectMapper canonicalMapper;

    public ActionDigestService() {
        this.canonicalMapper = new ObjectMapper();
        this.canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public record CanonicalDigestResult(String canonicalBytes, String digestHex) {}

    public CanonicalDigestResult generateDigestV1(
            UUID tenantId,
            UUID ticketId,
            UUID proposalId,
            String actionType,
            String target,
            Map<String, Object> normalizedInput,
            String currentStateVersion,
            String policyVersion,
            Map<String, Object> approvalRequirements,
            Instant expiresAt) {

        Map<String, Object> canonicalMap = new TreeMap<>();
        canonicalMap.put("actionType", actionType);
        canonicalMap.put("approvalRequirements", new TreeMap<>(approvalRequirements != null ? approvalRequirements : Map.of()));
        canonicalMap.put("currentStateVersion", currentStateVersion);
        canonicalMap.put("digestVersion", "v1");
        canonicalMap.put("expiresAt", expiresAt != null ? expiresAt.toString() : "");
        canonicalMap.put("normalizedInput", new TreeMap<>(normalizedInput != null ? normalizedInput : Map.of()));
        canonicalMap.put("policyVersion", policyVersion);
        canonicalMap.put("proposalId", proposalId.toString());
        canonicalMap.put("target", target);
        canonicalMap.put("tenantId", tenantId.toString());
        canonicalMap.put("ticketId", ticketId.toString());

        try {
            String canonicalBytes = canonicalMapper.writeValueAsString(canonicalMap);
            String digestHex = sha256Hex(canonicalBytes);
            return new CanonicalDigestResult(canonicalBytes, digestHex);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize canonical action digest", e);
        }
    }

    public String computeInputHash(String inputJson) {
        if (inputJson == null) {
            return sha256Hex("{}");
        }
        try {
            // Normalize JSON keys before hashing
            Map<String, Object> map = canonicalMapper.readValue(inputJson, Map.class);
            Map<String, Object> sortedMap = new TreeMap<>(map);
            return sha256Hex(canonicalMapper.writeValueAsString(sortedMap));
        } catch (Exception e) {
            return sha256Hex(inputJson.trim());
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
