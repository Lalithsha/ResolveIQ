import React, { useState, useEffect, useCallback } from 'react';
import { Send, CheckCircle2, Clock, MessageSquare, AlertCircle, RefreshCw } from 'lucide-react';
import { api } from '../api/client';
import { Ticket } from '../types';

export const CustomerPortal: React.FC = () => {
  const [activeView, setActiveView] = useState<'create' | 'list'>('create');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('BILLING');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedTicket, setSubmittedTicket] = useState<Ticket | null>(null);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [replyText, setReplyText] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadTickets = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.listCustomerTickets();
      setTickets(data);
    } catch (failure) {
      setTickets([]);
      setError(failure instanceof Error ? failure.message : 'Unable to load tickets');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeView === 'list') {
      loadTickets();
    }
  }, [activeView, loadTickets]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject || !description) return;
    setIsSubmitting(true);
    setError(null);

    const idempotencyKey = `cust-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;

    try {
      const ticket = await api.createCustomerTicket({
        subject,
        description,
        category,
        priority: 'HIGH'
      }, idempotencyKey);
      setSubmittedTicket(ticket);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to create ticket');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSendReply = async () => {
    if (!selectedTicket || !replyText.trim()) return;
    try {
      await api.addCustomerMessage(selectedTicket.id, replyText);
      setReplyText('');
      alert('Reply sent successfully!');
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Unable to send reply');
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-DEFAULT">Customer Support Portal</h1>
          <p className="text-sm text-muted mt-1">Submit a new request or track your open tickets.</p>
        </div>
        <div className="flex space-x-2 bg-surface-muted p-1 rounded-btn border border-border">
          <button
            onClick={() => { setActiveView('create'); setSelectedTicket(null); }}
            className={`px-4 py-1.5 text-xs font-semibold rounded-btn transition-colors ${
              activeView === 'create' ? 'bg-surface text-primary shadow-sm' : 'text-muted hover:text-DEFAULT'
            }`}
          >
            New Request
          </button>
          <button
            onClick={() => setActiveView('list')}
            className={`px-4 py-1.5 text-xs font-semibold rounded-btn transition-colors ${
              activeView === 'list' ? 'bg-surface text-primary shadow-sm' : 'text-muted hover:text-DEFAULT'
            }`}
          >
            My Tickets
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3 bg-danger/10 border border-danger/20 rounded-card text-danger text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {activeView === 'create' ? (
        submittedTicket ? (
          <div className="bg-surface border border-success/30 rounded-card p-6 shadow-sm space-y-4">
            <div className="flex items-center space-x-3 text-success">
              <CheckCircle2 className="w-6 h-6" />
              <h2 className="text-lg font-semibold">Ticket Created Successfully</h2>
            </div>
            <p className="text-sm text-muted">
              Your ticket <span className="font-mono font-bold text-DEFAULT">{submittedTicket.ticketNumber}</span> has been securely logged with an immutable Transactional Outbox event.
            </p>
            <div className="flex items-center space-x-2 text-xs text-ai font-medium bg-ai-soft px-3 py-2 rounded-btn">
              <Clock className="w-4 h-4 animate-spin" />
              <span>AI Triage Pipeline: Performing intent classification & hybrid knowledge retrieval...</span>
            </div>
            <div className="flex space-x-3 pt-2">
              <button
                onClick={() => {
                  setSubmittedTicket(null);
                  setSubject('');
                  setDescription('');
                }}
                className="px-4 py-2 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover transition-colors"
              >
                Submit Another Request
              </button>
              <button
                onClick={() => {
                  setActiveView('list');
                  setSubmittedTicket(null);
                }}
                className="px-4 py-2 bg-surface border border-border text-DEFAULT text-sm font-semibold rounded-btn hover:bg-surface-muted transition-colors"
              >
                View My Tickets
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="bg-surface border border-border rounded-card p-6 shadow-sm space-y-5">
            <h2 className="text-base font-semibold text-DEFAULT">Submit a Support Ticket</h2>
            
            <div>
              <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                Subject
              </label>
              <input
                type="text"
                required
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Brief summary of the issue (e.g. Cannot access payment history)"
                className="w-full h-10 px-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                Category
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full h-10 px-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface"
              >
                <option value="BILLING">Billing & Payments</option>
                <option value="TECHNICAL">Technical Support / SSO</option>
                <option value="ACCOUNT">Account Management</option>
                <option value="DELIVERY">Delivery & Logistics</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                Detailed Description
              </label>
              <textarea
                required
                rows={5}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Provide complete details including steps to reproduce, invoice numbers, error codes, and impacted features..."
                className="w-full p-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface"
              />
            </div>

            <div className="pt-2 flex justify-end">
              <button
                type="submit"
                disabled={isSubmitting}
                className="inline-flex items-center space-x-2 px-5 py-2.5 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors disabled:opacity-50"
              >
                <Send className="w-4 h-4" />
                <span>{isSubmitting ? 'Submitting...' : 'Submit Request'}</span>
              </button>
            </div>
          </form>
        )
      ) : (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-DEFAULT">My Support History</h2>
            <button
              onClick={loadTickets}
              className="inline-flex items-center space-x-1.5 text-xs text-muted hover:text-DEFAULT"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>

          {selectedTicket ? (
            <div className="bg-surface border border-border rounded-card p-6 shadow-sm space-y-4">
              <div className="flex items-center justify-between border-b border-border-subtle pb-3">
                <div>
                  <span className="font-mono text-xs font-bold text-DEFAULT">{selectedTicket.ticketNumber}</span>
                  <h3 className="text-base font-semibold text-DEFAULT mt-0.5">{selectedTicket.subject}</h3>
                </div>
                <button
                  onClick={() => setSelectedTicket(null)}
                  className="text-xs text-primary font-semibold hover:underline"
                >
                  Back to List
                </button>
              </div>

              <div className="p-3 bg-surface-muted rounded-card text-xs text-DEFAULT leading-relaxed">
                {selectedTicket.description}
              </div>

              <div className="pt-3 border-t border-border-subtle space-y-3">
                <label className="block text-xs font-semibold text-muted uppercase tracking-wider">
                  Add Reply to Support
                </label>
                <textarea
                  rows={3}
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  placeholder="Type your response here..."
                  className="w-full p-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface"
                />
                <div className="flex justify-end">
                  <button
                    onClick={handleSendReply}
                    className="inline-flex items-center space-x-2 px-4 py-2 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Send Reply</span>
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              {tickets.length === 0 && !isLoading && (
                <div className="p-8 text-center text-muted bg-surface border border-border rounded-card text-sm">
                  No tickets found. Create your first request above!
                </div>
              )}
              {tickets.map((t) => (
                <div
                  key={t.id}
                  onClick={() => setSelectedTicket(t)}
                  className="p-4 bg-surface border border-border hover:border-primary/50 cursor-pointer rounded-card transition-colors flex items-center justify-between"
                >
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-xs font-bold text-DEFAULT">{t.ticketNumber}</span>
                      <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded-full bg-surface-muted text-muted border border-border">
                        {t.status}
                      </span>
                    </div>
                    <p className="text-sm font-medium text-DEFAULT">{t.subject}</p>
                  </div>
                  <div className="flex items-center space-x-2 text-xs text-muted">
                    <MessageSquare className="w-4 h-4" />
                    <span>View Conversation</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
