package com.resolveiq.ticket.adapter.out.storage;

import com.resolveiq.ticket.application.port.AttachmentStoragePort;
import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@ConditionalOnProperty(name = "resolveiq.attachments.storage", havingValue = "minio", matchIfMissing = true)
public class MinioAttachmentStorageAdapter implements AttachmentStoragePort {
    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioAttachmentStorageAdapter(
        @Value("${resolveiq.attachments.minio.endpoint:http://localhost:9000}") String endpoint,
        @Value("${resolveiq.attachments.minio.access-key:resolveiq_minio_dev_access}") String accessKey,
        @Value("${resolveiq.attachments.minio.secret-key:resolveiq_minio_dev_secret_key_12345}") String secretKey,
        @Value("${resolveiq.attachments.minio.bucket:resolveiq-attachments}") String bucket
    ) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
    }

    @Override
    public void put(String key, String contentType, byte[] content) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .contentType(contentType).stream(new ByteArrayInputStream(content), content.length, -1).build());
        } catch (Exception error) {
            throw new IllegalStateException("Attachment storage is unavailable", error);
        }
    }

    @Override
    public byte[] get(String key) {
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return stream.readAllBytes();
        } catch (Exception error) {
            throw new IllegalStateException("Attachment content is unavailable", error);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ignored) {
            // best-effort compensation; the metadata transaction remains authoritative
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) return;
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        bucketReady = true;
    }
}
