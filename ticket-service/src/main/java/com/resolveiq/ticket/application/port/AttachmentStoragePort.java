package com.resolveiq.ticket.application.port;

public interface AttachmentStoragePort {
    void put(String key, String contentType, byte[] content);
    byte[] get(String key);
    void delete(String key);
}
