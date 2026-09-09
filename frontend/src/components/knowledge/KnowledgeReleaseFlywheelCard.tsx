import React, { useCallback, useEffect, useState } from 'react';
import {
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Play,
  Lock,
  Loader2,
  TrendingUp,
  Award,
  Users,
  Check,
  Plus,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { api } from '../../api/client';
import {
  KnowledgeCandidateResponse,
  EvaluationRunResponse,
  ResolutionMetricsResponse,
  Role,
} from '../../types';

interface Props {
  role?: Role;
}

export const KnowledgeReleaseFlywheelCard: React.FC<Props> = ({ role = 'KNOWLEDGE_MANAGER' }) => {
  const isApprover = role === 'ADMIN' || role === 'TEAM_LEAD' || role === 'KNOWLEDGE_MANAGER';

  const [metrics, setMetrics] = useState<ResolutionMetricsResponse | null>(null);
  const [candidates, setCandidates] = useState<KnowledgeCandidateResponse[]>([]);
  const [evalRuns, setEvalRuns] = useState<Record<string, EvaluationRunResponse>>({});
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // New Candidate Form State
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newCategory, setNewCategory] = useState('AUTHENTICATION');
  const [newContentDraft, setNewContentDraft] = useState('');
  const [newScore, setNewScore] = useState(90);
  const [newCustomerCount, setNewCustomerCount] = useState(6);

  // Rollback Modal State
  const [rollbackCandidateId, setRollbackCandidateId] = useState<string | null>(null);
  const [rollbackReason, setRollbackReason] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      const [m, cList] = await Promise.all([
        api.getResolutionMetrics().catch(() => null),
        api.listKnowledgeCandidates().catch(() => []),
      ]);
      if (m) setMetrics(m);
      setCandidates(cList);
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to load flywheel data', type: 'error' });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSanitize = async (candidateId: string) => {
    setActionLoading(`sanitize-${candidateId}`);
    try {
      const updated = await api.sanitizeKnowledgeCandidate(candidateId);
      setCandidates(prev => prev.map(c => (c.id === candidateId ? updated : c)));
      setMessage({ text: 'Candidate successfully sanitized! PII and secrets scrubbed.', type: 'success' });
    } catch (err: any) {
      setMessage({ text: err.message || 'Sanitization failed', type: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleRunEvaluation = async (candidateId: string) => {
    setActionLoading(`eval-${candidateId}`);
    try {
      const run = await api.runKnowledgeEvaluation(candidateId);
      setEvalRuns(prev => ({ ...prev, [candidateId]: run }));
      await loadData();
      if (run.gatePassed) {
        setMessage({ text: 'Holdout benchmark PASSED! Candidate is approved for release.', type: 'success' });
      } else {
        setMessage({ text: 'Holdout benchmark did not meet thresholds.', type: 'error' });
      }
    } catch (err: any) {
      setMessage({ text: err.message || 'Evaluation run failed', type: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleRelease = async (candidateId: string) => {
    setActionLoading(`release-${candidateId}`);
    try {
      await api.releaseKnowledgeCandidate(candidateId);
      await loadData();
      setMessage({ text: 'Knowledge release successfully published and activated!', type: 'success' });
    } catch (err: any) {
      setMessage({ text: err.message || 'Release failed', type: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleRollback = async (candidateId: string) => {
    if (!rollbackReason.trim()) return;
    setActionLoading(`rollback-${candidateId}`);
    try {
      await api.rollbackKnowledgeRelease(candidateId, { reason: rollbackReason.trim() });
      await loadData();
      setRollbackCandidateId(null);
      setRollbackReason('');
      setMessage({ text: 'Knowledge release successfully rolled back.', type: 'success' });
    } catch (err: any) {
      setMessage({ text: err.message || 'Rollback failed', type: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  const handleCreateCandidate = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading('create');
    try {
      const created = await api.createKnowledgeCandidate({
        title: newTitle.trim(),
        category: newCategory,
        contentDraft: newContentDraft.trim(),
        verifiedOutcomeScore: Number(newScore),
        distinctSourceCustomers: Number(newCustomerCount),
      });
      setCandidates(prev => [created, ...prev]);
      setShowCreateForm(false);
      setNewTitle('');
      setNewContentDraft('');
      setMessage({ text: 'Knowledge candidate created successfully from verified resolutions.', type: 'success' });
    } catch (err: any) {
      setMessage({ text: err.message || 'Failed to create candidate', type: 'error' });
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-6 w-6 text-primary" />
            <h2 className="text-xl font-bold tracking-tight text-DEFAULT">Verified Knowledge Release Flywheel</h2>
          </div>
          <p className="text-xs text-muted mt-1">
            Section 22.7 automated quality gate: requires ≥5 verified customers, ≥80 resolution score, PII scrubbing, and frozen benchmark pass before promotion.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={loadData}
            disabled={loading}
            className="btn-secondary min-h-9 px-3 text-xs"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
          <button
            type="button"
            onClick={() => setShowCreateForm(true)}
            className="btn-primary min-h-9 px-3 text-xs"
          >
            <Plus className="h-3.5 w-3.5" />
            <span>Propose Candidate</span>
          </button>
        </div>
      </div>

      {/* Alert Messages */}
      {message && (
        <div
          className={`rounded-card border p-3.5 text-xs flex items-center justify-between ${
            message.type === 'success'
              ? 'border-success/30 bg-success/10 text-success'
              : 'border-danger/30 bg-danger/10 text-danger'
          }`}
        >
          <div className="flex items-center gap-2">
            {message.type === 'success' ? (
              <CheckCircle2 className="h-4 w-4 flex-none" />
            ) : (
              <AlertTriangle className="h-4 w-4 flex-none" />
            )}
            <span>{message.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setMessage(null)}
            className="text-xs font-semibold hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Resolution Flywheel Governance Metrics */}
      {metrics && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="panel p-4 space-y-1">
            <div className="flex items-center justify-between text-xs text-muted">
              <span>Verified Success Rate</span>
              <Award className="h-4 w-4 text-success" />
            </div>
            <p className="text-2xl font-bold text-DEFAULT">
              {(metrics.verifiedSuccessRate * 100).toFixed(1)}%
            </p>
            <p className="text-[10px] text-muted">Confirmed positive resolutions</p>
          </div>

          <div className="panel p-4 space-y-1">
            <div className="flex items-center justify-between text-xs text-muted">
              <span>Feedback Coverage</span>
              <Users className="h-4 w-4 text-primary" />
            </div>
            <p className="text-2xl font-bold text-DEFAULT">
              {(metrics.feedbackCoverage * 100).toFixed(1)}%
            </p>
            <p className="text-[10px] text-muted">
              {metrics.respondedAttempts} / {metrics.eligibleAttempts} response rate
            </p>
          </div>

          <div className="panel p-4 space-y-1">
            <div className="flex items-center justify-between text-xs text-muted">
              <span>First Contact (FCR)</span>
              <Check className="h-4 w-4 text-info" />
            </div>
            <p className="text-2xl font-bold text-DEFAULT">
              {(metrics.firstContactResolutionRate * 100).toFixed(1)}%
            </p>
            <p className="text-[10px] text-muted">No repeat within 7 days</p>
          </div>

          <div className="panel p-4 space-y-1">
            <div className="flex items-center justify-between text-xs text-muted">
              <span>Avg Resolution Score</span>
              <TrendingUp className="h-4 w-4 text-ai" />
            </div>
            <p className="text-2xl font-bold text-DEFAULT">
              {metrics.averageScore} / 100
            </p>
            <p className="text-[10px] text-muted">Deterministic scoring formula</p>
          </div>
        </div>
      )}

      {/* Propose Candidate Modal/Form */}
      {showCreateForm && (
        <form onSubmit={handleCreateCandidate} className="panel p-5 border-primary/30 space-y-4">
          <div className="flex items-center justify-between border-b border-border-subtle pb-3">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary" />
              <h3 className="text-sm font-bold text-DEFAULT">Propose Knowledge Candidate from Verified Solutions</h3>
            </div>
            <button
              type="button"
              onClick={() => setShowCreateForm(false)}
              className="text-xs text-muted hover:text-DEFAULT"
            >
              Cancel
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="field-label">Candidate Title *</label>
              <input
                type="text"
                required
                value={newTitle}
                onChange={e => setNewTitle(e.target.value)}
                placeholder="e.g., Okta SAML 2.0 InResponseTo Mismatch Resolution"
                className="form-control text-xs h-9"
              />
            </div>
            <div>
              <label className="field-label">Category</label>
              <select
                value={newCategory}
                onChange={e => setNewCategory(e.target.value)}
                className="form-control text-xs h-9"
              >
                <option value="AUTHENTICATION">Authentication & SSO</option>
                <option value="BILLING">Billing & Invoicing</option>
                <option value="TECHNICAL">Technical Infrastructure</option>
                <option value="GENERAL">General Operational</option>
              </select>
            </div>
          </div>

          <div>
            <label className="field-label">Solution Content Draft (can contain raw evidence) *</label>
            <textarea
              required
              rows={4}
              value={newContentDraft}
              onChange={e => setNewContentDraft(e.target.value)}
              placeholder="Verified troubleshooting steps, error codes, and workarounds. Secrets and PII will be sanitized before release."
              className="form-control text-xs p-3"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="field-label">Verified Outcome Score (Threshold ≥ 80)</label>
              <input
                type="number"
                min="0"
                max="100"
                value={newScore}
                onChange={e => setNewScore(Number(e.target.value))}
                className="form-control text-xs h-9"
              />
            </div>
            <div>
              <label className="field-label">Distinct Source Customers (Threshold ≥ 5)</label>
              <input
                type="number"
                min="1"
                max="100"
                value={newCustomerCount}
                onChange={e => setNewCustomerCount(Number(e.target.value))}
                className="form-control text-xs h-9"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-border-subtle">
            <button
              type="button"
              onClick={() => setShowCreateForm(false)}
              className="btn-secondary min-h-8 text-xs px-3"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={actionLoading === 'create'}
              className="btn-primary min-h-8 text-xs px-4"
            >
              {actionLoading === 'create' ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Check className="h-3.5 w-3.5" />}
              <span>Save Candidate</span>
            </button>
          </div>
        </form>
      )}

      {/* Candidate Queue */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="section-title">Knowledge Candidates Quality Queue</h3>
          <span className="text-xs text-muted">{candidates.length} candidates in queue</span>
        </div>

        {loading && candidates.length === 0 ? (
          <div className="panel p-8 text-center text-xs text-muted">
            <Loader2 className="h-5 w-5 animate-spin mx-auto mb-2 text-primary" />
            Loading knowledge candidates…
          </div>
        ) : candidates.length === 0 ? (
          <div className="panel p-8 text-center text-xs text-muted space-y-2">
            <ShieldCheck className="h-8 w-8 mx-auto text-muted/50" />
            <p className="font-semibold text-DEFAULT">No knowledge candidates in queue</p>
            <p>Propose a candidate from verified resolutions above to trigger the promotion flywheel.</p>
          </div>
        ) : (
          candidates.map(candidate => {
            const isEligible = candidate.verifiedOutcomeScore >= 80 && candidate.distinctSourceCustomers >= 5;
            const isSanitized = candidate.sanitizationStatus === 'SANITIZED';
            const run = evalRuns[candidate.id];

            return (
              <div
                key={candidate.id}
                className="panel p-5 space-y-4 border transition-[border-color,box-shadow] hover:border-primary/40"
              >
                {/* Candidate Header */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-border-subtle pb-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm text-DEFAULT">{candidate.title}</span>
                      <span className="rounded-full bg-surface-muted px-2 py-0.5 text-[10px] font-medium text-muted">
                        {candidate.category}
                      </span>
                    </div>
                    <p className="text-[11px] text-muted">
                      Source score: <span className="font-semibold text-DEFAULT">{candidate.verifiedOutcomeScore}/100</span> · Distinct customers: <span className="font-semibold text-DEFAULT">{candidate.distinctSourceCustomers}</span>
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span
                      className={`status-chip ${
                        candidate.eligibilityStatus === 'RELEASED'
                          ? 'bg-success/10 text-success border-success/20'
                          : candidate.eligibilityStatus === 'APPROVED'
                          ? 'bg-primary/10 text-primary border-primary/20'
                          : candidate.eligibilityStatus === 'ELIGIBLE'
                          ? 'bg-info/10 text-info border-info/20'
                          : 'bg-surface-muted text-muted border-border'
                      }`}
                    >
                      {candidate.eligibilityStatus}
                    </span>
                    <span
                      className={`status-chip ${
                        isSanitized
                          ? 'bg-success/10 text-success border-success/20'
                          : 'bg-warning/10 text-warning border-warning/20'
                      }`}
                    >
                      {candidate.sanitizationStatus}
                    </span>
                  </div>
                </div>

                {/* Content Preview */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                  <div className="rounded-card border border-border-subtle bg-surface-muted p-3 space-y-1">
                    <span className="text-[10px] font-semibold text-muted uppercase">Raw Draft</span>
                    <p className="text-muted leading-relaxed line-clamp-3 whitespace-pre-wrap">{candidate.contentDraft}</p>
                  </div>
                  <div className="rounded-card border border-border-subtle bg-surface-muted p-3 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-semibold text-muted uppercase">Sanitized Content</span>
                      {candidate.contentHash && (
                        <span className="font-mono text-[9px] text-muted">SHA256: {candidate.contentHash.slice(0, 12)}…</span>
                      )}
                    </div>
                    <p className="text-DEFAULT leading-relaxed line-clamp-3 whitespace-pre-wrap">
                      {candidate.sanitizedContent || 'Pending privacy and secret sanitization.'}
                    </p>
                  </div>
                </div>

                {/* Evaluation Benchmark Details if executed */}
                {run && (
                  <div className="rounded-card border border-primary/20 bg-primary/5 p-3 space-y-2 text-xs">
                    <div className="flex items-center justify-between font-semibold text-DEFAULT">
                      <span className="flex items-center gap-1.5">
                        <CheckCircle2 className="h-4 w-4 text-success" />
                        <span>Frozen Evaluation Benchmark Results ({run.datasetVersion})</span>
                      </span>
                      <span className={`status-chip ${run.gatePassed ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'}`}>
                        Gate: {run.gatePassed ? 'PASSED' : 'FAILED'}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px]">
                      <div>
                        <span className="text-muted">Recall@5:</span>{' '}
                        <span className="font-semibold text-DEFAULT">
                          {(run.proposedRecallAt5 * 100).toFixed(1)}% (Base: {(run.baselineRecallAt5 * 100).toFixed(1)}%)
                        </span>
                      </div>
                      <div>
                        <span className="text-muted">MRR:</span>{' '}
                        <span className="font-semibold text-DEFAULT">
                          {(run.proposedMrr * 100).toFixed(1)}% (Base: {(run.baselineMrr * 100).toFixed(1)}%)
                        </span>
                      </div>
                      <div>
                        <span className="text-muted">Safety Cases:</span>{' '}
                        <span className={`font-semibold ${run.safetyCasesPassed ? 'text-success' : 'text-danger'}`}>
                          {run.safetyCasesPassed ? 'PASSED' : 'FAILED'}
                        </span>
                      </div>
                      <div>
                        <span className="text-muted">P95 Latency:</span>{' '}
                        <span className="font-semibold text-DEFAULT">{run.latencyP95Ratio}x baseline</span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Gate Execution Actions */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-border-subtle">
                  <div className="flex items-center gap-2 text-xs text-muted">
                    {!isEligible && (
                      <span className="text-warning flex items-center gap-1">
                        <AlertTriangle className="h-3.5 w-3.5" />
                        Requires ≥5 source customers and ≥80 score to qualify.
                      </span>
                    )}
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    {/* 1. Sanitize Step */}
                    {!isSanitized && (
                      <button
                        type="button"
                        disabled={actionLoading === `sanitize-${candidate.id}`}
                        onClick={() => handleSanitize(candidate.id)}
                        className="btn-secondary min-h-8 text-xs px-3"
                      >
                        {actionLoading === `sanitize-${candidate.id}` ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : (
                          <Lock className="h-3.5 w-3.5" />
                        )}
                        <span>Sanitize PII & Secrets</span>
                      </button>
                    )}

                    {/* 2. Benchmark Evaluation Step */}
                    {isSanitized && candidate.eligibilityStatus !== 'APPROVED' && candidate.eligibilityStatus !== 'RELEASED' && (
                      <button
                        type="button"
                        disabled={actionLoading === `eval-${candidate.id}`}
                        onClick={() => handleRunEvaluation(candidate.id)}
                        className="btn-secondary min-h-8 text-xs px-3"
                      >
                        {actionLoading === `eval-${candidate.id}` ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : (
                          <Play className="h-3.5 w-3.5" />
                        )}
                        <span>Run Evaluation Gate</span>
                      </button>
                    )}

                    {/* 3. Release Step (Requires Approver Role & Passing Evaluation) */}
                    {candidate.eligibilityStatus === 'APPROVED' && isApprover && (
                      <button
                        type="button"
                        disabled={actionLoading === `release-${candidate.id}`}
                        onClick={() => handleRelease(candidate.id)}
                        className="btn-primary min-h-8 text-xs px-3 bg-success hover:bg-success/90"
                      >
                        {actionLoading === `release-${candidate.id}` ? (
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        ) : (
                          <CheckCircle2 className="h-3.5 w-3.5" />
                        )}
                        <span>Approve & Release</span>
                      </button>
                    )}

                    {/* 4. Rollback Step */}
                    {candidate.eligibilityStatus === 'RELEASED' && isApprover && (
                      <button
                        type="button"
                        onClick={() => setRollbackCandidateId(candidate.id)}
                        className="btn-ghost min-h-8 text-xs px-2 text-danger hover:bg-danger/10"
                      >
                        <RotateCcw className="h-3.5 w-3.5" />
                        <span>Rollback Release</span>
                      </button>
                    )}
                  </div>
                </div>

                {/* Rollback Prompt if selected */}
                {rollbackCandidateId === candidate.id && (
                  <div className="rounded-card border border-danger/30 bg-danger/5 p-3 space-y-2 text-xs">
                    <label className="font-semibold text-danger">Rollback Reason *</label>
                    <input
                      type="text"
                      value={rollbackReason}
                      onChange={e => setRollbackReason(e.target.value)}
                      placeholder="Specify rationale for rollback (e.g. regression in holdout metrics or customer complaints)..."
                      className="form-control text-xs h-8"
                    />
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setRollbackCandidateId(null);
                          setRollbackReason('');
                        }}
                        className="btn-secondary min-h-7 text-[11px] px-2"
                      >
                        Cancel
                      </button>
                      <button
                        type="button"
                        disabled={!rollbackReason.trim() || actionLoading === `rollback-${candidate.id}`}
                        onClick={() => handleRollback(candidate.id)}
                        className="btn-primary min-h-7 text-[11px] px-3 bg-danger hover:bg-danger/90"
                      >
                        Confirm Rollback
                      </button>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
