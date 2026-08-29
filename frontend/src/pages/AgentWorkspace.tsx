import React, { useState, useEffect, useCallback } from 'react';
import { 
  Sparkles, 
  Send, 
  BookOpen, 
  ShieldCheck, 
  Clock, 
  ThumbsUp, 
  ThumbsDown 
} from 'lucide-react';
import { api } from '../api/client';
import { Ticket, AiSuggestion } from '../types';

export const AgentWorkspace: React.FC = () => {
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [suggestions, setSuggestions] = useState<AiSuggestion[]>([]);
  const [activeDraft, setActiveDraft] = useState('');
  const [feedbackGiven, setFeedbackGiven] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const loadSuggestions = useCallback(async (ticketId: string) => {
    try {
      const sugs = await api.getTicketSuggestions(ticketId);
      setSuggestions(sugs);
      if (sugs.length > 0) {
        setActiveDraft(sugs[0].suggestedResponse);
      }
    } catch (failure) {
      setStatusMessage(failure instanceof Error ? failure.message : 'Unable to load AI suggestions');
    }
  }, []);

  const loadTickets = useCallback(async () => {
    try {
      const data = await api.listAgentTickets();
      if (data.length > 0) {
        setSelectedTicket(data[0]);
        loadSuggestions(data[0].id);
      }
    } catch (failure) {
      setStatusMessage(failure instanceof Error ? failure.message : 'Unable to load agent queue');
    }
  }, [loadSuggestions]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  const handleFeedback = async (action: 'ACCEPTED' | 'EDITED' | 'REJECTED') => {
    if (!selectedTicket || suggestions.length === 0) return;
    const rejectionReason = action === 'REJECTED' ? window.prompt('Why is this suggestion unsafe or unhelpful?') : undefined;
    if (action === 'REJECTED' && !rejectionReason?.trim()) return;
    try {
      await api.recordFeedback(selectedTicket.id, {
        suggestionId: suggestions[0].id,
        action,
        rejectionReason: rejectionReason || undefined,
        editedContent: action === 'EDITED' ? activeDraft : undefined,
      });
      setFeedbackGiven(action);
      setStatusMessage(`Feedback recorded: ${action}`);
    } catch (failure) {
      setStatusMessage(failure instanceof Error ? failure.message : 'Unable to record feedback');
    }
  };

  const handleApproveAndSend = async () => {
    if (!selectedTicket || !activeDraft.trim()) return;
    setIsSending(true);
    setStatusMessage(null);
    try {
      if (suggestions.length > 0 && !feedbackGiven) {
        const action = activeDraft === suggestions[0].suggestedResponse ? 'ACCEPTED' : 'EDITED';
        await api.recordFeedback(selectedTicket.id, {
          suggestionId: suggestions[0].id,
          action,
          editedContent: action === 'EDITED' ? activeDraft : undefined,
        });
        setFeedbackGiven(action);
      }
      await api.addAgentMessage(selectedTicket.id, activeDraft, false);
      await api.updateTicketStatus(selectedTicket.id, 'WAITING_ON_CUSTOMER', 'Agent response sent with grounded citations.');
      setStatusMessage('Message approved and sent to customer! Ticket moved to WAITING_ON_CUSTOMER.');
    } catch (failure) {
      setStatusMessage(failure instanceof Error ? failure.message : 'Unable to send message');
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="h-[calc(100vh-4rem)] flex overflow-hidden">
      {/* 1. Left Context Rail (280-320px) */}
      <aside className="w-72 bg-surface border-r border-border p-4 flex flex-col justify-between overflow-y-auto">
        <div className="space-y-4">
          <div>
            <div className="flex items-center justify-between">
              <span className="font-mono font-bold text-sm text-DEFAULT">
                {selectedTicket?.ticketNumber || 'RIQ-2026-000412'}
              </span>
              <span className="text-[11px] font-semibold bg-danger/10 text-danger px-2 py-0.5 rounded-full border border-danger/20">
                {selectedTicket?.priority || 'HIGH PRIORITY'}
              </span>
            </div>
            <h2 className="text-sm font-semibold text-DEFAULT mt-1">
              {selectedTicket?.subject || 'Payment Failed Double Charge'}
            </h2>
          </div>

          <div className="border-t border-border-subtle pt-3 space-y-2 text-xs">
            <div>
              <span className="text-muted font-medium">Customer:</span>
              <p className="font-semibold text-DEFAULT mt-0.5">Alex Morgan (Tenant: ACME Corp)</p>
            </div>
            <div>
              <span className="text-muted font-medium">Assigned Team:</span>
              <p className="font-semibold text-DEFAULT mt-0.5">Billing Tier 2</p>
            </div>
            <div>
              <span className="text-muted font-medium">SLA Target:</span>
              <div className="flex items-center space-x-1.5 text-warning font-semibold mt-0.5">
                <Clock className="w-3.5 h-3.5" />
                <span>First response due in 42 mins</span>
              </div>
            </div>
          </div>

          <div className="border-t border-border-subtle pt-3 space-y-1.5 text-xs">
            <span className="text-muted font-medium">AI Classification:</span>
            <div className="flex flex-wrap gap-1 mt-1">
              <span className="bg-surface-muted border border-border-subtle px-2 py-0.5 rounded text-[11px] font-mono">
                intent: billing_dispute
              </span>
              <span className="bg-surface-muted border border-border-subtle px-2 py-0.5 rounded text-[11px] font-mono">
                sentiment: negative (0.88)
              </span>
              <span className="bg-surface-muted border border-border-subtle px-2 py-0.5 rounded text-[11px] font-mono">
                urgency: high (0.92)
              </span>
            </div>
          </div>
        </div>

        <div className="p-3 bg-surface-muted rounded-card border border-border-subtle text-xs text-muted">
          <div className="flex items-center space-x-1.5 text-DEFAULT font-semibold mb-1">
            <ShieldCheck className="w-4 h-4 text-success" />
            <span>Human-in-the-Loop</span>
          </div>
          No message is sent to the customer until approved below.
        </div>
      </aside>

      {/* 2. Center Conversation & Composer */}
      <main className="flex-1 flex flex-col bg-background overflow-hidden">
        <div className="flex-1 p-6 overflow-y-auto space-y-4">
          {statusMessage && (
            <div className="p-3 bg-success/10 border border-success/20 rounded-card text-success text-xs flex items-center justify-between">
              <span>{statusMessage}</span>
              <button onClick={() => setStatusMessage(null)} className="text-success font-bold hover:underline">
                Dismiss
              </button>
            </div>
          )}

          <div className="bg-surface border border-border rounded-card p-4 shadow-sm max-w-2xl">
            <div className="flex items-center justify-between text-xs text-muted mb-2">
              <span className="font-semibold text-DEFAULT">Alex Morgan (Customer)</span>
              <span>15 mins ago</span>
            </div>
            <p className="text-sm text-DEFAULT leading-relaxed">
              {selectedTicket?.description || 'I noticed my credit card was charged twice for invoice #INV-9812. The dashboard shows "payment pending" and my account is locked out of premium features. Please fix this immediately.'}
            </p>
          </div>
        </div>

        <div className="p-4 bg-surface border-t border-border space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-DEFAULT">Response Composer</span>
            <span className="text-muted">Supports Markdown</span>
          </div>
          <textarea
            rows={4}
            value={activeDraft}
            onChange={(e) => setActiveDraft(e.target.value)}
            className="w-full p-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface font-sans"
          />
          <div className="flex items-center justify-between">
            <span className="text-xs text-muted">Review response before sending to customer.</span>
            <button
              disabled={isSending}
              onClick={handleApproveAndSend}
              className="inline-flex items-center space-x-2 px-5 py-2 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors disabled:opacity-50"
            >
              <Send className="w-4 h-4" />
              <span>{isSending ? 'Sending...' : 'Approve & Send'}</span>
            </button>
          </div>
        </div>
      </main>

      {/* 3. Right AI Intelligence Panel */}
      <aside className="w-96 bg-surface border-l border-border p-4 flex flex-col justify-between overflow-y-auto">
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 text-ai font-bold text-sm">
              <Sparkles className="w-4 h-4" />
              <span>AI Copilot Suggestion</span>
            </div>
            <span className="text-[11px] font-mono bg-ai-soft text-ai px-2 py-0.5 rounded border border-ai/20">
              94% confidence
            </span>
          </div>

          {/* Citations / Evidence */}
          <div className="space-y-2">
            <span className="text-xs font-semibold text-muted uppercase tracking-wider">
              Grounded Citations (2)
            </span>
            <div className="p-2.5 bg-surface-muted rounded-btn border border-border-subtle space-y-1 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-DEFAULT flex items-center space-x-1">
                  <BookOpen className="w-3.5 h-3.5 text-primary" />
                  <span>KB-104: Payment Reconciliation</span>
                </span>
                <span className="text-[10px] text-muted">v2.1</span>
              </div>
              <p className="text-muted text-[11px]">
                "When a gateway timeout causes duplicate pending records, reconcile balance from payment processor..."
              </p>
            </div>

            <div className="p-2.5 bg-surface-muted rounded-btn border border-border-subtle space-y-1 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-DEFAULT flex items-center space-x-1">
                  <BookOpen className="w-3.5 h-3.5 text-primary" />
                  <span>Resolved Case: RIQ-2026-000109</span>
                </span>
                <span className="text-[10px] text-muted">Sanitized</span>
              </div>
              <p className="text-muted text-[11px]">
                "Customer reported double charge on Visa card. Triggered refund for pending auth ID #8812."
              </p>
            </div>
          </div>

          {/* Similar Cases */}
          <div className="space-y-1.5">
            <span className="text-xs font-semibold text-muted uppercase tracking-wider">
              Similar Cases
            </span>
            <div className="p-2 bg-surface-muted rounded-btn text-xs flex items-center justify-between border border-border-subtle">
              <span className="font-mono text-DEFAULT">RIQ-2026-000088</span>
              <span className="text-success font-medium text-[11px]">96% match</span>
            </div>
          </div>
        </div>

        {/* Suggestion Feedback Actions */}
        <div className="border-t border-border-subtle pt-3 space-y-2">
          <span className="text-xs text-muted font-medium">Was this suggestion helpful?</span>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => handleFeedback('ACCEPTED')}
              className={`flex-1 py-1.5 px-3 rounded-btn text-xs font-semibold border flex items-center justify-center space-x-1.5 transition-colors ${
                feedbackGiven === 'ACCEPTED'
                  ? 'bg-success text-white border-success'
                  : 'bg-surface hover:bg-surface-muted border-border text-DEFAULT'
              }`}
            >
              <ThumbsUp className="w-3.5 h-3.5" />
              <span>Accept</span>
            </button>
            <button
              onClick={() => handleFeedback('EDITED')}
              className={`flex-1 py-1.5 px-3 rounded-btn text-xs font-semibold border flex items-center justify-center space-x-1.5 transition-colors ${
                feedbackGiven === 'EDITED'
                  ? 'bg-warning text-white border-warning'
                  : 'bg-surface hover:bg-surface-muted border-border text-DEFAULT'
              }`}
            >
              <span>Edit</span>
            </button>
            <button
              onClick={() => handleFeedback('REJECTED')}
              className={`flex-1 py-1.5 px-3 rounded-btn text-xs font-semibold border flex items-center justify-center space-x-1.5 transition-colors ${
                feedbackGiven === 'REJECTED'
                  ? 'bg-danger text-white border-danger'
                  : 'bg-surface hover:bg-surface-muted border-border text-DEFAULT'
              }`}
            >
              <ThumbsDown className="w-3.5 h-3.5" />
              <span>Reject</span>
            </button>
          </div>
        </div>
      </aside>
    </div>
  );
};
