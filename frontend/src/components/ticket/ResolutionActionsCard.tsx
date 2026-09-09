import React, { useState, useEffect, useCallback } from 'react';
import {
  Loader2,
  RefreshCw,
  RotateCcw,
  Send,
  ShieldAlert,
  ShieldCheck,
  XCircle,
  CreditCard,
  KeyRound,
} from 'lucide-react';
import { api } from '../../api/client';
import { ActionProposalResponse, ActionExecutionResponse } from '../../types';

interface Props {
  ticketId: string;
  readOnly?: boolean;
}

export const ResolutionActionsCard: React.FC<Props> = ({ ticketId, readOnly = false }) => {
  const [proposals, setProposals] = useState<ActionProposalResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Propose Modal / Form State
  const [showModal, setShowModal] = useState<'REFUND' | 'UNLOCK' | null>(null);
  // Refund fields
  const [customerAccountId, setCustomerAccountId] = useState('');
  const [paymentReference, setPaymentReference] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [amountCents, setAmountCents] = useState('4000');
  const [duplicateOfReference, setDuplicateOfReference] = useState('');
  const [rationale, setRationale] = useState('');
  // Unlock fields
  const [userId, setUserId] = useState('');
  const [unlockReason, setUnlockReason] = useState('Identity verified via challenge');
  const [revokeSessions, setRevokeSessions] = useState(true);

  const loadProposals = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const list = await api.listTicketActions(ticketId);
      setProposals(list);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to load actions');
    } finally {
      setIsLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    loadProposals();
  }, [loadProposals]);

  const handleProposeRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading('proposing');
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.proposeResolutionAction(ticketId, {
        actionType: 'REFUND_DUPLICATE_CHARGE',
        input: {
          customerAccountId: customerAccountId.trim(),
          paymentReference: paymentReference.trim(),
          currency: currency.toUpperCase().trim(),
          amountCents: parseInt(amountCents, 10),
          reasonCode: 'DUPLICATE_TRANSACTION',
          duplicateOfReference: duplicateOfReference.trim(),
        },
        aiRationale: rationale.trim() || 'Verified duplicate billing charge during support interaction',
      });
      setSuccessMessage('Refund action proposed successfully. Awaiting policy approval.');
      setShowModal(null);
      await loadProposals();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to propose refund');
    } finally {
      setActionLoading(null);
    }
  };

  const handleProposeUnlock = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading('proposing');
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.proposeResolutionAction(ticketId, {
        actionType: 'UNLOCK_ACCOUNT',
        input: {
          userId: userId.trim(),
          unlockReason: unlockReason.trim(),
          revokeSessions,
        },
        aiRationale: rationale.trim() || 'Verified account recovery challenge completed',
      });
      setSuccessMessage('Account unlock proposed successfully. Awaiting policy approval.');
      setShowModal(null);
      await loadProposals();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to propose unlock');
    } finally {
      setActionLoading(null);
    }
  };

  const handleApprove = async (prop: ActionProposalResponse) => {
    setActionLoading(`approve-${prop.id}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.approveAction(prop.id, {
        approvedDigest: prop.canonicalDigest,
        comment: 'Approved in ticket workspace with valid human verification',
      });
      setSuccessMessage('Action proposal approved with verified canonical digest.');
      await loadProposals();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Approval failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (prop: ActionProposalResponse) => {
    setActionLoading(`reject-${prop.id}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.rejectAction(prop.id, { reason: 'Rejected by support agent after review' });
      setSuccessMessage('Action proposal rejected.');
      await loadProposals();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Rejection failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleExecute = async (prop: ActionProposalResponse) => {
    setActionLoading(`execute-${prop.id}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const result: ActionExecutionResponse = await api.executeAction(prop.id, {
        approvedDigest: prop.canonicalDigest,
        expectedVersion: prop.version,
      });
      if (result.status === 'SUCCEEDED') {
        setSuccessMessage(`Action executed & reconciled! Provider Ref: ${result.providerReference || 'confirmed'}`);
      } else {
        setErrorMessage(`Execution outcome: ${result.status}. ${result.errorMessage || ''}`);
      }
      await loadProposals();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Execution failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleCompensate = async (prop: ActionProposalResponse) => {
    setActionLoading(`compensate-${prop.id}`);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const res = await api.compensateAction(prop.id);
      setErrorMessage(`${res.status}: ${res.reason}`);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Compensation check failed');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="space-y-3 rounded-card border border-border-subtle bg-surface-muted/30 p-4">
      <div className="flex items-center justify-between border-b border-border-subtle pb-2.5">
        <div className="flex items-center gap-2">
          <ShieldAlert className="h-4 w-4 text-primary" />
          <span className="text-xs font-bold text-DEFAULT">Resolution Actions</span>
        </div>
        <div className="flex items-center gap-1.5">
          <button
            onClick={loadProposals}
            disabled={isLoading}
            className="text-muted hover:text-DEFAULT"
            title="Refresh actions"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          {!readOnly && (
            <div className="flex items-center gap-1">
              <button
                onClick={() => setShowModal('REFUND')}
                className="btn-secondary py-1 px-2 text-[10px] flex items-center gap-1"
                title="Propose duplicate refund"
              >
                <CreditCard className="h-3 w-3" /> Refund
              </button>
              <button
                onClick={() => setShowModal('UNLOCK')}
                className="btn-secondary py-1 px-2 text-[10px] flex items-center gap-1"
                title="Propose account unlock"
              >
                <KeyRound className="h-3 w-3" /> Unlock
              </button>
            </div>
          )}
        </div>
      </div>

      {errorMessage && (
        <div className="rounded bg-danger/10 border border-danger/20 p-2 text-[11px] text-danger flex items-center justify-between">
          <span>{errorMessage}</span>
          <button onClick={() => setErrorMessage(null)} className="font-bold ml-2">×</button>
        </div>
      )}
      {successMessage && (
        <div className="rounded bg-emerald-500/10 border border-emerald-500/20 p-2 text-[11px] text-emerald-600 flex items-center justify-between">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="font-bold ml-2">×</button>
        </div>
      )}

      {/* Propose Refund Modal */}
      {showModal === 'REFUND' && (
        <form onSubmit={handleProposeRefund} className="rounded-card border border-primary/30 bg-surface p-3 space-y-2.5 text-xs shadow-md">
          <div className="flex items-center justify-between font-semibold text-DEFAULT">
            <span className="flex items-center gap-1.5"><CreditCard className="h-3.5 w-3.5 text-primary" /> Propose Duplicate Refund</span>
            <button type="button" onClick={() => setShowModal(null)} className="text-muted hover:text-DEFAULT">×</button>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-muted uppercase">Customer Account ID</label>
              <input
                type="text"
                placeholder="cust_123"
                value={customerAccountId}
                onChange={(e) => setCustomerAccountId(e.target.value)}
                className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
                required
              />
            </div>
            <div>
              <label className="text-[10px] text-muted uppercase">Duplicate Payment Ref</label>
              <input
                type="text"
                placeholder="pay_dup_001"
                value={paymentReference}
                onChange={(e) => setPaymentReference(e.target.value)}
                className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-muted uppercase">Original Payment Ref</label>
              <input
                type="text"
                placeholder="pay_orig_001"
                value={duplicateOfReference}
                onChange={(e) => setDuplicateOfReference(e.target.value)}
                className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
                required
              />
            </div>
            <div>
              <label className="text-[10px] text-muted uppercase">Refund Amount (cents)</label>
              <input
                type="number"
                placeholder="4000 ($40.00)"
                value={amountCents}
                onChange={(e) => setAmountCents(e.target.value)}
                className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div>
              <label className="text-[10px] text-muted uppercase">Currency</label>
              <input
                type="text"
                value={currency}
                onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
                maxLength={3}
                required
              />
            </div>
            <div className="col-span-2">
              <label className="text-[10px] text-muted uppercase">AI Rationale / Reason</label>
              <input
                type="text"
                placeholder="e.g. Duplicate charge identified on statement"
                value={rationale}
                onChange={(e) => setRationale(e.target.value)}
                className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
              />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={() => setShowModal(null)} className="btn-ghost py-1 px-2.5 text-xs">Cancel</button>
            <button type="submit" disabled={actionLoading === 'proposing'} className="btn-primary py-1 px-3 text-xs">
              {actionLoading === 'proposing' ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Propose Action'}
            </button>
          </div>
        </form>
      )}

      {/* Propose Unlock Modal */}
      {showModal === 'UNLOCK' && (
        <form onSubmit={handleProposeUnlock} className="rounded-card border border-primary/30 bg-surface p-3 space-y-2.5 text-xs shadow-md">
          <div className="flex items-center justify-between font-semibold text-DEFAULT">
            <span className="flex items-center gap-1.5"><KeyRound className="h-3.5 w-3.5 text-primary" /> Propose Account Unlock</span>
            <button type="button" onClick={() => setShowModal(null)} className="text-muted hover:text-DEFAULT">×</button>
          </div>
          <div>
            <label className="text-[10px] text-muted uppercase">User ID (UUID)</label>
            <input
              type="text"
              placeholder="00000000-0000-0000-0000-000000000002"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
              required
            />
          </div>
          <div>
            <label className="text-[10px] text-muted uppercase">Unlock Reason</label>
            <input
              type="text"
              value={unlockReason}
              onChange={(e) => setUnlockReason(e.target.value)}
              className="mt-0.5 w-full rounded border border-border bg-surface px-2 py-1 text-xs"
              required
            />
          </div>
          <label className="flex items-center gap-2 text-[11px] text-DEFAULT cursor-pointer">
            <input
              type="checkbox"
              checked={revokeSessions}
              onChange={(e) => setRevokeSessions(e.target.checked)}
              className="rounded"
            />
            Revoke all active sessions on unlock
          </label>
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={() => setShowModal(null)} className="btn-ghost py-1 px-2.5 text-xs">Cancel</button>
            <button type="submit" disabled={actionLoading === 'proposing'} className="btn-primary py-1 px-3 text-xs">
              {actionLoading === 'proposing' ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Propose Action'}
            </button>
          </div>
        </form>
      )}

      {/* Proposals List */}
      {proposals.length === 0 ? (
        <p className="text-[11px] text-muted text-center py-3">
          No policy-controlled actions proposed for this ticket.
        </p>
      ) : (
        <div className="space-y-3">
          {proposals.map((prop) => {
            const isAwaitingApproval = prop.status === 'AWAITING_APPROVAL' || prop.status === 'PROPOSED';
            const isApproved = prop.status === 'APPROVED';
            const isCompleted = prop.status === 'RECONCILED' || prop.status === 'SUCCEEDED';

            return (
              <div
                key={prop.id}
                className="rounded-card border border-border-subtle bg-surface p-3 space-y-2 text-xs"
              >
                <div className="flex items-start justify-between gap-1">
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="font-bold text-DEFAULT">
                        {prop.actionType === 'REFUND_DUPLICATE_CHARGE' ? 'Refund Duplicate Charge' : 'Unlock Account'}
                      </span>
                      <span className={`px-1.5 py-0.5 rounded text-[9px] font-semibold ${
                        prop.riskLevel === 'HIGH' || prop.riskLevel === 'CRITICAL'
                          ? 'bg-amber-500/15 text-amber-600'
                          : 'bg-blue-500/15 text-blue-600'
                      }`}>
                        {prop.riskLevel}
                      </span>
                    </div>
                    <span className="font-mono text-[10px] text-muted truncate block max-w-[240px]">
                      Digest: {prop.canonicalDigest.substring(0, 16)}…
                    </span>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                    isCompleted
                      ? 'bg-emerald-500/15 text-emerald-600 border border-emerald-500/30'
                      : isApproved
                      ? 'bg-blue-500/15 text-blue-600 border border-blue-500/30'
                      : prop.status === 'REJECTED' || prop.status === 'POLICY_DENIED'
                      ? 'bg-danger/15 text-danger border border-danger/30'
                      : 'bg-amber-500/15 text-amber-600 border border-amber-500/30'
                  }`}>
                    {prop.status}
                  </span>
                </div>

                {/* Normalized Typed Values (Confirmation Source) */}
                <div className="rounded bg-surface-muted/60 p-2 text-[11px] space-y-1 font-mono">
                  {prop.actionType === 'REFUND_DUPLICATE_CHARGE' ? (
                    <>
                      <div>Target: {prop.input.paymentReference} (${((prop.input.amountCents || 0) / 100).toFixed(2)} {prop.input.currency})</div>
                      <div className="text-muted">Duplicate Of: {prop.input.duplicateOfReference}</div>
                    </>
                  ) : (
                    <>
                      <div>Target User: {prop.input.userId}</div>
                      <div className="text-muted">Reason: {prop.input.unlockReason}</div>
                    </>
                  )}
                </div>

                {/* Policy Decision & Approvals */}
                {prop.policyDecision && (
                  <div className="text-[10px] text-muted flex items-center justify-between">
                    <span>Required: {prop.policyDecision.requiredPermissions.join(', ') || 'AGENT'}</span>
                    <span>Approvals: {prop.approvals.length} / {prop.policyDecision.requiredApprovalCount}</span>
                  </div>
                )}

                {/* Action Controls */}
                {!readOnly && (
                  <div className="flex items-center justify-end gap-1.5 pt-1 border-t border-border-subtle">
                    {isAwaitingApproval && (
                      <>
                        <button
                          onClick={() => handleReject(prop)}
                          disabled={actionLoading === `reject-${prop.id}`}
                          className="btn-ghost py-1 px-2 text-[10px] text-muted hover:text-danger flex items-center gap-1"
                        >
                          <XCircle className="h-3 w-3" /> Reject
                        </button>
                        <button
                          onClick={() => handleApprove(prop)}
                          disabled={actionLoading === `approve-${prop.id}`}
                          className="btn-secondary py-1 px-2.5 text-[10px] flex items-center gap-1"
                        >
                          {actionLoading === `approve-${prop.id}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <ShieldCheck className="h-3 w-3" />}
                          Approve
                        </button>
                      </>
                    )}
                    {isApproved && (
                      <button
                        onClick={() => handleExecute(prop)}
                        disabled={actionLoading === `execute-${prop.id}`}
                        className="btn-primary py-1 px-3 text-[10px] flex items-center gap-1"
                      >
                        {actionLoading === `execute-${prop.id}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <Send className="h-3 w-3" />}
                        Execute Action
                      </button>
                    )}
                    {isCompleted && (
                      <button
                        onClick={() => handleCompensate(prop)}
                        disabled={actionLoading === `compensate-${prop.id}`}
                        className="btn-ghost py-1 px-2 text-[10px] text-muted flex items-center gap-1"
                        title="Check compensation policy"
                      >
                        <RotateCcw className="h-3 w-3" /> Revert
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
