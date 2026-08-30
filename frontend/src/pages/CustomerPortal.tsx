import React, { useState, useEffect, useCallback } from 'react';
import {
  Send,
  CheckCircle2,
  Clock,
  MessageSquare,
  AlertCircle,
  RefreshCw,
  BookOpen,
  Search,
  HelpCircle,
  ChevronRight,
  ShieldCheck,
  CreditCard,
  Key,
  Layers,
  ArrowLeft,
  Loader2,
  PlusCircle,
  Inbox
} from 'lucide-react';
import { api } from '../api/client';
import { Ticket, TicketMessage, Citation } from '../types';

interface CustomerPortalProps {
  activeTab?: string;
  onSelectTab?: (tab: string) => void;
}

export const CustomerPortal: React.FC<CustomerPortalProps> = ({
  activeTab = 'create',
  onSelectTab
}) => {
  const currentTab = activeTab === 'help' ? 'help' : activeTab === 'my-tickets' ? 'my-tickets' : 'create';

  const setTab = (tab: string) => {
    if (onSelectTab) {
      onSelectTab(tab);
    }
  };

  // Ticket creation state
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('BILLING');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedTicket, setSubmittedTicket] = useState<Ticket | null>(null);

  // Tickets list & conversation state
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [messages, setMessages] = useState<TicketMessage[]>([]);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [replyText, setReplyText] = useState('');
  const [isSendingReply, setIsSendingReply] = useState(false);
  const [isLoadingTickets, setIsLoadingTickets] = useState(false);
  const [ticketError, setTicketError] = useState<string | null>(null);

  // Help Center search state
  const [helpQuery, setHelpQuery] = useState('');
  const [isSearchingHelp, setIsSearchingHelp] = useState(false);
  const [helpResults, setHelpResults] = useState<Citation[] | null>(null);
  const [selectedArticle, setSelectedArticle] = useState<Citation | null>(null);
  const [helpError, setHelpError] = useState<string | null>(null);

  const loadTickets = useCallback(async () => {
    setIsLoadingTickets(true);
    setTicketError(null);
    try {
      const data = await api.listCustomerTickets();
      setTickets(data);
    } catch (failure) {
      setTickets([]);
      setTicketError(failure instanceof Error ? failure.message : 'Unable to load tickets');
    } finally {
      setIsLoadingTickets(false);
    }
  }, []);

  const loadMessages = useCallback(async (ticketId: string) => {
    setIsLoadingMessages(true);
    try {
      const msgList = await api.getCustomerMessages(ticketId);
      setMessages(msgList);
    } catch {
      setMessages([]);
    } finally {
      setIsLoadingMessages(false);
    }
  }, []);

  useEffect(() => {
    if (currentTab === 'my-tickets') {
      loadTickets();
    }
  }, [currentTab, loadTickets]);

  useEffect(() => {
    if (selectedTicket) {
      loadMessages(selectedTicket.id);
    } else {
      setMessages([]);
    }
  }, [selectedTicket, loadMessages]);

  const handleSubmitTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject.trim() || !description.trim()) return;
    setIsSubmitting(true);
    setTicketError(null);

    const idempotencyKey = `cust-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;

    try {
      const ticket = await api.createCustomerTicket({
        subject: subject.trim(),
        description: description.trim(),
        category,
        priority: 'HIGH'
      }, idempotencyKey);
      setSubmittedTicket(ticket);
    } catch (failure) {
      setTicketError(failure instanceof Error ? failure.message : 'Unable to create ticket');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSendReply = async () => {
    if (!selectedTicket || !replyText.trim()) return;
    setIsSendingReply(true);
    setTicketError(null);
    try {
      const newMsg = await api.addCustomerMessage(selectedTicket.id, replyText.trim());
      setMessages(prev => [...prev, newMsg]);
      setReplyText('');
    } catch (failure) {
      setTicketError(failure instanceof Error ? failure.message : 'Unable to send reply');
    } finally {
      setIsSendingReply(false);
    }
  };

  const handleHelpSearch = async (e?: React.FormEvent, customQuery?: string) => {
    if (e) e.preventDefault();
    const query = customQuery !== undefined ? customQuery : helpQuery;
    if (!query.trim()) return;
    if (customQuery !== undefined) setHelpQuery(customQuery);
    setIsSearchingHelp(true);
    setHelpError(null);
    setSelectedArticle(null);
    try {
      const res = await api.searchKnowledge(query.trim(), 5);
      setHelpResults(res.citations);
    } catch (failure) {
      setHelpResults(null);
      setHelpError(failure instanceof Error ? failure.message : 'Knowledge retrieval search failed');
    } finally {
      setIsSearchingHelp(false);
    }
  };

  const curatedTopics = [
    {
      title: 'Billing & Invoice Disputes',
      desc: 'Learn how to retrieve past invoices, update recurring billing methods, and dispute duplicate charges.',
      query: 'duplicate charge invoice billing dispute credit card',
      icon: CreditCard
    },
    {
      title: 'SSO & SAML Authentication',
      desc: 'Troubleshoot Okta / Azure AD SAML 2.0 integration, certificate renewal, and 401 signature mismatches.',
      query: 'Okta SAML 401 signature certificate validation error',
      icon: Key
    },
    {
      title: 'API Rate Limits & Webhooks',
      desc: 'Understand HMAC-SHA256 signature verification, automatic backoff retries, and token refresh policies.',
      query: 'webhook signature verification retry rate limit',
      icon: Layers
    },
    {
      title: 'Account Security & Roles',
      desc: 'Manage workspace access, user permissions, audit logs, and multi-tenant domain isolation.',
      query: 'multi tenant permissions security workspace access',
      icon: ShieldCheck
    }
  ];

  return (
    <div className="max-w-5xl mx-auto p-6 space-y-6">
      {/* Header & Sub-Nav */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold text-DEFAULT">Customer Support Portal</h1>
          <p className="text-sm text-muted mt-1">Submit support tickets, track active investigations, or browse the Knowledge Center.</p>
        </div>
        <div className="flex items-center space-x-1.5 bg-surface-muted p-1 rounded-btn border border-border self-start sm:self-auto">
          <button
            onClick={() => { setTab('create'); setSelectedTicket(null); }}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 text-xs font-semibold rounded-btn transition-colors ${
              currentTab === 'create' ? 'bg-surface text-primary shadow-sm' : 'text-muted hover:text-DEFAULT'
            }`}
          >
            <PlusCircle className="w-3.5 h-3.5" />
            <span>Create Ticket</span>
          </button>
          <button
            onClick={() => { setTab('my-tickets'); setSelectedTicket(null); }}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 text-xs font-semibold rounded-btn transition-colors ${
              currentTab === 'my-tickets' ? 'bg-surface text-primary shadow-sm' : 'text-muted hover:text-DEFAULT'
            }`}
          >
            <Inbox className="w-3.5 h-3.5" />
            <span>My Tickets</span>
          </button>
          <button
            onClick={() => { setTab('help'); setSelectedTicket(null); }}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 text-xs font-semibold rounded-btn transition-colors ${
              currentTab === 'help' ? 'bg-surface text-primary shadow-sm' : 'text-muted hover:text-DEFAULT'
            }`}
          >
            <BookOpen className="w-3.5 h-3.5" />
            <span>Help Center</span>
          </button>
        </div>
      </div>

      {ticketError && (
        <div role="alert" className="p-3.5 bg-danger/10 border border-danger/20 rounded-card text-danger text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{ticketError}</span>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 1. CREATE TICKET VIEW */}
      {/* ========================================================================= */}
      {currentTab === 'create' && (
        submittedTicket ? (
          <div className="bg-surface border border-success/30 rounded-card p-6 shadow-sm space-y-4">
            <div className="flex items-center space-x-3 text-success">
              <CheckCircle2 className="w-6 h-6 flex-shrink-0" />
              <h2 className="text-lg font-semibold">Ticket Created Successfully</h2>
            </div>
            <p className="text-sm text-muted">
              Your ticket <span className="font-mono font-bold text-DEFAULT">{submittedTicket.ticketNumber}</span> has been securely logged into the platform with transactional consistency.
            </p>
            <div className="p-3.5 bg-surface-muted rounded-card space-y-1.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-muted font-medium">Subject:</span>
                <span className="font-semibold text-DEFAULT">{submittedTicket.subject}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted font-medium">Category / Priority:</span>
                <span className="font-semibold text-DEFAULT">{submittedTicket.category} / {submittedTicket.priority}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted font-medium">Initial Status:</span>
                <span className="font-semibold text-primary">{submittedTicket.status}</span>
              </div>
            </div>
            <div className="flex items-center space-x-2 text-xs text-ai font-medium bg-ai-soft px-3.5 py-2.5 rounded-btn">
              <Clock className="w-4 h-4 animate-spin flex-shrink-0" />
              <span>AI Triage Pipeline: Analyzing ticket content, classifying urgency, and retrieving knowledge citations...</span>
            </div>
            <div className="flex flex-wrap gap-3 pt-2">
              <button
                onClick={() => {
                  setSubmittedTicket(null);
                  setSubject('');
                  setDescription('');
                }}
                className="px-4 py-2 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors"
              >
                Submit Another Request
              </button>
              <button
                onClick={() => {
                  setSubmittedTicket(null);
                  setTab('my-tickets');
                }}
                className="px-4 py-2 bg-surface border border-border text-DEFAULT text-xs font-semibold rounded-btn hover:bg-surface-muted transition-colors"
              >
                View My Tickets
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmitTicket} className="bg-surface border border-border rounded-card p-6 shadow-sm space-y-5">
            <div>
              <h2 className="text-base font-semibold text-DEFAULT">Submit a Support Ticket</h2>
              <p className="text-xs text-muted mt-0.5">Please provide specific details so our intelligent triage pipeline can route your issue immediately.</p>
            </div>
            
            <div>
              <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                Subject <span className="text-danger">*</span>
              </label>
              <input
                type="text"
                required
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="Brief summary of the issue (e.g. Cannot access payment history)"
                className="w-full h-10 px-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface text-DEFAULT"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                  Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full h-10 px-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface text-DEFAULT"
                >
                  <option value="BILLING">Billing & Payments</option>
                  <option value="TECHNICAL">Technical Support / SSO</option>
                  <option value="ACCOUNT">Account Management</option>
                  <option value="DELIVERY">Delivery & Logistics</option>
                  <option value="GENERAL">General Inquiries</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                  Suggested Action
                </label>
                <div className="h-10 px-3 flex items-center text-xs text-muted bg-surface-muted border border-border rounded-input">
                  Automated routing & triage by ResolveIQ AI
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-muted uppercase tracking-wider mb-1.5">
                Detailed Description <span className="text-danger">*</span>
              </label>
              <textarea
                required
                rows={5}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Provide complete details including steps to reproduce, invoice numbers, error codes, and impacted features..."
                className="w-full p-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface text-DEFAULT"
              />
            </div>

            <div className="pt-2 flex items-center justify-between">
              <button
                type="button"
                onClick={() => setTab('help')}
                className="text-xs text-primary font-medium hover:underline flex items-center space-x-1"
              >
                <HelpCircle className="w-3.5 h-3.5" />
                <span>Search Knowledge Base before submitting</span>
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="inline-flex items-center space-x-2 px-5 py-2.5 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors disabled:opacity-50"
              >
                {isSubmitting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                <span>{isSubmitting ? 'Submitting Ticket...' : 'Submit Request'}</span>
              </button>
            </div>
          </form>
        )
      )}

      {/* ========================================================================= */}
      {/* 2. MY TICKETS LIST & CONVERSATION VIEW */}
      {/* ========================================================================= */}
      {currentTab === 'my-tickets' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-DEFAULT">My Support History</h2>
            <button
              onClick={loadTickets}
              disabled={isLoadingTickets}
              className="inline-flex items-center space-x-1.5 text-xs text-muted hover:text-DEFAULT px-2.5 py-1 rounded-btn hover:bg-surface-muted transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoadingTickets ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>

          {selectedTicket ? (
            /* Ticket Conversation View */
            <div className="bg-surface border border-border rounded-card p-6 shadow-sm space-y-5">
              <div className="flex items-center justify-between border-b border-border-subtle pb-4">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-xs font-bold text-DEFAULT">{selectedTicket.ticketNumber}</span>
                    <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded-full bg-surface-muted text-muted border border-border">
                      {selectedTicket.status}
                    </span>
                    <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20">
                      {selectedTicket.priority}
                    </span>
                  </div>
                  <h3 className="text-lg font-semibold text-DEFAULT">{selectedTicket.subject}</h3>
                  <p className="text-xs text-muted">Created on {new Date(selectedTicket.createdAt).toLocaleString()}</p>
                </div>
                <button
                  onClick={() => setSelectedTicket(null)}
                  className="inline-flex items-center space-x-1 text-xs text-primary font-semibold hover:underline"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back to All Tickets</span>
                </button>
              </div>

              {/* Initial Ticket Description */}
              <div className="p-4 bg-surface-muted rounded-card space-y-1.5">
                <span className="text-[10px] font-semibold text-muted uppercase tracking-wider">Original Request</span>
                <p className="text-xs text-DEFAULT whitespace-pre-wrap leading-relaxed">{selectedTicket.description}</p>
              </div>

              {/* Message Thread */}
              <div className="space-y-3 pt-2">
                <h4 className="text-xs font-semibold text-muted uppercase tracking-wider">Conversation History</h4>
                {isLoadingMessages ? (
                  <div className="py-4 text-center text-xs text-muted flex items-center justify-center space-x-2">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Loading messages...</span>
                  </div>
                ) : messages.length === 0 ? (
                  <div className="p-4 text-center text-xs text-muted bg-surface-muted rounded-card">
                    No replies yet. An agent or automated update will appear here shortly.
                  </div>
                ) : (
                  messages.map((m) => (
                    <div
                      key={m.id}
                      className={`p-3.5 rounded-card text-xs space-y-1 border ${
                        m.senderRole === 'CUSTOMER'
                          ? 'bg-primary/5 border-primary/20 ml-6 text-DEFAULT'
                          : 'bg-surface-muted border-border mr-6 text-DEFAULT'
                      }`}
                    >
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="font-semibold text-primary">
                          {m.senderRole === 'CUSTOMER' ? 'You' : m.senderRole === 'AGENT' ? 'Support Agent' : 'System Bot'}
                        </span>
                        <span className="text-muted text-[10px]">{new Date(m.createdAt).toLocaleTimeString()}</span>
                      </div>
                      <p className="whitespace-pre-wrap leading-relaxed">{m.content}</p>
                    </div>
                  ))
                )}
              </div>

              {/* Reply Box */}
              <div className="pt-4 border-t border-border-subtle space-y-3">
                <label className="block text-xs font-semibold text-muted uppercase tracking-wider">
                  Add Reply to Support Team
                </label>
                <textarea
                  rows={3}
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  placeholder="Provide additional details or response..."
                  className="w-full p-3 rounded-input border border-border text-sm focus:outline-none focus:ring-2 focus:ring-primary bg-surface text-DEFAULT"
                />
                <div className="flex justify-end">
                  <button
                    onClick={handleSendReply}
                    disabled={isSendingReply || !replyText.trim()}
                    className="inline-flex items-center space-x-2 px-4 py-2 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors disabled:opacity-50"
                  >
                    {isSendingReply ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                    <span>{isSendingReply ? 'Sending Reply...' : 'Send Reply'}</span>
                  </button>
                </div>
              </div>
            </div>
          ) : (
            /* Ticket Listing Table/Cards */
            <div className="space-y-2.5">
              {isLoadingTickets && (
                <div className="p-8 text-center text-muted bg-surface border border-border rounded-card text-xs flex items-center justify-center space-x-2">
                  <Loader2 className="w-4 h-4 animate-spin text-primary" />
                  <span>Loading support tickets...</span>
                </div>
              )}

              {!isLoadingTickets && tickets.length === 0 && (
                <div className="p-10 text-center bg-surface border border-border rounded-card space-y-3">
                  <div className="w-10 h-10 mx-auto rounded-full bg-primary/10 flex items-center justify-center text-primary">
                    <Inbox className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-DEFAULT">No Tickets Found</p>
                    <p className="text-xs text-muted mt-0.5">You haven't submitted any support requests in this workspace yet.</p>
                  </div>
                  <button
                    onClick={() => setTab('create')}
                    className="px-4 py-2 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors"
                  >
                    Create Your First Ticket
                  </button>
                </div>
              )}

              {!isLoadingTickets && tickets.map((t) => (
                <div
                  key={t.id}
                  onClick={() => setSelectedTicket(t)}
                  className="p-4 bg-surface border border-border hover:border-primary/50 cursor-pointer rounded-card transition-all flex items-center justify-between group shadow-sm hover:shadow"
                >
                  <div className="space-y-1.5 flex-1 pr-4">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-xs font-bold text-DEFAULT">{t.ticketNumber}</span>
                      <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-full border ${
                        t.status === 'RESOLVED' || t.status === 'CLOSED'
                          ? 'bg-success/10 text-success border-success/20'
                          : t.status === 'READY_FOR_AGENT' || t.status === 'IN_PROGRESS'
                          ? 'bg-primary/10 text-primary border-primary/20'
                          : 'bg-surface-muted text-muted border-border'
                      }`}>
                        {t.status.replace(/_/g, ' ')}
                      </span>
                      {t.category && (
                        <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-surface-muted text-muted">
                          {t.category}
                        </span>
                      )}
                    </div>
                    <p className="text-sm font-medium text-DEFAULT group-hover:text-primary transition-colors line-clamp-1">
                      {t.subject}
                    </p>
                    <p className="text-xs text-muted">
                      Created on {new Date(t.createdAt).toLocaleDateString()} at {new Date(t.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                  <div className="flex items-center space-x-2 text-xs font-medium text-primary flex-shrink-0">
                    <MessageSquare className="w-4 h-4" />
                    <span className="hidden sm:inline">View Details</span>
                    <ChevronRight className="w-4 h-4 text-muted group-hover:text-primary transition-colors" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* 3. HELP CENTER & SELF-SERVICE KNOWLEDGE BASE VIEW */}
      {/* ========================================================================= */}
      {currentTab === 'help' && (
        <div className="space-y-6">
          <div className="bg-surface border border-border rounded-card p-6 shadow-sm space-y-4">
            <div className="max-w-2xl">
              <h2 className="text-lg font-bold text-DEFAULT flex items-center space-x-2">
                <BookOpen className="w-5 h-5 text-primary" />
                <span>ResolveIQ Self-Service Knowledge Base</span>
              </h2>
              <p className="text-xs text-muted mt-1">
                Search verified troubleshooting guides, SLA policies, and knowledge articles powered by our hybrid RRF search engine.
              </p>
            </div>

            <form onSubmit={(e) => handleHelpSearch(e)} className="flex items-center space-x-2 bg-surface-muted p-1.5 rounded-card border border-border">
              <Search className="w-4 h-4 text-muted ml-2.5 flex-shrink-0" />
              <input
                type="text"
                value={helpQuery}
                onChange={(e) => setHelpQuery(e.target.value)}
                placeholder="Search troubleshooting guides (e.g. invoice dispute, Okta SAML error, webhook HMAC)..."
                className="flex-1 bg-transparent text-sm focus:outline-none text-DEFAULT px-2 py-1"
              />
              <button
                type="submit"
                disabled={isSearchingHelp || !helpQuery.trim()}
                className="px-4 py-2 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors disabled:opacity-50 flex items-center space-x-1.5 flex-shrink-0"
              >
                {isSearchingHelp && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>{isSearchingHelp ? 'Searching...' : 'Search Articles'}</span>
              </button>
            </form>

            {helpError && (
              <div role="alert" className="p-3 bg-danger/10 text-danger border border-danger/20 rounded-card text-xs">
                {helpError}
              </div>
            )}
          </div>

          {/* Search Results Display */}
          {helpResults && (
            <div className="bg-surface border border-primary/30 rounded-card p-5 space-y-4 shadow-sm">
              <div className="flex items-center justify-between border-b border-border pb-3">
                <h3 className="text-sm font-semibold text-DEFAULT flex items-center space-x-2">
                  <Search className="w-4 h-4 text-primary" />
                  <span>Search Results ({helpResults.length} articles found)</span>
                </h3>
                <button
                  onClick={() => { setHelpResults(null); setSelectedArticle(null); }}
                  className="text-xs text-muted hover:text-DEFAULT"
                >
                  Clear Results
                </button>
              </div>

              {selectedArticle ? (
                /* Full Article Detail Card */
                <div className="p-4 bg-surface-muted rounded-card space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-primary uppercase tracking-wider">{selectedArticle.sourceType}</span>
                    <button
                      onClick={() => setSelectedArticle(null)}
                      className="text-xs text-primary font-semibold hover:underline"
                    >
                      Back to Results
                    </button>
                  </div>
                  <h4 className="text-base font-bold text-DEFAULT">{selectedArticle.title}</h4>
                  <div className="text-xs text-DEFAULT leading-relaxed whitespace-pre-wrap p-3 bg-surface rounded-card border border-border">
                    {selectedArticle.citationText || selectedArticle.snippet}
                  </div>
                </div>
              ) : helpResults.length === 0 ? (
                <div className="p-6 text-center text-muted text-xs space-y-2">
                  <p>No verified articles matched your query.</p>
                  <button
                    onClick={() => setTab('create')}
                    className="text-xs text-primary font-semibold hover:underline"
                  >
                    Submit a support ticket instead →
                  </button>
                </div>
              ) : (
                <div className="space-y-2.5">
                  {helpResults.map((art, idx) => (
                    <div
                      key={idx}
                      onClick={() => setSelectedArticle(art)}
                      className="p-3.5 bg-surface border border-border hover:border-primary/50 cursor-pointer rounded-card transition-colors space-y-1"
                    >
                      <div className="flex items-center justify-between">
                        <h4 className="text-sm font-semibold text-DEFAULT hover:text-primary">{art.title}</h4>
                        {art.confidenceScore !== undefined && (
                          <span className="text-[10px] font-mono text-muted bg-surface-muted px-2 py-0.5 rounded-full">
                            Score: {(art.confidenceScore * 100).toFixed(1)}%
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-muted line-clamp-2">{art.snippet || art.citationText}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Curated Help Topics */}
          <div>
            <h3 className="text-sm font-bold text-muted uppercase tracking-wider mb-3">Popular Knowledge Topics</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {curatedTopics.map((topic, idx) => {
                const IconComponent = topic.icon;
                return (
                  <div
                    key={idx}
                    onClick={() => handleHelpSearch(undefined, topic.query)}
                    className="p-4 bg-surface border border-border hover:border-primary/50 rounded-card transition-all cursor-pointer group shadow-sm hover:shadow space-y-2"
                  >
                    <div className="flex items-center space-x-3">
                      <div className="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center group-hover:bg-primary group-hover:text-white transition-colors">
                        <IconComponent className="w-4 h-4" />
                      </div>
                      <h4 className="text-sm font-semibold text-DEFAULT group-hover:text-primary transition-colors">
                        {topic.title}
                      </h4>
                    </div>
                    <p className="text-xs text-muted leading-relaxed">{topic.desc}</p>
                    <div className="flex items-center space-x-1 text-xs text-primary font-medium pt-1">
                      <span>Explore articles</span>
                      <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Still Need Help Banner */}
          <div className="p-5 bg-surface border border-border rounded-card flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm">
            <div>
              <h4 className="text-sm font-bold text-DEFAULT">Can't find what you're looking for?</h4>
              <p className="text-xs text-muted mt-0.5">Our support team and automated AI triage engine are ready to assist you.</p>
            </div>
            <button
              onClick={() => setTab('create')}
              className="px-4 py-2 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors flex-shrink-0"
            >
              Submit a Support Ticket
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
