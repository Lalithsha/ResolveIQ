package com.resolveiq.ticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class AttachmentProductionGuard implements ApplicationRunner {
    private final String storage;
    private final String scanner;

    public AttachmentProductionGuard(@Value("${resolveiq.attachments.storage:}") String storage,
                                     @Value("${resolveiq.attachments.scanner:}") String scanner) {
        this.storage = storage;
        this.scanner = scanner;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"minio".equals(storage)) throw new IllegalStateException("Production attachments require MinIO-compatible storage");
        if (!"clamav".equals(scanner)) throw new IllegalStateException("Production attachments require ClamAV scanning");
    }
}
