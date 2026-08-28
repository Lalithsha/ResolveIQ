import React, { useState } from 'react';
import { Send, CheckCircle2, Clock } from 'lucide-react';

export const CustomerPortal: React.FC = () => {
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('TECHNICAL');
  const [submittedTicket, setSubmittedTicket] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject || !description) return;
    const ticketNum = `RIQ-2026-${Math.floor(100000 + Math.random() * 900000)}`;
    setSubmittedTicket(ticketNum);
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-DEFAULT">Customer Support Portal</h1>
        <p className="text-sm text-muted mt-1">Submit a new request or check the status of your existing tickets.</p>
      </div>

      {submittedTicket ? (
        <div className="bg-surface border border-success/30 rounded-card p-6 shadow-sm space-y-4">
          <div className="flex items-center space-x-3 text-success">
            <CheckCircle2 className="w-6 h-6" />
            <h2 className="text-lg font-semibold">Ticket Created Successfully</h2>
          </div>
          <p className="text-sm text-muted">
            Your ticket <span className="font-mono font-bold text-DEFAULT">{submittedTicket}</span> has been logged. Our AI triage system is analyzing your request and matching it with relevant support resources.
          </p>
          <div className="flex items-center space-x-2 text-xs text-ai font-medium bg-ai-soft px-3 py-2 rounded-btn">
            <Clock className="w-4 h-4 animate-spin" />
            <span>AI Triage Status: Asynchronous processing in progress...</span>
          </div>
          <button
            onClick={() => {
              setSubmittedTicket(null);
              setSubject('');
              setDescription('');
            }}
            className="px-4 py-2 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover transition-colors"
          >
            Create Another Ticket
          </button>
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
              <option value="TECHNICAL">Technical Support</option>
              <option value="BILLING">Billing & Payments</option>
              <option value="ACCOUNT">Account Management</option>
              <option value="DELIVERY">Delivery & Orders</option>
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
              placeholder="Provide complete details including steps to reproduce, error codes, and impacted features..."
              className="w-full p-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface"
            />
          </div>

          <div className="pt-2 flex justify-end">
            <button
              type="submit"
              className="inline-flex items-center space-x-2 px-5 py-2.5 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors"
            >
              <Send className="w-4 h-4" />
              <span>Submit Request</span>
            </button>
          </div>
        </form>
      )}
    </div>
  );
};
