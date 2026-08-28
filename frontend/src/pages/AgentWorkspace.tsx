import React, { useState } from 'react';
import { 
  Sparkles, 
  Check, 
  X, 
  RefreshCw, 
  Send, 
  BookOpen, 
  ShieldCheck,
  Clock
} from 'lucide-react';

export const AgentWorkspace: React.FC = () => {
  const [activeDraft, setActiveDraft] = useState(
    "Hello Alex, thank you for contacting support. I have verified your account and identified that the payment retry mechanism encountered a temporary gateway timeout. I have manually triggered a balance reconciliation and your invoice status has now updated to Paid. Please let us know if you need any additional assistance."
  );
  const [feedbackGiven, setFeedbackGiven] = useState<string | null>(null);

  return (
    <div className="h-[calc(100vh-4rem)] flex overflow-hidden">
      {/* 1. Left Context Rail (280-320px) */}
      <aside className="w-72 bg-surface border-r border-border p-4 flex flex-col justify-between overflow-y-auto">
        <div className="space-y-4">
          <div>
            <div className="flex items-center justify-between">
              <span className="font-mono font-bold text-sm text-DEFAULT">RIQ-2026-000412</span>
              <span className="text-[11px] font-semibold bg-danger/10 text-danger px-2 py-0.5 rounded-full border border-danger/20">
                HIGH PRIORITY
              </span>
            </div>
            <h2 className="text-sm font-semibold text-DEFAULT mt-1">Payment Failed Double Charge</h2>
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
          <div className="bg-surface border border-border rounded-card p-4 shadow-sm max-w-2xl">
            <div className="flex items-center justify-between text-xs text-muted mb-2">
              <span className="font-semibold text-DEFAULT">Alex Morgan (Customer)</span>
              <span>15 mins ago</span>
            </div>
            <p className="text-sm text-DEFAULT leading-relaxed">
              I noticed my credit card was charged twice for invoice #INV-9812. The dashboard shows "payment pending" and my account is locked out of premium features. Please fix this immediately.
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
              onClick={() => alert('Message approved and sent to customer!')}
              className="inline-flex items-center space-x-2 px-5 py-2 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors"
            >
              <Send className="w-4 h-4" />
              <span>Approve & Send</span>
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
                <span className="text-[10px] text-success font-medium">Sanitized</span>
              </div>
              <p className="text-muted text-[11px]">
                "Resolved double charge issue by manual ledger check and unlocking tenant subscription."
              </p>
            </div>
          </div>

          {/* Feedback Controls */}
          <div className="border-t border-border-subtle pt-3 space-y-2">
            <span className="text-xs font-semibold text-muted uppercase tracking-wider">
              Agent Suggestion Feedback
            </span>
            {feedbackGiven ? (
              <div className="text-xs text-success bg-success/10 p-2 rounded border border-success/20 font-medium">
                Feedback recorded: {feedbackGiven}
              </div>
            ) : (
              <div className="grid grid-cols-3 gap-2">
                <button
                  onClick={() => setFeedbackGiven('Accepted')}
                  className="flex items-center justify-center space-x-1 px-2 py-1.5 bg-surface-muted hover:bg-success/10 hover:text-success border border-border-subtle rounded-btn text-xs font-medium transition-colors"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>Accept</span>
                </button>
                <button
                  onClick={() => setFeedbackGiven('Rejected')}
                  className="flex items-center justify-center space-x-1 px-2 py-1.5 bg-surface-muted hover:bg-danger/10 hover:text-danger border border-border-subtle rounded-btn text-xs font-medium transition-colors"
                >
                  <X className="w-3.5 h-3.5" />
                  <span>Reject</span>
                </button>
                <button
                  onClick={() => setFeedbackGiven('Regenerated')}
                  className="flex items-center justify-center space-x-1 px-2 py-1.5 bg-surface-muted hover:bg-ai/10 hover:text-ai border border-border-subtle rounded-btn text-xs font-medium transition-colors"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Regen</span>
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="text-[11px] text-muted space-y-1 border-t border-border-subtle pt-3">
          <div><span className="font-medium">Model:</span> mock-chat-v1</div>
          <div><span className="font-medium">Prompt:</span> triage-agent-v1</div>
          <div><span className="font-medium">Latency:</span> 420ms</div>
        </div>
      </aside>
    </div>
  );
};
