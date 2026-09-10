package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceObjectStore;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class MinioEvidenceObjectStore implements EvidenceObjectStore {
    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioEvidenceObjectStore(
        @Value("${resolveiq.evidence.minio.endpoint:http://localhost:9000}") String endpoint,
        @Value("${resolveiq.evidence.minio.access-key:resolveiq_minio_dev_access}") String accessKey,
        @Value("${resolveiq.evidence.minio.secret-key:resolveiq_minio_dev_secret_key_12345}") String secretKey,
        @Value("${resolveiq.evidence.minio.bucket:resolveiq-evidence}") String bucket) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
    }

    @Override
    public void put(String key, String contentType, byte[] content) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType(contentType)
                .stream(new ByteArrayInputStream(content), content.length, -1).build());
        } catch (Exception error) {
            throw new IllegalStateException("Evidence object storage is unavailable", error);
        }
    }

    @Override
    public byte[] get(String key) {
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return stream.readAllBytes();
        } catch (Exception error) {
            throw new IllegalStateException("Evidence object is unavailable", error);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception error) {
            throw new IllegalStateException("Evidence object deletion failed", error);
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) return;
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        bucketReady = true;
    }
}
