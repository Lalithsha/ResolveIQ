package com.resolveiq.analysis.evidence;

import com.resolveiq.analysis.adapter.out.evidence.*;
import com.resolveiq.analysis.application.dto.EvidenceDtos.*;
import com.resolveiq.analysis.application.service.evidence.EvidenceService;
import com.resolveiq.analysis.application.service.evidence.EvidenceObjectStore;
import com.resolveiq.analysis.domain.model.evidence.*;
import com.resolveiq.analysis.domain.repository.evidence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock private EvidenceJobRepository jobRepository;
    @Mock private EvidenceArtifactRepository artifactRepository;
    @Mock private EvidenceObservationRepository observationRepository;
    @Mock private EvidenceRedactionRepository redactionRepository;
    @Mock private EvidenceObjectStore objectStore;

    private EvidenceService evidenceService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();
    private final UUID attachmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evidenceService = new EvidenceService(
            jobRepository,
            artifactRepository,
            observationRepository,
            redactionRepository,
            objectStore,
            new FixtureEvidenceAdapters.Ocr(),
            new DeterministicLogAnalysisAdapter(),
            new DeterministicCsvSanitizationAdapter(),
            new FixtureEvidenceAdapters.Pdf(),
            new FixtureEvidenceAdapters.Video()
        );
    }

    @Test
    @DisplayName("Admission test rejects prohibited archive formats to prevent decompression bombs")
    void rejectsArchiveFormats() {
        byte[] content = "fake zip content".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() ->
            evidenceService.createUploadSession(tenantId, ticketId, attachmentId, "malicious.zip", "application/zip", content, true)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Archive formats");

        assertThatThrownBy(() ->
            evidenceService.createUploadSession(tenantId, ticketId, attachmentId, "malicious.tar.gz", "application/gzip", content, true)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Archive formats");
    }

    @Test
    @DisplayName("Admission test rejects files exceeding maximum count per ticket")
    void rejectsWhenTicketFileLimitReached() {
        when(jobRepository.countByTenantIdAndTicketId(tenantId, ticketId)).thenReturn(10L);

        byte[] content = "log content".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() ->
            evidenceService.createUploadSession(tenantId, ticketId, attachmentId, "app.log", "text/plain", content, true)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Maximum files per ticket limit reached");
    }

    @Test
    @DisplayName("Upload without consent remains QUEUED without generating artifacts")
    void uploadWithoutConsentRemainsQueued() {
        when(jobRepository.countByTenantIdAndTicketId(tenantId, ticketId)).thenReturn(0L);
        when(jobRepository.save(any(EvidenceJob.class))).thenAnswer(i -> i.getArgument(0));

        byte[] content = new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        EvidenceJobResponse response = evidenceService.createUploadSession(
            tenantId, ticketId, attachmentId, "error.png", "image/png", content, false
        );

        assertThat(response.pipelineStatus()).isEqualTo(PipelineStatus.QUEUED);
        assertThat(response.consentGranted()).isFalse();
        // pipeline should NOT run
        verify(artifactRepository, never()).save(any());
        verify(observationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Granting consent triggers pipeline processing to READY state")
    void grantingConsentRunsPipeline() {
        EvidenceJob job = new EvidenceJob(tenantId, ticketId, attachmentId, "error.png", "image/png", 100, false);
        job.setRawContent("Error: SAML_SIGNATURE_INVALID");
        when(jobRepository.findByTenantIdAndTicketIdAndAttachmentId(tenantId, ticketId, attachmentId))
            .thenReturn(Optional.of(job));

        EvidenceJobResponse response = evidenceService.updateConsent(tenantId, ticketId, attachmentId, true);

        assertThat(response.pipelineStatus()).isEqualTo(PipelineStatus.READY);
        assertThat(response.consentGranted()).isTrue();
        verify(artifactRepository).save(any());
        verify(observationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Revoking consent clears derived artifacts and returns job to QUEUED")
    void revokingConsentClearsArtifacts() {
        EvidenceJob job = new EvidenceJob(tenantId, ticketId, attachmentId, "error.png", "image/png", 100, true);
        job.setPipelineStatus(PipelineStatus.READY);
        when(jobRepository.findByTenantIdAndTicketIdAndAttachmentId(tenantId, ticketId, attachmentId))
            .thenReturn(Optional.of(job));

        EvidenceJobResponse response = evidenceService.updateConsent(tenantId, ticketId, attachmentId, false);

        assertThat(response.pipelineStatus()).isEqualTo(PipelineStatus.QUEUED);
        assertThat(response.consentGranted()).isFalse();
        verify(artifactRepository).deleteByTenantIdAndJobId(tenantId, job.getId());
        verify(observationRepository).deleteByTenantIdAndJobId(tenantId, job.getId());
    }

    @Test
    @DisplayName("Tombstoning evidence marks it TOMBSTONED and deletes all derivatives")
    void tombstoneEvidenceRemovesDerivatives() {
        EvidenceJob job = new EvidenceJob(tenantId, ticketId, attachmentId, "error.png", "image/png", 100, true);
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));

        evidenceService.tombstoneEvidence(tenantId, job.getId());

        assertThat(job.getPipelineStatus()).isEqualTo(PipelineStatus.TOMBSTONED);
        verify(artifactRepository).deleteByTenantIdAndJobId(tenantId, job.getId());
        verify(observationRepository).deleteByTenantIdAndJobId(tenantId, job.getId());
    }

    @Test
    @DisplayName("Original content access requires EVIDENCE_VIEW_ORIGINAL permission")
    void originalContentAccessAuthorizationCheck() {
        EvidenceJob job = new EvidenceJob(tenantId, ticketId, attachmentId, "app.log", "text/plain", 100, true);
        job.setRawContent("raw secrets sk-live-12345");
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));

        // Attempt as regular user without permission
        assertThatThrownBy(() ->
            evidenceService.getContent(tenantId, job.getId(), "original", Set.of("AGENT"), Set.of(), "Need to view")
        ).isInstanceOf(AccessDeniedException.class)
         .hasMessageContaining("EVIDENCE_VIEW_ORIGINAL");

        // Attempt with EVIDENCE_VIEW_ORIGINAL permission
        String content = evidenceService.getContent(
            tenantId, job.getId(), "original", Set.of("AGENT"), Set.of("EVIDENCE_VIEW_ORIGINAL"), "Valid audit reason"
        );
        assertThat(content).isEqualTo("raw secrets sk-live-12345");
    }

    @Test
    @DisplayName("Redacted content is accessible to standard users without EVIDENCE_VIEW_ORIGINAL")
    void redactedContentAccessForStandardUsers() {
        EvidenceJob job = new EvidenceJob(tenantId, ticketId, attachmentId, "app.log", "text/plain", 100, true);
        when(jobRepository.findByIdAndTenantId(job.getId(), tenantId)).thenReturn(Optional.of(job));

        EvidenceArtifact artifact = new EvidenceArtifact(
            tenantId, job.getId(), ticketId, ArtifactType.LOG_REDACTED, "redacted/key", null, null, "hash", "INTERNAL", "redacted log content"
        );
        when(artifactRepository.findByTenantIdAndJobId(tenantId, job.getId())).thenReturn(List.of(artifact));

        String content = evidenceService.getContent(
            tenantId, job.getId(), "redacted", Set.of("CUSTOMER"), Set.of(), null
        );
        assertThat(content).isEqualTo("redacted log content");
    }
}
