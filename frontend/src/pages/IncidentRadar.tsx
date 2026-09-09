import React, { useState, useEffect, useCallback } from 'react';
import {
  Activity,
  AlertOctagon,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Flame,
  Layers,
  Link as LinkIcon,
  Loader2,
  Plus,
  Radio,
  RefreshCw,
  Send,
  ShieldAlert,
  Trash2,
  Users,
  XCircle,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
  SupportIncident,
  IncidentCluster,
  IncidentUpdate,
  CustomerImpact,
  Role,
} from '../types';

interface IncidentRadarProps {
  role?: Role;
}

export const IncidentRadar: React.FC<IncidentRadarProps> = ({ role: _role = 'TEAM_LEAD' }) => {
  const { user } = useAuth();
  const [activeIncidents, setActiveIncidents] = useState<SupportIncident[]>([]);
  const [proposals, setProposals] = useState<IncidentCluster[]>([]);
  const [selectedIncidentId, setSelectedIncidentId] = useState<string | null>(null);
  const [incidentDetail, setIncidentDetail] = useState<{
    incident: SupportIncident;
    tickets: string[];
    updates: IncidentUpdate[];
    impacts: CustomerImpact[];
  } | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isDetecting, setIsDetecting] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // New ticket link input
  const [ticketToLink, setTicketToLink] = useState('');

  // Draft update state
  const [updateType, setUpdateType] = useState<string>('INVESTIGATING');
  const [updateSummary, setUpdateSummary] = useState('');
  const [customerFacingMessage, setCustomerFacingMessage] = useState('');

  const loadData = useCallback(async () => {
    setErrorMessage(null);
    try {
      const [incidents, proposed] = await Promise.all([
        api.listActiveIncidents(),
        api.listIncidentProposals(),
      ]);
      setActiveIncidents(incidents);
      setProposals(proposed);

      // Select first active incident if none selected
      if (!selectedIncidentId && incidents.length > 0) {
        setSelectedIncidentId(incidents[0].id);
      } else if (selectedIncidentId && !incidents.some(i => i.id === selectedIncidentId)) {
        setSelectedIncidentId(incidents.length > 0 ? incidents[0].id : null);
      }
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to load incident data');
    } finally {
      setIsLoading(false);
    }
  }, [selectedIncidentId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const loadIncidentDetail = useCallback(async (id: string) => {
    try {
      const detail = await api.getIncidentDetails(id);
      setIncidentDetail(detail);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to load incident details');
    }
  }, []);

  useEffect(() => {
    if (selectedIncidentId) {
      loadIncidentDetail(selectedIncidentId);
    } else {
      setIncidentDetail(null);
    }
  }, [selectedIncidentId, loadIncidentDetail]);

  const handleTriggerDetection = async () => {
    setIsDetecting(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const result = await api.triggerIncidentDetection(30);
      setSuccessMessage(`Radar scan completed. ${result.clustersDetected} outage clusters detected.`);
      await loadData();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Detection scan failed');
    } finally {
      setIsDetecting(false);
    }
  };

  const handleConfirmProposal = async (proposalId: string) => {
    setActionLoading(`confirm-${proposalId}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const created = await api.confirmIncidentProposal(proposalId);
      setSuccessMessage(`Incident declared: ${created.title}`);
      setSelectedIncidentId(created.id);
      await loadData();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to confirm incident');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDismissProposal = async (proposalId: string) => {
    setActionLoading(`dismiss-${proposalId}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.dismissIncidentProposal(proposalId);
      setSuccessMessage('Proposal dismissed.');
      await loadData();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to dismiss proposal');
    } finally {
      setActionLoading(null);
    }
  };

  const handleStatusChange = async (newStatus: string) => {
    if (!selectedIncidentId) return;
    setActionLoading(`status-${newStatus}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const updated = await api.updateIncidentStatus(selectedIncidentId, newStatus);
      setSuccessMessage(`Incident status updated to ${updated.status}`);
      await loadData();
      if (selectedIncidentId) {
        await loadIncidentDetail(selectedIncidentId);
      }
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to update status');
    } finally {
      setActionLoading(null);
    }
  };

  const handleLinkTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIncidentId || !ticketToLink.trim()) return;
    setActionLoading('link-ticket');
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.linkTicketToIncident(selectedIncidentId, ticketToLink.trim());
      setSuccessMessage(`Ticket linked successfully`);
      setTicketToLink('');
      await loadIncidentDetail(selectedIncidentId);
      await loadData();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to link ticket');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnlinkTicket = async (ticketId: string) => {
    if (!selectedIncidentId) return;
    setActionLoading(`unlink-${ticketId}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.unlinkTicketFromIncident(selectedIncidentId, ticketId);
      setSuccessMessage(`Ticket unlinked`);
      await loadIncidentDetail(selectedIncidentId);
      await loadData();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to unlink ticket');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDraftUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIncidentId || !updateSummary.trim() || !customerFacingMessage.trim()) return;
    setActionLoading('draft-update');
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.draftIncidentUpdate(selectedIncidentId, {
        updateType,
        summary: updateSummary.trim(),
        customerFacingMessage: customerFacingMessage.trim(),
      });
      setSuccessMessage('Incident update drafted. Pending review.');
      setUpdateSummary('');
      setCustomerFacingMessage('');
      await loadIncidentDetail(selectedIncidentId);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to draft update');
    } finally {
      setActionLoading(null);
    }
  };

  const handleApproveUpdate = async (updateId: string) => {
    if (!selectedIncidentId) return;
    setActionLoading(`approve-${updateId}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.approveIncidentUpdate(selectedIncidentId, updateId);
      setSuccessMessage('Update approved.');
      await loadIncidentDetail(selectedIncidentId);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to approve update');
    } finally {
      setActionLoading(null);
    }
  };

  const handlePublishUpdate = async (updateId: string) => {
    if (!selectedIncidentId) return;
    setActionLoading(`publish-${updateId}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.publishIncidentUpdate(selectedIncidentId, updateId);
      setSuccessMessage('Update published & fanout delivered to impacted customers.');
      await loadIncidentDetail(selectedIncidentId);
      await loadData();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to publish update');
    } finally {
      setActionLoading(null);
    }
  };

  const totalAffectedCustomers = activeIncidents.reduce((sum, inc) => sum + (inc.affectedCustomerCount || 0), 0);

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return <span className="inline-flex items-center gap-1 rounded bg-danger/15 px-2 py-0.5 text-xs font-semibold text-danger"><Flame className="h-3 w-3" />CRITICAL</span>;
      case 'HIGH':
        return <span className="inline-flex items-center gap-1 rounded bg-amber-500/15 px-2 py-0.5 text-xs font-semibold text-amber-500"><AlertTriangle className="h-3 w-3" />HIGH</span>;
      case 'MEDIUM':
        return <span className="inline-flex items-center gap-1 rounded bg-blue-500/15 px-2 py-0.5 text-xs font-semibold text-blue-500">MEDIUM</span>;
      default:
        return <span className="inline-flex items-center gap-1 rounded bg-slate-500/15 px-2 py-0.5 text-xs font-semibold text-muted">LOW</span>;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'INVESTIGATING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-danger/10 px-2.5 py-0.5 text-xs font-semibold text-danger border border-danger/20">Investigating</span>;
      case 'IDENTIFIED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-semibold text-amber-500 border border-amber-500/20">Identified</span>;
      case 'MONITORING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-semibold text-blue-500 border border-blue-500/20">Monitoring</span>;
      case 'RESOLVED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-500 border border-emerald-500/20">Resolved</span>;
      default:
        return <span className="inline-flex items-center gap-1 rounded-full bg-surface-muted px-2.5 py-0.5 text-xs font-semibold text-muted">{status}</span>;
    }
  };

  if (isLoading) {
    return (
      <div className="app-page grid min-h-[60vh] place-items-center text-sm text-muted">
        <div className="flex items-center gap-3">
          <Loader2 className="h-5 w-5 animate-spin text-primary" />
          Scanning Support Incident Radar...
        </div>
      </div>
    );
  }

  return (
    <div className="app-page max-w-7xl space-y-6">
      <header className="page-header">
        <div>
          <div className="flex items-center gap-2">
            <span className="eyebrow">Operations & Resilience</span>
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[11px] font-semibold text-emerald-500">
              <Radio className="h-3 w-3 animate-pulse" /> Live Radar
            </span>
          </div>
          <h1 className="page-title">Support Incident Radar</h1>
          <p className="page-description">
            Continuous similarity clustering, operational anomaly correlation, and proactive customer broadcast.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={loadData}
            className="btn-secondary text-xs"
            title="Refresh radar"
          >
            <RefreshCw className="h-3.5 w-3.5" /> Refresh
          </button>
          <button
            onClick={handleTriggerDetection}
            disabled={isDetecting}
            className="btn-primary text-xs"
          >
            {isDetecting ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" /> Scanning...
              </>
            ) : (
              <>
                <Radio className="h-3.5 w-3.5" /> Scan Anomaly Radar
              </>
            )}
          </button>
        </div>
      </header>

      {/* Status Alerts */}
      {errorMessage && (
        <div role="alert" className="flex items-center justify-between rounded-card border border-danger/20 bg-danger/10 p-3.5 text-xs text-danger">
          <div className="flex items-center gap-2">
            <AlertOctagon className="h-4 w-4 flex-none" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-danger hover:underline">Dismiss</button>
        </div>
      )}
      {successMessage && (
        <div role="status" className="flex items-center justify-between rounded-card border border-emerald-500/20 bg-emerald-500/10 p-3.5 text-xs text-emerald-600">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 flex-none" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="text-emerald-600 hover:underline">Dismiss</button>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-card border border-border-subtle bg-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted">Active Incidents</span>
            <Flame className="h-4 w-4 text-danger" />
          </div>
          <p className="mt-2 text-2xl font-bold text-DEFAULT">{activeIncidents.length}</p>
          <p className="mt-1 text-[11px] text-muted">Impacting ongoing operations</p>
        </div>

        <div className="rounded-card border border-border-subtle bg-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted">Proposed Clusters</span>
            <Layers className="h-4 w-4 text-amber-500" />
          </div>
          <p className="mt-2 text-2xl font-bold text-DEFAULT">{proposals.length}</p>
          <p className="mt-1 text-[11px] text-muted">Pending Team Lead review</p>
        </div>

        <div className="rounded-card border border-border-subtle bg-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted">Affected Customers</span>
            <Users className="h-4 w-4 text-primary" />
          </div>
          <p className="mt-2 text-2xl font-bold text-DEFAULT">{totalAffectedCustomers}</p>
          <p className="mt-1 text-[11px] text-muted">Receiving proactive updates</p>
        </div>

        <div className="rounded-card border border-border-subtle bg-surface p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted">Detection Algorithm</span>
            <Activity className="h-4 w-4 text-emerald-500" />
          </div>
          <p className="mt-2 text-sm font-bold text-DEFAULT">Sliding Window (30m)</p>
          <p className="mt-1 text-[11px] text-muted">RAG Cosine + Volume Spike</p>
        </div>
      </div>

      {/* Proposed Outage Clusters (If Any) */}
      {proposals.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-semibold text-DEFAULT">Proposed Incident Clusters</h2>
              <span className="rounded-full bg-amber-500/10 px-2 py-0.5 text-xs font-semibold text-amber-600">
                {proposals.length} awaiting confirmation
              </span>
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {proposals.map((prop) => (
              <div
                key={prop.id}
                className="rounded-card border border-amber-500/30 bg-amber-500/5 p-4 space-y-3"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="flex items-center gap-2">
                      {getSeverityBadge(prop.suggestedSeverity)}
                      <span className="rounded bg-surface px-2 py-0.5 text-[11px] font-medium text-muted border border-border-subtle">
                        {prop.affectedComponent}
                      </span>
                    </div>
                    <h3 className="mt-1.5 text-sm font-bold text-DEFAULT">{prop.title}</h3>
                  </div>
                  <span className="flex-none rounded-full bg-surface px-2.5 py-1 text-xs font-semibold text-DEFAULT border border-border-subtle">
                    {prop.ticketCount} tickets
                  </span>
                </div>
                <p className="text-xs text-muted leading-relaxed">{prop.summary}</p>
                <div className="text-[11px] text-muted">
                  <span className="font-semibold">Sample Tickets: </span>
                  {prop.sampleTicketIds && prop.sampleTicketIds.length > 0
                    ? prop.sampleTicketIds.slice(0, 3).join(', ')
                    : 'Correlated in active window'}
                </div>
                <div className="flex items-center justify-end gap-2 pt-2 border-t border-amber-500/20">
                  <button
                    onClick={() => handleDismissProposal(prop.id)}
                    disabled={actionLoading === `dismiss-${prop.id}`}
                    className="btn-ghost text-xs text-muted hover:text-danger"
                  >
                    {actionLoading === `dismiss-${prop.id}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <XCircle className="h-3.5 w-3.5" />}
                    Dismiss
                  </button>
                  <button
                    onClick={() => handleConfirmProposal(prop.id)}
                    disabled={actionLoading === `confirm-${prop.id}`}
                    className="btn-primary text-xs"
                  >
                    {actionLoading === `confirm-${prop.id}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <CheckCircle2 className="h-3.5 w-3.5" />}
                    Declare Incident
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Main Grid: Active Incidents List & Incident Detail */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left Column: Incidents List */}
        <div className="space-y-3 lg:col-span-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-DEFAULT">Active Outages</h2>
            <span className="text-xs text-muted">{activeIncidents.length} active</span>
          </div>

          {activeIncidents.length === 0 ? (
            <div className="rounded-card border border-dashed border-border-subtle bg-surface-muted/50 p-8 text-center">
              <ShieldAlert className="mx-auto h-8 w-8 text-muted/60" />
              <h3 className="mt-2 text-xs font-semibold text-DEFAULT">All Systems Operational</h3>
              <p className="mt-1 text-[11px] text-muted">
                No active outage incidents detected. Run a radar scan to analyze incoming tickets.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {activeIncidents.map((inc) => {
                const isSelected = inc.id === selectedIncidentId;
                return (
                  <button
                    key={inc.id}
                    onClick={() => setSelectedIncidentId(inc.id)}
                    className={`w-full text-left rounded-card p-3.5 border transition-all ${
                      isSelected
                        ? 'border-primary bg-primary/5 shadow-sm'
                        : 'border-border-subtle bg-surface hover:border-border hover:bg-surface-muted/30'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5">
                        {getSeverityBadge(inc.severity)}
                        <span className="text-[11px] font-medium text-muted">{inc.affectedComponent}</span>
                      </div>
                      {getStatusBadge(inc.status)}
                    </div>
                    <h3 className="mt-2 text-xs font-bold text-DEFAULT line-clamp-1">{inc.title}</h3>
                    <p className="mt-1 text-[11px] text-muted line-clamp-2">{inc.summary}</p>
                    <div className="mt-2.5 flex items-center justify-between text-[10px] text-muted border-t border-border-subtle/50 pt-2">
                      <span>{inc.ticketCount} linked tickets</span>
                      <span>{inc.affectedCustomerCount} customers impacted</span>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: Selected Incident Detailed Workspace */}
        <div className="space-y-6 lg:col-span-8">
          {incidentDetail ? (
            <div className="space-y-6">
              {/* Incident Header & Lifecycle Controls */}
              <div className="rounded-card border border-border-subtle bg-surface p-5 space-y-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      {getSeverityBadge(incidentDetail.incident.severity)}
                      {getStatusBadge(incidentDetail.incident.status)}
                      <span className="rounded bg-surface-muted px-2 py-0.5 text-xs font-mono font-medium text-muted">
                        {incidentDetail.incident.affectedComponent}
                      </span>
                    </div>
                    <h2 className="mt-2 text-base font-bold text-DEFAULT">{incidentDetail.incident.title}</h2>
                    <p className="mt-1 text-xs text-muted leading-relaxed">{incidentDetail.incident.summary}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-1.5 self-start">
                    <span className="text-[11px] font-medium text-muted mr-1">Transition:</span>
                    {(['INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED'] as const).map((st) => (
                      <button
                        key={st}
                        onClick={() => handleStatusChange(st)}
                        disabled={incidentDetail.incident.status === st || !!actionLoading}
                        className={`px-2 py-1 text-[10px] font-semibold rounded ${
                          incidentDetail.incident.status === st
                            ? 'bg-primary text-white'
                            : 'bg-surface-muted text-muted hover:text-DEFAULT'
                        }`}
                      >
                        {st}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Telemetry / Signal Indicator */}
                <div className="rounded-card border border-border-subtle bg-surface-muted/50 p-3 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <Activity className="h-4 w-4 text-primary" />
                    <span className="font-medium text-DEFAULT">Telemetry Signals:</span>
                    <span className="text-muted">High error rate & latency spike confirmed on {incidentDetail.incident.affectedComponent} gateway</span>
                  </div>
                  <span className="text-[10px] font-mono text-muted">Anomaly Score: 0.94</span>
                </div>
              </div>

              {/* Updates & Customer Broadcast Communication */}
              <div className="rounded-card border border-border-subtle bg-surface p-5 space-y-4">
                <div className="flex items-center justify-between border-b border-border-subtle pb-3">
                  <div className="flex items-center gap-2">
                    <Bell className="h-4 w-4 text-primary" />
                    <h3 className="text-sm font-semibold text-DEFAULT">Customer Communications</h3>
                  </div>
                  <span className="text-xs text-muted">
                    {incidentDetail.updates.length} updates recorded
                  </span>
                </div>

                {/* List of Updates */}
                {incidentDetail.updates.length > 0 && (
                  <div className="space-y-3">
                    {incidentDetail.updates.map((update) => {
                      const isHighCritical = incidentDetail.incident.severity === 'HIGH' || incidentDetail.incident.severity === 'CRITICAL';
                      const isAuthor = user?.id && update.authorId === user.id;
                      const canApprove = update.status === 'DRAFT' && (!isHighCritical || !isAuthor);

                      return (
                        <div
                          key={update.id}
                          className="rounded-card border border-border-subtle bg-surface-muted/30 p-3.5 space-y-2"
                        >
                          <div className="flex items-center justify-between text-xs">
                            <div className="flex items-center gap-2">
                              <span className="font-semibold text-DEFAULT">{update.updateType}</span>
                              <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${
                                update.status === 'PUBLISHED'
                                  ? 'bg-emerald-500/10 text-emerald-600'
                                  : update.status === 'APPROVED'
                                  ? 'bg-blue-500/10 text-blue-600'
                                  : 'bg-amber-500/10 text-amber-600'
                              }`}>
                                {update.status}
                              </span>
                            </div>
                            <span className="text-[10px] text-muted">
                              {update.publishedAt ? `Published ${new Date(update.publishedAt).toLocaleTimeString()}` : 'Not published'}
                            </span>
                          </div>

                          <p className="text-xs font-medium text-DEFAULT">{update.summary}</p>
                          <div className="rounded border border-border-subtle bg-surface p-2.5 text-xs text-muted leading-relaxed">
                            <span className="font-semibold text-[10px] uppercase text-muted block mb-1">Customer-Facing Message:</span>
                            {update.customerFacingMessage}
                          </div>

                          <div className="flex items-center justify-between text-[10px] text-muted pt-1">
                            <span>Author: {update.authorId} {update.approverId && `• Approved by: ${update.approverId}`}</span>
                            <div className="flex items-center gap-2">
                              {update.status === 'DRAFT' && (
                                <>
                                  {isHighCritical && isAuthor && (
                                    <span className="text-amber-500 font-medium">
                                      Two-person rule: A different lead must approve
                                    </span>
                                  )}
                                  <button
                                    onClick={() => handleApproveUpdate(update.id)}
                                    disabled={!canApprove || actionLoading === `approve-${update.id}`}
                                    className="btn-secondary text-[11px] py-1 px-2.5"
                                  >
                                    {actionLoading === `approve-${update.id}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <CheckCircle2 className="h-3 w-3" />}
                                    Approve
                                  </button>
                                </>
                              )}
                              {update.status === 'APPROVED' && (
                                <button
                                  onClick={() => handlePublishUpdate(update.id)}
                                  disabled={actionLoading === `publish-${update.id}`}
                                  className="btn-primary text-[11px] py-1 px-2.5"
                                >
                                  {actionLoading === `publish-${update.id}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <Send className="h-3 w-3" />}
                                  Publish & Broadcast
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Draft Update Form */}
                <form onSubmit={handleDraftUpdate} className="space-y-3 rounded-card border border-border-subtle bg-surface p-3.5">
                  <h4 className="text-xs font-semibold text-DEFAULT">Draft Incident Broadcast</h4>
                  <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                    <div>
                      <label className="block text-[10px] font-medium text-muted uppercase">Update Type</label>
                      <select
                        value={updateType}
                        onChange={(e) => setUpdateType(e.target.value)}
                        className="mt-1 w-full rounded border border-border-subtle bg-surface px-2.5 py-1.5 text-xs text-DEFAULT"
                      >
                        <option value="INVESTIGATING">INVESTIGATING</option>
                        <option value="IDENTIFIED">IDENTIFIED</option>
                        <option value="MONITORING">MONITORING</option>
                        <option value="RESOLVED">RESOLVED</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-[10px] font-medium text-muted uppercase">Internal Summary</label>
                      <input
                        type="text"
                        placeholder="e.g., Engineering identified DB lock contention"
                        value={updateSummary}
                        onChange={(e) => setUpdateSummary(e.target.value)}
                        className="mt-1 w-full rounded border border-border-subtle bg-surface px-2.5 py-1.5 text-xs text-DEFAULT"
                        required
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-[10px] font-medium text-muted uppercase">Customer Facing Message</label>
                    <textarea
                      rows={3}
                      placeholder="e.g., We are currently experiencing elevated latency on billing operations. Our engineering team is actively investigating..."
                      value={customerFacingMessage}
                      onChange={(e) => setCustomerFacingMessage(e.target.value)}
                      className="mt-1 w-full rounded border border-border-subtle bg-surface px-2.5 py-1.5 text-xs text-DEFAULT"
                      required
                    />
                  </div>
                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={actionLoading === 'draft-update'}
                      className="btn-secondary text-xs"
                    >
                      {actionLoading === 'draft-update' ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Plus className="h-3.5 w-3.5" />}
                      Draft Update
                    </button>
                  </div>
                </form>
              </div>

              {/* Linked Tickets & Impacted Customers Split */}
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                {/* Linked Tickets */}
                <div className="rounded-card border border-border-subtle bg-surface p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <LinkIcon className="h-4 w-4 text-primary" />
                      <h3 className="text-xs font-semibold text-DEFAULT">Linked Tickets ({incidentDetail.tickets.length})</h3>
                    </div>
                  </div>

                  <form onSubmit={handleLinkTicket} className="flex gap-2">
                    <input
                      type="text"
                      placeholder="Ticket UUID to link"
                      value={ticketToLink}
                      onChange={(e) => setTicketToLink(e.target.value)}
                      className="flex-1 rounded border border-border-subtle bg-surface px-2.5 py-1 text-xs text-DEFAULT"
                    />
                    <button
                      type="submit"
                      disabled={!ticketToLink.trim() || actionLoading === 'link-ticket'}
                      className="btn-primary text-xs py-1 px-2.5"
                    >
                      Link
                    </button>
                  </form>

                  <div className="max-h-48 overflow-y-auto space-y-1.5">
                    {incidentDetail.tickets.map((tId) => (
                      <div
                        key={tId}
                        className="flex items-center justify-between rounded bg-surface-muted/40 px-2.5 py-1.5 text-xs"
                      >
                        <span className="font-mono text-[11px] text-DEFAULT truncate max-w-[200px]">{tId}</span>
                        <button
                          onClick={() => handleUnlinkTicket(tId)}
                          disabled={actionLoading === `unlink-${tId}`}
                          className="text-muted hover:text-danger"
                          title="Unlink ticket"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Impacted Customers */}
                <div className="rounded-card border border-border-subtle bg-surface p-4 space-y-3">
                  <div className="flex items-center gap-1.5">
                    <Users className="h-4 w-4 text-primary" />
                    <h3 className="text-xs font-semibold text-DEFAULT">Impacted Customers ({incidentDetail.impacts.length})</h3>
                  </div>

                  <div className="max-h-56 overflow-y-auto space-y-1.5">
                    {incidentDetail.impacts.length === 0 ? (
                      <p className="text-xs text-muted text-center py-4">No customers linked yet.</p>
                    ) : (
                      incidentDetail.impacts.map((imp) => (
                        <div
                          key={imp.id}
                          className="rounded bg-surface-muted/40 p-2 text-xs space-y-0.5"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-medium text-DEFAULT">{imp.customerName || 'Customer'}</span>
                            <span className="text-[10px] text-muted">{imp.customerEmail}</span>
                          </div>
                          <p className="text-[10px] font-mono text-muted truncate">Ticket: {imp.linkedTicketId}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-card border border-border-subtle bg-surface p-12 text-center text-muted">
              Select an incident from the left to view operational signals, manage linked tickets, and broadcast customer updates.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default IncidentRadar;
