import React, { useState, useEffect, useCallback } from 'react';
import {
  Mail,
  Globe,
  Lock,
  UserCheck,
  UserX,
  Send,
  Loader2,
  RefreshCw,
  Clock,
  MessageSquare,
} from 'lucide-react';
import { api } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import {
  TimelineResponse,
  ChannelType,
  ChannelIdentity,
  HandoffResponse,
} from '../../types';

interface Props {
  ticketId: string;
  isAgent?: boolean;
  readOnly?: boolean;
  onTicketUpdated?: () => void;
}

export const OmnichannelTimelineCard: React.FC<Props> = ({
  ticketId,
  isAgent = false,
  readOnly = false,
  onTicketUpdated,
}) => {
  const { user } = useAuth();
  const [timeline, setTimeline] = useState<TimelineResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Message posting state
  const [messageContent, setMessageContent] = useState('');
  const [selectedChannel, setSelectedChannel] = useState<ChannelType>('PORTAL');
  const [isInternalNote, setIsInternalNote] = useState(false);

  // Handoff state
  const [isHandoffLoading, setIsHandoffLoading] = useState(false);
  const [handoffReason, setHandoffReason] = useState('');
  const [showHandoffModal, setShowHandoffModal] = useState(false);

  // Email challenge linking state (for customers)
  const [identities, setIdentities] = useState<ChannelIdentity[]>([]);
  const [showVerifyModal, setShowVerifyModal] = useState(false);
  const [verifyEmail, setVerifyEmail] = useState('');
  const [verifyCode, setVerifyCode] = useState('');
  const [challengeStep, setChallengeStep] = useState<'REQUEST' | 'VERIFY'>('REQUEST');

  const loadTimeline = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await api.getConversationTimeline(ticketId);
      setTimeline(data);
      setSelectedChannel(data.preferredChannel || 'PORTAL');
    } catch {
      // If conversation timeline not yet available for legacy ticket, fall back silently
      setTimeline(null);
    } finally {
      setIsLoading(false);
    }
  }, [ticketId]);

  const loadIdentities = useCallback(async () => {
    if (isAgent) return;
    try {
      const list = await api.getCustomerIdentities();
      setIdentities(list);
    } catch {
      setIdentities([]);
    }
  }, [isAgent]);

  useEffect(() => {
    loadTimeline();
    loadIdentities();
  }, [loadTimeline, loadIdentities]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageContent.trim() || !timeline) return;

    setIsSending(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await api.addConversationMessage(timeline.conversationId, {
        content: messageContent.trim(),
        isInternal: isInternalNote,
        channel: isInternalNote ? undefined : selectedChannel,
        subject: 'Support Update',
      });
      setMessageContent('');
      setSuccessMessage(isInternalNote ? 'Internal note added (never sent to customer)' : 'Message dispatched successfully');
      await loadTimeline();
      if (onTicketUpdated) onTicketUpdated();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to send message');
    } finally {
      setIsSending(false);
    }
  };

  const handleRequestHandoff = async () => {
    if (!timeline) return;
    setIsHandoffLoading(true);
    setErrorMessage(null);
    try {
      const resp: HandoffResponse = await api.requestHandoff(timeline.conversationId, handoffReason);
      setTimeline((prev) => (prev ? { ...prev, handoffState: resp.state, latestHandoff: resp.summary } : prev));
      setShowHandoffModal(false);
      setSuccessMessage('Handoff requested. You are now in queue for a specialist.');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to request handoff');
    } finally {
      setIsHandoffLoading(false);
    }
  };

  const handleAssignToMe = async () => {
    if (!timeline) return;
    setIsHandoffLoading(true);
    try {
      const targetAgentId = user?.id || '00000000-0000-0000-0000-000000000002';
      await api.assignHandoff(timeline.conversationId, targetAgentId);
      setTimeline((prev) => (prev ? { ...prev, handoffState: 'ASSIGNED' } : prev));
      setSuccessMessage('Conversation assigned to you.');
      if (onTicketUpdated) onTicketUpdated();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to assign conversation');
    } finally {
      setIsHandoffLoading(false);
    }
  };

  const handleRequestChallenge = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verifyEmail.trim()) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      await api.requestEmailChallenge(verifyEmail.trim());
      setChallengeStep('VERIFY');
      setSuccessMessage(`Verification code sent to ${verifyEmail.trim()}`);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Failed to request code');
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verifyCode.trim()) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const resp = await api.verifyEmailChallenge(verifyEmail.trim(), verifyCode.trim());
      setSuccessMessage(`Email verified! ${resp.linkedPendingIntakes} prior email(s) linked to your account.`);
      setShowVerifyModal(false);
      setChallengeStep('REQUEST');
      await loadIdentities();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Verification failed');
    } finally {
      setIsLoading(false);
    }
  };

  const verifiedEmail = identities.find((i) => i.channel === 'EMAIL' && i.isVerified);

  return (
    <div className="rounded-card border border-border bg-surface p-4 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-border/60 pb-3">
        <div className="flex items-center gap-2">
          <MessageSquare className="h-4 w-4 text-primary" />
          <span className="text-xs font-bold text-DEFAULT">Omnichannel Continuity</span>
          {timeline?.preferredChannel && (
            <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">
              {timeline.preferredChannel === 'EMAIL' ? <Mail className="h-3 w-3" /> : <Globe className="h-3 w-3" />}
              Preferred: {timeline.preferredChannel}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={loadTimeline}
            disabled={isLoading}
            className="text-muted hover:text-DEFAULT"
            title="Refresh conversation"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          {!isAgent && (
            <button
              onClick={() => setShowVerifyModal(true)}
              className="btn-secondary py-1 px-2 text-[10px] flex items-center gap-1"
            >
              {verifiedEmail ? <UserCheck className="h-3 w-3 text-emerald-500" /> : <UserX className="h-3 w-3 text-amber-500" />}
              {verifiedEmail ? 'Email Verified' : 'Verify Email'}
            </button>
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

      {/* Handoff Status Bar */}
      {timeline?.handoffState && timeline.handoffState !== 'NONE' && (
        <div className="rounded-card border border-amber-500/30 bg-amber-500/10 p-3 text-xs space-y-1.5">
          <div className="flex items-center justify-between font-semibold text-amber-700">
            <span className="flex items-center gap-1.5">
              <Clock className="h-3.5 w-3.5" />
              Human Specialist Handoff: <span className="uppercase">{timeline.handoffState}</span>
            </span>
            {isAgent && timeline.handoffState === 'QUEUED' && (
              <button
                onClick={handleAssignToMe}
                disabled={isHandoffLoading}
                className="btn-primary py-0.5 px-2 text-[10px]"
              >
                Claim Handoff
              </button>
            )}
          </div>
          {timeline.latestHandoff && (
            <div className="text-[11px] text-muted space-y-1 pt-1 border-t border-amber-500/20">
              <div><strong className="text-DEFAULT">Reason:</strong> {timeline.latestHandoff.issueSummary}</div>
              <div><strong className="text-DEFAULT">Verified Context:</strong> {timeline.latestHandoff.verifiedFacts}</div>
              <div><strong className="text-DEFAULT">Attempted:</strong> {timeline.latestHandoff.attemptedSteps}</div>
              <div><strong className="text-DEFAULT">Sentiment:</strong> <span className="font-semibold text-amber-600">{timeline.latestHandoff.sentiment}</span></div>
            </div>
          )}
        </div>
      )}

      {/* "Talk to a Person" prompt for Customer */}
      {!isAgent && timeline?.handoffState === 'NONE' && !readOnly && (
        <div className="flex items-center justify-between rounded bg-surface-muted p-2.5 border border-border">
          <div className="text-[11px] text-muted">
            Need dedicated help from a human support specialist?
          </div>
          <button
            onClick={() => setShowHandoffModal(true)}
            className="btn-secondary py-1 px-2.5 text-[11px]"
          >
            Talk to a Person
          </button>
        </div>
      )}

      {/* Messages Timeline */}
      <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1">
        {(!timeline?.messages || timeline.messages.length === 0) ? (
          <p className="text-[11px] text-muted text-center py-4">No conversation messages yet.</p>
        ) : (
          timeline.messages.map((msg) => {
            const isCustomerSender = msg.senderRole === 'CUSTOMER';
            const isInternal = msg.isInternal;

            return (
              <div
                key={msg.messageId}
                className={`p-2.5 rounded-card border text-xs space-y-1 ${
                  isInternal
                    ? 'bg-amber-500/10 border-amber-500/30'
                    : isCustomerSender
                    ? 'bg-surface border-border'
                    : 'bg-primary/5 border-primary/20'
                }`}
              >
                <div className="flex items-center justify-between text-[10px] text-muted">
                  <div className="flex items-center gap-1.5 font-semibold text-DEFAULT">
                    {isInternal ? (
                      <span className="flex items-center gap-1 text-amber-600">
                        <Lock className="h-3 w-3" /> INTERNAL NOTE (Staff Only)
                      </span>
                    ) : (
                      <span>{isCustomerSender ? 'Customer' : 'Support Specialist'}</span>
                    )}
                    <span className="inline-flex items-center gap-0.5 rounded px-1.5 py-0.2 bg-surface-muted border text-[9px]">
                      {msg.channel === 'EMAIL' ? <Mail className="h-2.5 w-2.5" /> : <Globe className="h-2.5 w-2.5" />}
                      {msg.channel}
                    </span>
                    {msg.deliveryStatus && msg.deliveryStatus !== 'DELIVERED' && (
                      <span className="text-[9px] text-amber-600">({msg.deliveryStatus})</span>
                    )}
                  </div>
                  <span>{new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
                <p className="whitespace-pre-wrap text-DEFAULT leading-relaxed">{msg.content}</p>
                {msg.recipientAddress && (
                  <div className="text-[9px] text-muted">Dispatched to: {msg.recipientAddress}</div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Reply / Send Box */}
      {!readOnly && (
        <form onSubmit={handleSendMessage} className="space-y-2 border-t border-border/60 pt-3">
          <div className="flex items-center justify-between text-[11px]">
            <span className="font-semibold text-DEFAULT">Reply via Omnichannel</span>
            {isAgent && (
              <div className="flex items-center gap-2">
                <label className="flex items-center gap-1 cursor-pointer">
                  <input
                    type="radio"
                    name="replyChannel"
                    checked={!isInternalNote && selectedChannel === 'PORTAL'}
                    onChange={() => { setIsInternalNote(false); setSelectedChannel('PORTAL'); }}
                  />
                  <span>Portal</span>
                </label>
                <label className="flex items-center gap-1 cursor-pointer">
                  <input
                    type="radio"
                    name="replyChannel"
                    checked={!isInternalNote && selectedChannel === 'EMAIL'}
                    onChange={() => { setIsInternalNote(false); setSelectedChannel('EMAIL'); }}
                  />
                  <span>Email</span>
                </label>
                <label className="flex items-center gap-1 cursor-pointer text-amber-600 font-semibold">
                  <input
                    type="radio"
                    name="replyChannel"
                    checked={isInternalNote}
                    onChange={() => setIsInternalNote(true)}
                  />
                  <span className="flex items-center gap-0.5"><Lock className="h-3 w-3" /> Note</span>
                </label>
              </div>
            )}
          </div>

          <textarea
            rows={2}
            placeholder={
              isInternalNote
                ? 'Type staff-only internal note (will never be sent to customer)...'
                : `Type response (will be delivered via ${selectedChannel})...`
            }
            value={messageContent}
            onChange={(e) => setMessageContent(e.target.value)}
            className={`w-full rounded border px-3 py-2 text-xs focus:outline-none focus:ring-1 ${
              isInternalNote
                ? 'border-amber-500/40 bg-amber-500/5 focus:ring-amber-500'
                : 'border-border bg-surface focus:ring-primary'
            }`}
            required
          />

          <div className="flex items-center justify-between pt-0.5">
            <span className="text-[10px] text-muted">
              {isInternalNote ? 'Protected by internal note security filter.' : `Will dispatch via ${selectedChannel}.`}
            </span>
            <button
              type="submit"
              disabled={isSending || !messageContent.trim()}
              className={`py-1 px-3 text-xs flex items-center gap-1.5 rounded font-semibold text-white ${
                isInternalNote ? 'bg-amber-600 hover:bg-amber-700' : 'btn-primary'
              }`}
            >
              {isSending ? <Loader2 className="h-3 w-3 animate-spin" /> : <Send className="h-3 w-3" />}
              {isInternalNote ? 'Save Internal Note' : 'Send Message'}
            </button>
          </div>
        </form>
      )}

      {/* Customer Email Verification Challenge Modal */}
      {showVerifyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-card border border-border bg-surface p-4 space-y-3 shadow-xl">
            <div className="flex items-center justify-between border-b border-border pb-2">
              <span className="text-xs font-bold text-DEFAULT">Link & Verify Email Address</span>
              <button onClick={() => setShowVerifyModal(false)} className="text-muted hover:text-DEFAULT font-bold">×</button>
            </div>

            {challengeStep === 'REQUEST' ? (
              <form onSubmit={handleRequestChallenge} className="space-y-2.5">
                <p className="text-[11px] text-muted">
                  Link an email address to receive and reply to support updates directly from your mailbox.
                </p>
                <div>
                  <label className="text-[10px] text-muted uppercase">Email Address</label>
                  <input
                    type="email"
                    placeholder="you@example.com"
                    value={verifyEmail}
                    onChange={(e) => setVerifyEmail(e.target.value)}
                    className="mt-0.5 w-full rounded border border-border bg-surface px-2.5 py-1 text-xs"
                    required
                  />
                </div>
                <div className="flex justify-end gap-2 pt-1">
                  <button type="button" onClick={() => setShowVerifyModal(false)} className="btn-ghost py-1 px-2.5 text-xs">Cancel</button>
                  <button type="submit" disabled={isLoading} className="btn-primary py-1 px-3 text-xs">
                    {isLoading ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Send Code'}
                  </button>
                </div>
              </form>
            ) : (
              <form onSubmit={handleVerifyCode} className="space-y-2.5">
                <p className="text-[11px] text-muted">
                  Enter the 6-digit verification code sent to <strong>{verifyEmail}</strong>.
                </p>
                <div>
                  <label className="text-[10px] text-muted uppercase">Verification Code</label>
                  <input
                    type="text"
                    maxLength={6}
                    placeholder="123456"
                    value={verifyCode}
                    onChange={(e) => setVerifyCode(e.target.value)}
                    className="mt-0.5 w-full rounded border border-border bg-surface px-2.5 py-1 text-center font-mono text-sm tracking-widest"
                    required
                  />
                </div>
                <div className="flex justify-end gap-2 pt-1">
                  <button type="button" onClick={() => setChallengeStep('REQUEST')} className="btn-ghost py-1 px-2.5 text-xs">Back</button>
                  <button type="submit" disabled={isLoading} className="btn-primary py-1 px-3 text-xs">
                    {isLoading ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Confirm & Link'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Customer Handoff Request Modal */}
      {showHandoffModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-card border border-border bg-surface p-4 space-y-3 shadow-xl">
            <div className="flex items-center justify-between border-b border-border pb-2">
              <span className="text-xs font-bold text-DEFAULT">Request Human Specialist</span>
              <button onClick={() => setShowHandoffModal(false)} className="text-muted hover:text-DEFAULT font-bold">×</button>
            </div>
            <p className="text-[11px] text-muted">
              Describe what you need help with so the specialist can review the full context and previous steps.
            </p>
            <textarea
              rows={3}
              placeholder="e.g. Automated suggestion didn't address my specific billing discrepancy..."
              value={handoffReason}
              onChange={(e) => setHandoffReason(e.target.value)}
              className="w-full rounded border border-border bg-surface p-2 text-xs"
            />
            <div className="flex justify-end gap-2 pt-1">
              <button type="button" onClick={() => setShowHandoffModal(false)} className="btn-ghost py-1 px-2.5 text-xs">Cancel</button>
              <button
                type="button"
                disabled={isHandoffLoading}
                onClick={handleRequestHandoff}
                className="btn-primary py-1 px-3 text-xs"
              >
                {isHandoffLoading ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Enter Queue'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
