package com.resolveiq.analysis.application.service.evidence;

public interface EvidenceObjectStore {
    void put(String key, String contentType, byte[] content);
    byte[] get(String key);
    void delete(String key);
}
