import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import {
  FileText,
  Image as ImageIcon,
  Video,
  FileCode,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Eye,
  EyeOff,
  RefreshCw,
  Trash2,
  Play,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  Lock,
  Clock,
  ChevronRight,
  Info,
  X
} from 'lucide-react';
import { api } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import {
  EvidenceJobResponse,
  EvidenceArtifactResponse,
  EvidenceObservationResponse
} from '../../types';

interface Props {
  ticketId: string;
  isAgent?: boolean;
  onEvidenceUpdated?: () => void;
}

export const EvidenceLabCard: React.FC<Props> = ({
  ticketId,
  isAgent = false,
  onEvidenceUpdated
}) => {
  const { user } = useAuth();
  const [evidenceJobs, setEvidenceJobs] = useState<EvidenceJobResponse[]>([]);
  const [selectedJob, setSelectedJob] = useState<EvidenceJobResponse | null>(null);
  const [artifacts, setArtifacts] = useState<EvidenceArtifactResponse[]>([]);
  const [observations, setObservations] = useState<EvidenceObservationResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  // Upload Form State
  const [uploadFileName, setUploadFileName] = useState('');
  const [uploadMediaType, setUploadMediaType] = useState('image/png');
  const [uploadContent, setUploadContent] = useState('');
  const [consentGranted, setConsentGranted] = useState(true);

  // Original Access State
  const [viewVariant, setViewVariant] = useState<'redacted' | 'original'>('redacted');
  const [originalContent, setOriginalContent] = useState<string | null>(null);
  const [originalAccessReason, setOriginalAccessReason] = useState('');
  const [isRequestingOriginal, setIsRequestingOriginal] = useState(false);
  const [originalError, setOriginalError] = useState<string | null>(null);

  // Video / Audio Jump State
  const [highlightedTimestamp, setHighlightedTimestamp] = useState<string | null>(null);

  const loadEvidence = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const jobs = await api.listEvidence(ticketId);
      setEvidenceJobs(jobs || []);
      if (jobs && jobs.length > 0 && !selectedJob) {
        setSelectedJob(jobs[0]);
      } else if (selectedJob) {
        const refreshed = jobs.find((j: EvidenceJobResponse) => j.id === selectedJob.id);
        if (refreshed) setSelectedJob(refreshed);
      }
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to load evidence');
    } finally {
      setIsLoading(false);
    }
  }, [ticketId, selectedJob]);

  useEffect(() => {
    loadEvidence();
  }, [ticketId]);

  useEffect(() => {
    if (!isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setIsOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  // When selectedJob changes, load artifacts & observations
  useEffect(() => {
    if (!selectedJob) {
      setArtifacts([]);
      setObservations([]);
      return;
    }

    const loadDetails = async () => {
      try {
        const [artData, obsData] = await Promise.all([
          api.getEvidenceArtifacts(selectedJob.id),
          api.getEvidenceObservations(selectedJob.id)
        ]);
        setArtifacts(artData || []);
        setObservations(obsData || []);
        setViewVariant('redacted');
        setOriginalContent(null);
        setOriginalError(null);
      } catch (err) {
        console.error('Failed to load job details', err);
      }
    };
    loadDetails();
  }, [selectedJob?.id]);

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFileName.trim()) return;

    setIsUploading(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const encoder = new TextEncoder();
      const contentBytes = Array.from(encoder.encode(uploadContent || 'sample log/evidence text'));

      await api.uploadEvidence(ticketId, {
        fileName: uploadFileName,
        mediaType: uploadMediaType,
        content: contentBytes,
        consentGranted
      });

      setSuccessMessage('Evidence uploaded and queued for multimodal analysis');
      setUploadFileName('');
      setUploadContent('');
      await loadEvidence();
      if (onEvidenceUpdated) onEvidenceUpdated();
    } catch (err: any) {
      setErrorMessage(err?.message || 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const handleConsentToggle = async (job: EvidenceJobResponse, newConsent: boolean) => {
    try {
      await api.updateEvidenceConsent(ticketId, job.attachmentId, newConsent);
      setSuccessMessage(newConsent ? 'AI analysis consent granted' : 'AI analysis consent revoked');
      await loadEvidence();
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to update consent');
    }
  };

  const handleReprocess = async (jobId: string) => {
    try {
      await api.reprocessEvidence(jobId);
      setSuccessMessage('Evidence reprocessed successfully');
      await loadEvidence();
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to reprocess evidence');
    }
  };

  const handleDelete = async (jobId: string) => {
    if (!confirm('Are you sure you want to tombstone and delete this evidence? All derivatives will be permanently purged under retention policy.')) return;
    try {
      await api.deleteEvidence(jobId);
      setSuccessMessage('Evidence tombstoned and derivatives removed under retention policy');
      setSelectedJob(null);
      await loadEvidence();
      if (onEvidenceUpdated) onEvidenceUpdated();
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to delete evidence');
    }
  };

  const handleViewOriginal = async () => {
    if (!selectedJob) return;
    setIsRequestingOriginal(true);
    setOriginalError(null);
    try {
      const content = await api.getEvidenceContent(
        selectedJob.id,
        'original',
        originalAccessReason || `Diagnostic investigation for ticket ${ticketId}`
      );
      setOriginalContent(content);
      setViewVariant('original');
      setSuccessMessage('Security Audit Logged: Original evidence viewed.');
    } catch (err: any) {
      setOriginalError(err?.message || 'Access Denied: EVIDENCE_VIEW_ORIGINAL permission required.');
    } finally {
      setIsRequestingOriginal(false);
    }
  };

  const getMediaIcon = (type: string, name: string) => {
    const n = name.toLowerCase();
    const t = type.toLowerCase();
    if (n.endsWith('.mp4') || n.endsWith('.webm') || t.startsWith('video/')) return <Video className="w-5 h-5 text-primary" />;
    if (n.endsWith('.png') || n.endsWith('.jpg') || t.startsWith('image/')) return <ImageIcon className="w-5 h-5 text-success" />;
    if (n.endsWith('.pdf') || t.includes('pdf')) return <FileText className="w-5 h-5 text-danger" />;
    if (n.endsWith('.csv') || t.includes('csv')) return <FileCode className="w-5 h-5 text-warning" />;
    return <FileText className="w-5 h-5 text-info" />;
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'READY':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-success/10 text-success border border-success/20 flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Ready</span>;
      case 'QUEUED':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-info/10 text-info border border-info/20 flex items-center gap-1"><Clock className="w-3 h-3" /> Queued</span>;
      case 'EXTRACTING':
      case 'REDACTING':
      case 'ANALYZING':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-ai-soft text-ai border border-ai/20 flex items-center gap-1 animate-pulse"><RefreshCw className="w-3 h-3 animate-spin" /> {status}</span>;
      case 'BLOCKED_REDACTION':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-warning/10 text-warning border border-warning/20 flex items-center gap-1"><AlertTriangle className="w-3 h-3" /> Redaction Blocked</span>;
      case 'FAILED':
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-danger/10 text-danger border border-danger/20 flex items-center gap-1"><AlertTriangle className="w-3 h-3" /> Failed</span>;
      default:
        return <span className="px-2 py-0.5 text-xs font-semibold rounded bg-surface-muted text-DEFAULT">{status}</span>;
    }
  };

  return (
    <>
      <section className="rounded-card border border-border-subtle bg-surface p-4 shadow-sm">
        <div className="flex items-start gap-3">
          <div className="rounded-lg border border-primary/20 bg-primary/10 p-2 text-primary">
            <Shield className="h-4 w-4" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h3 className="text-sm font-semibold text-DEFAULT">Evidence Lab</h3>
              <span className="rounded-full border border-border-subtle bg-surface-muted px-2 py-0.5 text-[10px] font-medium text-muted">
                {evidenceJobs.length} {evidenceJobs.length === 1 ? 'artifact' : 'artifacts'}
              </span>
            </div>
            <p className="mt-1 text-xs leading-5 text-muted">
              Inspect sanitized screenshots, logs, documents, and recordings in a focused workspace.
            </p>
          </div>
        </div>

        <div className="mt-4 flex items-center justify-between gap-3 border-t border-border-subtle pt-3">
          <div className="min-w-0 text-[11px] text-muted">
            {isLoading
              ? 'Checking evidence…'
              : evidenceJobs[0]
              ? <span className="truncate">Latest: <span className="font-medium text-DEFAULT">{evidenceJobs[0].fileName}</span> · {evidenceJobs[0].pipelineStatus}</span>
              : 'No diagnostic evidence attached'}
          </div>
          <button onClick={() => setIsOpen(true)} className="btn-secondary shrink-0 px-3 py-1.5 text-xs">
            Open Evidence Lab
          </button>
        </div>
      </section>

      {isOpen && createPortal(
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-3 backdrop-blur-sm sm:p-6"
          role="dialog"
          aria-modal="true"
          aria-labelledby="evidence-lab-title"
          onMouseDown={(event) => {
            if (event.currentTarget === event.target) setIsOpen(false);
          }}
        >
          <div className="max-h-[92vh] w-full max-w-6xl overflow-y-auto rounded-xl border border-border bg-surface p-4 text-DEFAULT shadow-none sm:p-6">
            {/* Header */}
            <div className="mb-5 flex items-start justify-between gap-4 border-b border-border-subtle pb-4">
              <div className="flex min-w-0 items-start gap-3">
                <div className="rounded-lg border border-primary/20 bg-primary/10 p-2 text-primary">
                  <Shield className="h-5 w-5" />
                </div>
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 id="evidence-lab-title" className="text-base font-bold text-DEFAULT">Multimodal Evidence Lab</h3>
                    <span className="rounded-full border border-border bg-surface-muted px-2 py-0.5 text-[10px] font-normal text-muted">
                      Air-gapped redaction
                    </span>
                  </div>
                  <p className="mt-1 text-xs leading-5 text-muted">
                    Upload safe fixtures, inspect sanitized derivatives, and request audited original access.
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <button
                  onClick={loadEvidence}
                  disabled={isLoading}
                  className="rounded-lg border border-border p-2 text-muted transition-colors hover:bg-surface-muted hover:text-DEFAULT"
                  title="Refresh evidence"
                >
                  <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin text-primary' : ''}`} />
                </button>
                <button
                  onClick={() => setIsOpen(false)}
                  className="rounded-lg border border-border p-2 text-muted transition-colors hover:bg-surface-muted hover:text-DEFAULT"
                  title="Close Evidence Lab"
                  aria-label="Close Evidence Lab"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>

      {/* Notifications */}
      {errorMessage && (
        <div className="mb-4 p-3 bg-danger/10 border border-danger/20 rounded-lg text-danger text-xs flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}
      {successMessage && (
        <div className="mb-4 p-3 bg-success/10 border border-success/20 rounded-lg text-success text-xs flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Main Grid: Left Evidence List / Right Detail Viewer */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-5">
        {/* Left Column: Evidence List & Upload */}
        <div className="md:col-span-5 flex flex-col gap-4">
          <div className="text-xs font-semibold text-muted uppercase tracking-wider flex items-center justify-between">
            <span>Evidence Artifacts ({evidenceJobs.length})</span>
            <span className="text-[10px] text-muted">Max 10 per ticket</span>
          </div>

          {evidenceJobs.length === 0 && !isLoading ? (
            <div className="p-6 text-center border border-dashed border-border-subtle rounded-lg bg-surface-muted">
              <FileCode className="w-8 h-8 text-muted mx-auto mb-2" />
              <p className="text-xs text-muted">No diagnostic evidence attached yet.</p>
              <p className="text-[11px] text-muted mt-1">
                Upload screenshots, log files, CSVs, or video captures below.
              </p>
            </div>
          ) : (
            <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
              {evidenceJobs.map((job) => {
                const isSelected = selectedJob?.id === job.id;
                return (
                  <div
                    key={job.id}
                    onClick={() => setSelectedJob(job)}
                    className={`p-3 rounded-lg border cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-primary/5 border-primary/30 shadow-sm'
                        : 'bg-surface border-border-subtle hover:border-border'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2 overflow-hidden">
                        {getMediaIcon(job.mediaType, job.fileName)}
                        <span className="text-xs font-medium text-DEFAULT truncate" title={job.fileName}>
                          {job.fileName}
                        </span>
                      </div>
                      {getStatusBadge(job.pipelineStatus)}
                    </div>
                    <div className="mt-2 flex items-center justify-between text-[11px] text-muted">
                      <span>{(job.fileSizeBytes / 1024).toFixed(1)} KB</span>
                      <div className="flex items-center gap-2">
                        {job.consentGranted ? (
                          <span className="text-success flex items-center gap-0.5">
                            <ShieldCheck className="w-3 h-3" /> AI Consent
                          </span>
                        ) : (
                          <span className="text-warning flex items-center gap-0.5">
                            <ShieldAlert className="w-3 h-3" /> No Consent
                          </span>
                        )}
                        <ChevronRight className="w-3.5 h-3.5 text-muted" />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Upload Accordion */}
          <div className="mt-2 border border-border-subtle rounded-lg p-3 bg-surface-muted">
            <h4 className="text-xs font-semibold text-DEFAULT mb-2 flex items-center gap-1.5">
              <UploadCloud className="w-3.5 h-3.5 text-primary" />
              Attach Diagnostic Evidence
            </h4>
            <form onSubmit={handleUpload} className="space-y-2.5">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[10px] text-muted block mb-1">File Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. saml_error.png"
                    value={uploadFileName}
                    onChange={(e) => setUploadFileName(e.target.value)}
                    className="w-full text-xs bg-surface-muted border border-border rounded px-2 py-1.5 text-DEFAULT focus:outline-none focus:border-primary"
                  />
                </div>
                <div>
                  <label className="text-[10px] text-muted block mb-1">Media Type</label>
                  <select
                    value={uploadMediaType}
                    onChange={(e) => setUploadMediaType(e.target.value)}
                    className="w-full text-xs bg-surface-muted border border-border rounded px-2 py-1.5 text-DEFAULT focus:outline-none focus:border-primary"
                  >
                    <option value="image/png">Image / Screenshot (PNG)</option>
                    <option value="text/plain">Log / Trace (TXT/LOG)</option>
                    <option value="text/csv">Table / Data (CSV)</option>
                    <option value="application/pdf">Document (PDF)</option>
                    <option value="video/mp4">Screen Recording (MP4)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-[10px] text-muted block mb-1">Content / Fixture Text</label>
                <textarea
                  rows={2}
                  placeholder="Paste log snippet, OCR fixture, or description..."
                  value={uploadContent}
                  onChange={(e) => setUploadContent(e.target.value)}
                  className="w-full text-xs bg-surface-muted border border-border rounded px-2 py-1.5 text-DEFAULT focus:outline-none focus:border-primary font-mono"
                />
              </div>

              {/* Consent Checkbox */}
              <label className="flex items-center gap-2 cursor-pointer text-[11px] text-DEFAULT">
                <input
                  type="checkbox"
                  checked={consentGranted}
                  onChange={(e) => setConsentGranted(e.target.checked)}
                  className="rounded bg-surface-muted border-border text-primary focus:ring-primary h-3.5 w-3.5"
                />
                <span>Allow AI to extract error codes and sanitize diagnostic evidence</span>
              </label>

              <button
                type="submit"
                disabled={isUploading || !uploadFileName.trim()}
                className="w-full py-1.5 bg-primary hover:bg-primary-hover text-white rounded text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors disabled:opacity-50"
              >
                {isUploading ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" /> Uploading & Scanning...
                  </>
                ) : (
                  <>
                    <UploadCloud className="w-3.5 h-3.5" /> Upload & Analyze
                  </>
                )}
              </button>
            </form>
          </div>
        </div>

        {/* Right Column: Evidence Inspector */}
        <div className="md:col-span-7 border border-border-subtle rounded-lg p-4 bg-surface-muted flex flex-col justify-between">
          {selectedJob ? (
            <div className="space-y-4">
              {/* Job Summary Banner */}
              <div className="flex items-center justify-between pb-3 border-b border-border-subtle">
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="font-semibold text-sm text-DEFAULT">{selectedJob.fileName}</h4>
                    {getStatusBadge(selectedJob.pipelineStatus)}
                  </div>
                  <p className="text-[11px] text-muted mt-0.5">
                    Retention: <span className="font-mono text-DEFAULT">{selectedJob.retentionClass}</span> • Job ID: <span className="font-mono text-muted">{selectedJob.id.substring(0, 8)}</span>
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleReprocess(selectedJob.id)}
                    className="px-2 py-1 text-xs bg-surface-muted hover:bg-surface-muted text-DEFAULT rounded border border-border flex items-center gap-1 transition-colors"
                    title="Reprocess evidence"
                  >
                    <RefreshCw className="w-3 h-3" /> Reprocess
                  </button>
                  <button
                    onClick={() => handleDelete(selectedJob.id)}
                    className="px-2 py-1 text-xs bg-danger/5 hover:bg-danger/10 text-danger rounded border border-danger/30 flex items-center gap-1 transition-colors"
                    title="Tombstone & delete under retention"
                  >
                    <Trash2 className="w-3 h-3" /> Tombstone
                  </button>
                </div>
              </div>

              {/* Extracted Observations (Tags & Coordinates) */}
              <div>
                <h5 className="text-[11px] font-semibold text-muted uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <Info className="w-3 h-3 text-primary" /> Extracted Observations & Coordinates
                </h5>

                {observations.length === 0 ? (
                  <p className="text-xs text-muted italic">No diagnostic observations identified yet.</p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {observations.map((obs) => {
                      const isSaml = obs.codeOrKey.includes('SAML');
                      const isTimestamp = obs.observationType === 'FAILURE_TIMESTAMP';

                      return (
                        <div
                          key={obs.id}
                          className={`p-2 rounded border text-xs flex flex-col gap-1 max-w-full ${
                            isSaml
                              ? 'bg-danger/5 border-danger/30 text-danger'
                              : isTimestamp
                              ? 'bg-primary/5 border-primary/30 text-primary'
                              : 'bg-surface-muted border-border text-DEFAULT'
                          }`}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <span className="font-mono font-bold text-xs">{obs.codeOrKey}</span>
                            <span className="text-[10px] px-1.5 py-0.2 rounded bg-surface-muted border border-border text-muted">
                              {(obs.confidence * 100).toFixed(0)}% conf
                            </span>
                          </div>
                          <p className="text-[11px] text-DEFAULT">{obs.summary}</p>
                          {obs.sourceCoordinates && (
                            <div className="text-[10px] font-mono text-muted bg-surface-muted px-1.5 py-0.5 rounded">
                              Coords: {obs.sourceCoordinates}
                            </div>
                          )}

                          {/* Video Jump to Failure Action */}
                          {isTimestamp && obs.codeOrKey === '00:42' && (
                            <button
                              onClick={() => setHighlightedTimestamp('00:42')}
                              className="mt-1 px-2 py-1 bg-primary hover:bg-primary-hover text-white font-semibold rounded text-[11px] flex items-center gap-1 self-start transition-colors"
                            >
                              <Play className="w-3 h-3 fill-white" /> Jump to failure at 00:42
                            </button>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Redacted vs Original Content Viewer */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setViewVariant('redacted')}
                      className={`px-2.5 py-1 rounded text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                        viewVariant === 'redacted'
                          ? 'bg-primary text-white'
                          : 'bg-surface-muted text-muted hover:text-DEFAULT'
                      }`}
                    >
                      <Eye className="w-3 h-3" /> Redacted Preview (Default)
                    </button>

                    {isAgent && (
                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={handleViewOriginal}
                          disabled={isRequestingOriginal}
                          className={`px-2.5 py-1 rounded text-xs font-semibold flex items-center gap-1.5 transition-colors ${
                            viewVariant === 'original'
                              ? 'bg-warning text-white'
                              : 'bg-surface-muted text-muted hover:text-DEFAULT'
                          }`}
                        >
                          {isRequestingOriginal ? (
                            <RefreshCw className="w-3 h-3 animate-spin" />
                          ) : (
                            <Lock className="w-3 h-3" />
                          )}
                          Request Original (Audited)
                        </button>
                        <input
                          type="text"
                          placeholder="Audit reason..."
                          value={originalAccessReason}
                          onChange={(e) => setOriginalAccessReason(e.target.value)}
                          className="text-[11px] bg-surface-muted border border-border rounded px-2 py-0.5 text-DEFAULT focus:outline-none focus:border-warning w-32"
                        />
                      </div>
                    )}
                  </div>

                  {highlightedTimestamp && (
                    <span className="text-xs bg-primary/10 text-primary border border-primary/20 px-2 py-0.5 rounded font-mono flex items-center gap-1">
                      <Play className="w-3 h-3" /> Position: {highlightedTimestamp}
                    </span>
                  )}
                </div>

                {originalError && (
                  <div className="p-2.5 bg-danger/10 border border-danger/20 rounded text-danger text-xs flex items-center gap-2 mb-2">
                    <ShieldAlert className="w-4 h-4 shrink-0" />
                    <span>{originalError}</span>
                  </div>
                )}

                {/* Content Display Box */}
                <div className="bg-surface-muted border border-border-subtle rounded-lg p-3 font-mono text-xs text-DEFAULT max-h-56 overflow-y-auto whitespace-pre-wrap">
                  {viewVariant === 'original' && originalContent ? (
                    <div>
                      <div className="mb-2 pb-1 border-b border-warning/30 text-[10px] text-warning uppercase font-sans font-bold flex items-center justify-between">
                        <span>UNREDACTED RAW OBJECT — AUDIT LOGGED</span>
                        <span>{user?.email}</span>
                      </div>
                      {originalContent}
                    </div>
                  ) : artifacts.length > 0 ? (
                    <div>
                      <div className="mb-2 pb-1 border-b border-border-subtle text-[10px] text-success uppercase font-sans font-bold flex items-center justify-between">
                        <span>SANITIZED DERIVATIVE ({artifacts[0].artifactType})</span>
                        <span className="text-muted font-mono text-[9px]">{artifacts[0].checksumSha256.substring(0, 12)}...</span>
                      </div>
                      {artifacts[0].redactedContent}
                    </div>
                  ) : (
                    <span className="text-muted italic">No redacted preview available yet.</span>
                  )}
                </div>
              </div>

              {/* Consent Toggle & Status Info */}
              <div className="pt-3 border-t border-border-subtle flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 text-muted">
                  <ShieldCheck className="w-4 h-4 text-success" />
                  <span>AI Consent:</span>
                  <span className={selectedJob.consentGranted ? 'text-success font-semibold' : 'text-warning font-semibold'}>
                    {selectedJob.consentGranted ? 'Granted' : 'Revoked'}
                  </span>
                </div>
                <button
                  onClick={() => handleConsentToggle(selectedJob, !selectedJob.consentGranted)}
                  className="px-2 py-1 bg-surface-muted hover:bg-surface-muted text-DEFAULT rounded border border-border text-xs transition-colors"
                >
                  {selectedJob.consentGranted ? 'Revoke AI Consent' : 'Grant AI Consent'}
                </button>
              </div>
            </div>
          ) : (
            <div className="h-64 flex flex-col items-center justify-center text-muted">
              <EyeOff className="w-10 h-10 mb-2 opacity-50" />
              <p className="text-xs">Select an evidence artifact from the left list to inspect.</p>
            </div>
          )}
              </div>
            </div>
          </div>
        </div>,
        document.body,
      )}
    </>
  );
};
