package com.resolveiq.analysis.application.service.evidence;

import com.resolveiq.analysis.application.dto.EvidenceDtos.*;
import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.domain.model.evidence.*;
import com.resolveiq.analysis.domain.repository.evidence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class EvidenceService implements EvidenceServicePort {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    private static final int MAX_FILES_PER_TICKET = 10;
    private static final long MAX_DOCUMENT_BYTES = 20 * 1024 * 1024; // 20 MiB
    private static final long MAX_MEDIA_BYTES = 100 * 1024 * 1024; // 100 MiB
    private static final Set<String> PROHIBITED_ARCHIVE_EXTENSIONS = Set.of(".zip", ".tar", ".gz", ".tgz", ".rar", ".7z", ".bz2");

    private final EvidenceJobRepository jobRepository;
    private final EvidenceArtifactRepository artifactRepository;
    private final EvidenceObservationRepository observationRepository;
    private final EvidenceRedactionRepository redactionRepository;
    private final EvidenceObjectStore objectStore;

    private final OcrPort ocrPort;
    private final LogAnalysisPort logAnalysisPort;
    private final CsvSanitizationPort csvSanitizationPort;
    private final PdfExtractionPort pdfExtractionPort;
    private final VideoFrameSamplingPort videoPort;

    public EvidenceService(EvidenceJobRepository jobRepository,
                           EvidenceArtifactRepository artifactRepository,
                           EvidenceObservationRepository observationRepository,
                           EvidenceRedactionRepository redactionRepository,
                           EvidenceObjectStore objectStore,
                           OcrPort ocrPort,
                           LogAnalysisPort logAnalysisPort,
                           CsvSanitizationPort csvSanitizationPort,
                           PdfExtractionPort pdfExtractionPort,
                           VideoFrameSamplingPort videoPort) {
        this.jobRepository = jobRepository;
        this.artifactRepository = artifactRepository;
        this.observationRepository = observationRepository;
        this.redactionRepository = redactionRepository;
        this.objectStore = objectStore;
        this.ocrPort = ocrPort;
        this.logAnalysisPort = logAnalysisPort;
        this.csvSanitizationPort = csvSanitizationPort;
        this.pdfExtractionPort = pdfExtractionPort;
        this.videoPort = videoPort;
    }

    public EvidenceJobResponse createUploadSession(UUID tenantId, UUID ticketId, UUID attachmentId,
                                                   String fileName, String mediaType, byte[] content,
                                                   boolean consentGranted) {
        validateAdmission(tenantId, ticketId, fileName, mediaType, content);

        EvidenceJob job = new EvidenceJob(
            tenantId, ticketId,
            attachmentId != null ? attachmentId : UUID.randomUUID(),
            fileName, mediaType, content.length, consentGranted
        );
        String objectKey = "quarantine/" + tenantId + "/" + ticketId + "/" + job.getAttachmentId();
        objectStore.put(objectKey, mediaType, content);
        job.setOriginalObjectKey(objectKey);
        job.setOriginalChecksumSha256(sha256(content));
        job.setRawContent(null);
        job = jobRepository.save(job);

        if (consentGranted) {
            runPipeline(job, content);
        } else {
            log.info("Evidence job {} queued pending customer consent", job.getId());
        }

        return toJobResponse(job);
    }

    public EvidenceJobResponse updateConsent(UUID tenantId, UUID ticketId, UUID attachmentId, boolean consent) {
        EvidenceJob job = jobRepository.findByTenantIdAndTicketIdAndAttachmentId(tenantId, ticketId, attachmentId)
            .orElseThrow(() -> new NoSuchElementException("Evidence job not found for attachment: " + attachmentId));

        job.setConsentGranted(consent);
        if (consent) {
            if (job.getPipelineStatus() == PipelineStatus.QUEUED || job.getPipelineStatus() == PipelineStatus.FAILED) {
                byte[] bytes = extractRawBytes(job);
                runPipeline(job, bytes);
            }
        } else {
            // Revoking consent cancels queued/derived content
            artifactRepository.deleteByTenantIdAndJobId(tenantId, job.getId());
            observationRepository.deleteByTenantIdAndJobId(tenantId, job.getId());
            redactionRepository.deleteByTenantIdAndJobId(tenantId, job.getId());
            job.setPipelineStatus(PipelineStatus.QUEUED);
            log.info("Consent revoked for job {}. Cleared derived artifacts.", job.getId());
        }
        return toJobResponse(job);
    }

    public EvidenceJobResponse reprocess(UUID tenantId, UUID jobId) {
        EvidenceJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Evidence job not found: " + jobId));

        if (job.getPipelineStatus() == PipelineStatus.TOMBSTONED) {
            throw new IllegalStateException("Cannot reprocess tombstoned evidence");
        }

        byte[] bytes = extractRawBytes(job);
        artifactRepository.deleteByTenantIdAndJobId(tenantId, job.getId());
        observationRepository.deleteByTenantIdAndJobId(tenantId, job.getId());
        redactionRepository.deleteByTenantIdAndJobId(tenantId, job.getId());

        runPipeline(job, bytes);
        return toJobResponse(job);
    }

    public void tombstoneEvidence(UUID tenantId, UUID jobId) {
        EvidenceJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Evidence job not found: " + jobId));

        job.setPipelineStatus(PipelineStatus.TOMBSTONED);
        job.setRawContent(null);
        if (job.getOriginalObjectKey() != null) {
            objectStore.delete(job.getOriginalObjectKey());
        }
        artifactRepository.deleteByTenantIdAndJobId(tenantId, jobId);
        observationRepository.deleteByTenantIdAndJobId(tenantId, jobId);
        redactionRepository.deleteByTenantIdAndJobId(tenantId, jobId);
        log.info("Evidence job {} tombstoned and derived artifacts deleted under retention policy", jobId);
    }

    @Transactional(readOnly = true)
    public EvidenceJobResponse getJob(UUID tenantId, UUID jobId) {
        EvidenceJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Evidence job not found: " + jobId));
        if (job.getPipelineStatus() == PipelineStatus.TOMBSTONED) {
            throw new NoSuchElementException("Evidence job has been tombstoned: " + jobId);
        }
        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<EvidenceJobResponse> listByTicket(UUID tenantId, UUID ticketId) {
        return jobRepository.findByTenantIdAndTicketIdOrderByCreatedAtDesc(tenantId, ticketId).stream()
            .filter(j -> j.getPipelineStatus() != PipelineStatus.TOMBSTONED)
            .map(this::toJobResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceArtifactResponse> getArtifacts(UUID tenantId, UUID jobId) {
        getJob(tenantId, jobId); // ensures existence and active state
        return artifactRepository.findByTenantIdAndJobId(tenantId, jobId).stream()
            .map(this::toArtifactResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceObservationResponse> getObservations(UUID tenantId, UUID jobId) {
        getJob(tenantId, jobId);
        return observationRepository.findByTenantIdAndJobId(tenantId, jobId).stream()
            .map(this::toObservationResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public String getContent(UUID tenantId, UUID jobId, String variant, Set<String> roles,
                             Set<String> permissions, String reason) {
        EvidenceJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Evidence job not found: " + jobId));

        if (job.getPipelineStatus() == PipelineStatus.TOMBSTONED) {
            throw new NoSuchElementException("Evidence is tombstoned");
        }

        if ("original".equalsIgnoreCase(variant)) {
            boolean hasPermission = permissions.contains("EVIDENCE_VIEW_ORIGINAL");
            if (!hasPermission) {
                log.warn("Unauthorized access attempt to original evidence: jobId={}, tenantId={}", jobId, tenantId);
                throw new org.springframework.security.access.AccessDeniedException("Missing required permission: EVIDENCE_VIEW_ORIGINAL");
            }
            log.info("SECURITY AUDIT: Original evidence viewed for job {} by authorized principal. Reason: {}", jobId, reason);
            return new String(extractRawBytes(job), StandardCharsets.UTF_8);
        } else {
            // Default to redacted content
            List<EvidenceArtifact> artifacts = artifactRepository.findByTenantIdAndJobId(tenantId, jobId);
            if (!artifacts.isEmpty()) {
                return artifacts.get(0).getRedactedContent();
            }
            return "[REDACTED_CONTENT_NOT_AVAILABLE]";
        }
    }

    private void runPipeline(EvidenceJob job, byte[] content) {
        try {
            job.setPipelineStatus(PipelineStatus.EXTRACTING);
            job.setAttempt(job.getAttempt() + 1);

            ExtractionResult result = routeExtraction(job.getFileName(), job.getMediaType(), content);

            job.setPipelineStatus(PipelineStatus.REDACTING);
            // Save artifacts
            String hash = sha256(result.redactedContent().getBytes(StandardCharsets.UTF_8));
            EvidenceArtifact artifact = new EvidenceArtifact(
                job.getTenantId(), job.getId(), job.getTicketId(),
                result.artifactType(),
                "redacted/" + job.getTenantId() + "/" + job.getId() + "/" + job.getFileName(),
                result.pageOrFrame(), result.timestampSeconds(),
                hash, "INTERNAL", result.redactedContent()
            );
            artifactRepository.save(artifact);

            // Save observations
            for (ObservationData obs : result.observations()) {
                observationRepository.save(new EvidenceObservation(
                    job.getTenantId(), job.getId(), job.getTicketId(),
                    obs.type(), obs.codeOrKey(), obs.summary(), obs.confidence(), obs.sourceCoordinates()
                ));
            }

            // Save redactions
            for (RedactionData red : result.redactions()) {
                redactionRepository.save(new EvidenceRedaction(
                    job.getTenantId(), job.getId(), red.category(), red.location(), red.maskMethod()
                ));
            }

            job.setPipelineStatus(PipelineStatus.READY);
            job.setToolVersions("{\"tesseract\": \"5.3\", \"tika\": \"2.9\", \"ffmpeg\": \"6.1\"}");
            log.info("Evidence job {} completed analysis successfully with status READY", job.getId());
        } catch (Exception e) {
            log.error("Failed to process evidence job {}", job.getId(), e);
            job.setPipelineStatus(PipelineStatus.FAILED);
            job.setErrorDetails(e.getMessage());
        }
    }

    private ExtractionResult routeExtraction(String fileName, String mediaType, byte[] content) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        String lowerType = mediaType != null ? mediaType.toLowerCase() : "";

        if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerType.startsWith("image/")) {
            return ocrPort.processImage(fileName, content);
        } else if (lowerName.endsWith(".csv") || lowerType.contains("csv")) {
            return csvSanitizationPort.sanitizeCsv(fileName, content);
        } else if (lowerName.endsWith(".pdf") || lowerType.contains("pdf")) {
            return pdfExtractionPort.extractPdf(fileName, content);
        } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".webm") || lowerName.endsWith(".mp3") ||
                   lowerName.endsWith(".wav") || lowerType.startsWith("video/") || lowerType.startsWith("audio/")) {
            return videoPort.sampleVideo(fileName, content);
        } else {
            // Default to log / text analysis
            return logAnalysisPort.processLog(fileName, content);
        }
    }

    private boolean isBinaryContent(String fileName, String mediaType) {
        String fn = fileName != null ? fileName.toLowerCase() : "";
        String mt = mediaType != null ? mediaType.toLowerCase() : "";
        return fn.endsWith(".png") || fn.endsWith(".jpg") || fn.endsWith(".jpeg") ||
               fn.endsWith(".pdf") || fn.endsWith(".mp4") || fn.endsWith(".webm") ||
               fn.endsWith(".mp3") || fn.endsWith(".wav") || mt.startsWith("image/") ||
               mt.startsWith("video/") || mt.startsWith("audio/") || mt.contains("pdf");
    }

    private byte[] extractRawBytes(EvidenceJob job) {
        if (job.getOriginalObjectKey() != null) {
            byte[] stored = objectStore.get(job.getOriginalObjectKey());
            if (job.getOriginalChecksumSha256() != null && !job.getOriginalChecksumSha256().equals(sha256(stored))) {
                throw new IllegalStateException("Evidence checksum mismatch; source object is quarantined");
            }
            return stored;
        }
        // Read compatibility only for pre-V4 records. New uploads never use raw_content.
        if (job.getRawContent() == null) return new byte[0];
        if (job.getRawContent().startsWith("base64:")) {
            return Base64.getDecoder().decode(job.getRawContent().substring(7));
        }
        return job.getRawContent().getBytes(StandardCharsets.UTF_8);
    }

    private void validateAdmission(UUID tenantId, UUID ticketId, String fileName, String mediaType, byte[] content) {
        long currentCount = jobRepository.countByTenantIdAndTicketId(tenantId, ticketId);
        if (currentCount >= MAX_FILES_PER_TICKET) {
            throw new IllegalArgumentException("Maximum files per ticket limit reached: " + MAX_FILES_PER_TICKET);
        }

        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        for (String ext : PROHIBITED_ARCHIVE_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                throw new IllegalArgumentException("Archive formats (" + ext + ") are prohibited to prevent decompression bombs");
            }
        }

        // Magic bytes check for archive formats
        if (content.length >= 4) {
            if ((content[0] == 0x50 && content[1] == 0x4B) || // PK zip
                (content[0] == 0x1F && (content[1] & 0xFF) == 0x8B)) { // gzip
                throw new IllegalArgumentException("Archive formats are prohibited to prevent decompression bombs");
            }
        }

        // Malware detection check
        String contentHeader = new String(content, 0, Math.min(content.length, 512), StandardCharsets.ISO_8859_1);
        if (contentHeader.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE")) {
            throw new SecurityException("Malware signature detected in uploaded evidence: quarantine active");
        }

        // Magic byte verification for declared extensions
        if (lowerName.endsWith(".png")) {
            if (content.length < 8 || (content[0] & 0xFF) != 0x89 || content[1] != 'P' || content[2] != 'N' || content[3] != 'G') {
                throw new IllegalArgumentException("Invalid PNG file: magic bytes mismatch");
            }
        } else if (lowerName.endsWith(".pdf")) {
            if (content.length < 4 || content[0] != '%' || content[1] != 'P' || content[2] != 'D' || content[3] != 'F') {
                throw new IllegalArgumentException("Invalid PDF file: magic bytes mismatch");
            }
        }

        String lowerType = mediaType != null ? mediaType.toLowerCase() : "";
        boolean isMedia = lowerType.startsWith("video/") || lowerType.startsWith("audio/") ||
            lowerName.endsWith(".mp4") || lowerName.endsWith(".webm") || lowerName.endsWith(".mp3");
        long maxBytes = isMedia ? MAX_MEDIA_BYTES : MAX_DOCUMENT_BYTES;

        if (content.length > maxBytes) {
            throw new IllegalArgumentException("File size exceeds limit of " + (maxBytes / (1024 * 1024)) + " MiB");
        }
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "sha256-placeholder";
        }
    }

    private EvidenceJobResponse toJobResponse(EvidenceJob job) {
        return new EvidenceJobResponse(
            job.getId(), job.getTenantId(), job.getTicketId(), job.getAttachmentId(),
            job.getFileName(), job.getMediaType(), job.getFileSizeBytes(),
            job.getPipelineStatus(), job.isConsentGranted(), job.getRetentionClass(),
            job.getAttempt(), job.getErrorDetails(), job.getCreatedAt(), job.getUpdatedAt()
        );
    }

    private EvidenceArtifactResponse toArtifactResponse(EvidenceArtifact a) {
        return new EvidenceArtifactResponse(
            a.getId(), a.getJobId(), a.getTicketId(), a.getArtifactType().name(),
            a.getRedactedObjectKey(), a.getPageOrFrame(), a.getTimestampSeconds(),
            a.getChecksumSha256(), a.getSensitivityClass(), a.getRedactedContent(), a.getCreatedAt()
        );
    }

    private EvidenceObservationResponse toObservationResponse(EvidenceObservation o) {
        return new EvidenceObservationResponse(
            o.getId(), o.getJobId(), o.getTicketId(), o.getObservationType().name(),
            o.getCodeOrKey(), o.getSummary(), o.getConfidence(), o.getSourceCoordinates(), o.getCreatedAt()
        );
    }
}
