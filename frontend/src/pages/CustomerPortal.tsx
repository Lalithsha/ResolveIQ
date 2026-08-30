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
  Inbox,
  Paperclip,
  Download
} from 'lucide-react';
import { api } from '../api/client';
import { Ticket, TicketMessage, Citation, Attachment } from '../types';

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
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [isUploadingAttachment, setIsUploadingAttachment] = useState(false);

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

  const loadAttachments = useCallback(async (ticketId: string) => {
    try { setAttachments(await api.listCustomerAttachments(ticketId)); }
    catch { setAttachments([]); }
  }, []);

  useEffect(() => {
    if (currentTab === 'my-tickets') {
      loadTickets();
    }
  }, [currentTab, loadTickets]);

  useEffect(() => {
    if (selectedTicket) {
      loadMessages(selectedTicket.id);
      loadAttachments(selectedTicket.id);
    } else {
      setMessages([]);
      setAttachments([]);
    }
  }, [selectedTicket, loadMessages, loadAttachments]);

  const handleAttachment = async (file?: File) => {
    if (!selectedTicket || !file) return;
    setIsUploadingAttachment(true); setTicketError(null);
    try {
      const uploaded = await api.uploadCustomerAttachment(selectedTicket.id, file);
      setAttachments(previous => [...previous, uploaded]);
    } catch (failure) {
      setTicketError(failure instanceof Error ? failure.message : 'Unable to upload attachment');
    } finally { setIsUploadingAttachment(false); }
  };

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

  const pageCopy = {
    create: {
      eyebrow: 'Customer portal',
      title: 'How can we help?',
      description: 'Tell us what happened and we’ll route your request to the right support team.',
    },
    'my-tickets': {
      eyebrow: 'Your support',
      title: 'My tickets',
      description: 'Follow active requests and keep every conversation in one place.',
    },
    help: {
      eyebrow: 'Self-service',
      title: 'Help center',
      description: 'Find verified answers from ResolveIQ’s approved support knowledge.',
    },
  }[currentTab];

  return (
    <div className="app-page max-w-6xl">
      <header className="page-header">
        <div>
          <span className="eyebrow">{pageCopy.eyebrow}</span>
          <h1 className="page-title">{pageCopy.title}</h1>
          <p className="page-description">{pageCopy.description}</p>
        </div>
        {currentTab !== 'create' && (
          <button
            onClick={() => { setTab('create'); setSelectedTicket(null); }}
            className="btn-primary self-start sm:self-auto"
          >
            <PlusCircle className="h-4 w-4" />
            New ticket
          </button>
        )}
      </header>

      {ticketError && (
        <div role="alert" className="flex items-center gap-2 rounded-card border border-danger/20 bg-danger/10 p-3.5 text-xs text-danger">
          <AlertCircle className="h-4 w-4 flex-none" />
          <span>{ticketError}</span>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 1. CREATE TICKET VIEW */}
      {/* ========================================================================= */}
      {currentTab === 'create' && (
        submittedTicket ? (
          <div className="panel mx-auto max-w-3xl p-6 sm:p-8">
            <div className="grid h-11 w-11 place-items-center rounded-full bg-success/10 text-success">
              <CheckCircle2 className="h-5 w-5" />
            </div>
            <h2 className="mt-4 text-xl font-semibold tracking-[-0.02em] text-DEFAULT">Your request is with us</h2>
            <p className="mt-2 text-sm leading-6 text-muted">
              Ticket <span className="font-mono font-semibold text-DEFAULT">{submittedTicket.ticketNumber}</span> has been created. We’ll keep you updated as it moves through support.
            </p>
            <div className="mt-6 space-y-2 rounded-card border border-border-subtle bg-surface-muted p-4 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-medium text-muted">Subject</span>
                <span className="max-w-[65%] truncate font-semibold text-DEFAULT">{submittedTicket.subject}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-muted">Category / priority</span>
                <span className="font-semibold text-DEFAULT">{submittedTicket.category} / {submittedTicket.priority}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-muted">Status</span>
                <span className="font-semibold text-primary">{submittedTicket.status}</span>
              </div>
            </div>
            <div className="mt-3 flex items-start gap-2.5 rounded-btn bg-ai-soft px-3.5 py-3 text-xs leading-5 text-ai">
              <Clock className="mt-0.5 h-4 w-4 flex-none" />
              <span>We’re reviewing the details and routing your request to the right specialist.</span>
            </div>
            <div className="mt-6 flex flex-wrap gap-3">
              <button
                onClick={() => {
                  setSubmittedTicket(null);
                  setSubject('');
                  setDescription('');
                }}
                className="btn-primary"
              >
                Submit another request
              </button>
              <button
                onClick={() => {
                  setSubmittedTicket(null);
                  setTab('my-tickets');
                }}
                className="btn-secondary"
              >
                View my tickets
              </button>
            </div>
          </div>
        ) : (
          <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_280px]">
          <form onSubmit={handleSubmitTicket} className="panel space-y-5 p-5 sm:p-7">
            <div className="border-b border-border-subtle pb-5">
              <h2 className="text-base font-semibold text-DEFAULT">Tell us what happened</h2>
              <p className="mt-1 text-xs leading-5 text-muted">Include the outcome you expected and any useful IDs or error messages.</p>
            </div>
            
            <div>
              <label className="field-label">
                Subject <span className="text-danger" aria-hidden="true">*</span>
              </label>
              <input
                type="text"
                required
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="For example, I can’t access my payment history"
                className="form-control h-11"
              />
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label className="field-label">Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="form-control h-11"
                >
                  <option value="BILLING">Billing & Payments</option>
                  <option value="TECHNICAL">Technical Support / SSO</option>
                  <option value="ACCOUNT">Account Management</option>
                  <option value="DELIVERY">Delivery & Logistics</option>
                  <option value="GENERAL">General Inquiries</option>
                </select>
              </div>
              <div>
                <span className="field-label">Routing</span>
                <div className="flex h-11 items-center rounded-input border border-border-subtle bg-surface-muted px-3.5 text-xs text-muted">
                  Automatically matched to a support team
                </div>
              </div>
            </div>

            <div>
              <label className="field-label">
                Details <span className="text-danger" aria-hidden="true">*</span>
              </label>
              <textarea
                required
                rows={5}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What happened? When did it start? Include any invoice numbers, error codes, or steps we can reproduce."
                className="form-control min-h-36 resize-y p-3.5 leading-6"
              />
              <p className="mt-1.5 text-right text-[10px] text-muted">{description.length} characters</p>
            </div>

            <div className="flex flex-col-reverse gap-3 border-t border-border-subtle pt-5 sm:flex-row sm:items-center sm:justify-between">
              <button
                type="button"
                onClick={() => setTab('help')}
                className="btn-ghost justify-start px-0 text-xs text-primary hover:bg-transparent"
              >
                <HelpCircle className="h-3.5 w-3.5" />
                Search the help center first
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="btn-primary"
              >
                {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                <span>{isSubmitting ? 'Submitting…' : 'Submit request'}</span>
              </button>
            </div>
          </form>
          <aside className="panel-flat p-5 lg:sticky lg:top-20">
            <span className="eyebrow">What happens next</span>
            <ol className="space-y-5">
              {[
                ['1', 'We review the details', 'Your request is categorized and checked for urgency.'],
                ['2', 'A specialist takes ownership', 'We route it to the team best equipped to help.'],
                ['3', 'You stay in the loop', 'Replies and status changes appear in My tickets.'],
              ].map(([number, title, copy]) => (
                <li key={number} className="flex gap-3">
                  <span className="grid h-6 w-6 flex-none place-items-center rounded-full bg-primary-soft text-[10px] font-semibold text-primary">{number}</span>
                  <div>
                    <p className="text-xs font-semibold text-DEFAULT">{title}</p>
                    <p className="mt-1 text-[11px] leading-4 text-muted">{copy}</p>
                  </div>
                </li>
              ))}
            </ol>
            <div className="mt-5 border-t border-border-subtle pt-4 text-[11px] leading-4 text-muted">
              Avoid including passwords, full card numbers, or other sensitive credentials.
            </div>
          </aside>
          </div>
        )
      )}

      {/* ========================================================================= */}
      {/* 2. MY TICKETS LIST & CONVERSATION VIEW */}
      {/* ========================================================================= */}
      {currentTab === 'my-tickets' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="section-title">Support requests</h2>
              <p className="mt-1 text-[11px] text-muted">{tickets.length} tickets in this workspace</p>
            </div>
            <button
              onClick={loadTickets}
              disabled={isLoadingTickets}
              className="btn-ghost min-h-9 text-xs"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isLoadingTickets ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>

          {selectedTicket ? (
            /* Ticket Conversation View */
            <div className="panel space-y-5 p-5 sm:p-6">
              <div className="flex flex-col-reverse gap-4 border-b border-border-subtle pb-5 sm:flex-row sm:items-start sm:justify-between">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-xs font-bold text-DEFAULT">{selectedTicket.ticketNumber}</span>
                    <span className="status-chip border-border bg-surface-muted text-muted">
                      {selectedTicket.status.replace(/_/g, ' ')}
                    </span>
                    <span className="status-chip border-primary/20 bg-primary/10 text-primary">
                      {selectedTicket.priority}
                    </span>
                  </div>
                  <h3 className="text-lg font-semibold tracking-[-0.02em] text-DEFAULT">{selectedTicket.subject}</h3>
                  <p className="text-[11px] text-muted">Created {new Date(selectedTicket.createdAt).toLocaleString()}</p>
                </div>
                <button
                  onClick={() => setSelectedTicket(null)}
                  className="btn-ghost min-h-8 self-start px-0 text-xs text-primary hover:bg-transparent"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back to tickets</span>
                </button>
              </div>

              {/* Initial Ticket Description */}
              <div className="rounded-card border border-border-subtle bg-surface-muted p-4">
                <span className="eyebrow mb-1.5 text-muted">Original request</span>
                <p className="whitespace-pre-wrap text-xs leading-5 text-DEFAULT">{selectedTicket.description}</p>
              </div>

              {/* Message Thread */}
              <div className="space-y-3 pt-2">
                <h4 className="section-title">Conversation</h4>
                {isLoadingMessages ? (
                  <div className="py-4 text-center text-xs text-muted flex items-center justify-center space-x-2">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Loading messages...</span>
                  </div>
                ) : messages.length === 0 ? (
                  <div className="rounded-card border border-dashed border-border bg-surface-muted p-6 text-center text-xs text-muted">
                    No replies yet. An agent or automated update will appear here shortly.
                  </div>
                ) : (
                  messages.map((m) => (
                    <div
                      key={m.id}
                      className={`space-y-1 rounded-card border p-3.5 text-xs ${
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

              <div className="space-y-3 border-t border-border-subtle pt-4">
                <div className="flex items-center justify-between">
                  <span className="section-title">Attachments</span>
                  <label className="btn-secondary min-h-9 cursor-pointer px-3 text-xs">
                    <Paperclip className="w-3.5 h-3.5" />
                    <span>{isUploadingAttachment ? 'Scanning…' : 'Attach file'}</span>
                    <input type="file" className="hidden" disabled={isUploadingAttachment}
                      accept=".pdf,.png,.jpg,.jpeg,.txt,.json" onChange={event => void handleAttachment(event.target.files?.[0])} />
                  </label>
                </div>
                {attachments.length === 0 ? (
                  <p className="text-xs text-muted">No files attached. Allowed: PDF, PNG, JPEG, TXT, JSON; maximum 10 MiB.</p>
                ) : attachments.map(attachment => (
                  <button key={attachment.id}
                    onClick={() => void api.downloadAttachment('customer', selectedTicket.id, attachment.id, attachment.fileName)}
                    className="flex w-full items-center justify-between rounded-btn border border-border-subtle bg-surface-muted p-2.5 text-xs hover:border-primary/40">
                    <span className="flex items-center space-x-2"><Paperclip className="w-3.5 h-3.5 text-primary" /><span>{attachment.fileName}</span></span>
                    <span className="flex items-center space-x-2 text-success"><span>{attachment.scanStatus}</span><Download className="w-3.5 h-3.5" /></span>
                  </button>
                ))}
              </div>

              {/* Reply Box */}
              <div className="space-y-3 border-t border-border-subtle pt-4">
                <label className="section-title">Reply to support</label>
                <textarea
                  rows={3}
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  placeholder="Provide additional details or response..."
                  className="form-control min-h-28 resize-y p-3.5 leading-6"
                />
                <div className="flex justify-end">
                  <button
                    onClick={handleSendReply}
                    disabled={isSendingReply || !replyText.trim()}
                    className="btn-primary"
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
                <div className="panel-flat flex items-center justify-center gap-2 p-10 text-center text-xs text-muted">
                  <Loader2 className="w-4 h-4 animate-spin text-primary" />
                  <span>Loading support tickets...</span>
                </div>
              )}

              {!isLoadingTickets && tickets.length === 0 && (
                <div className="panel space-y-3 p-10 text-center">
                  <div className="mx-auto grid h-11 w-11 place-items-center rounded-full bg-primary/10 text-primary">
                    <Inbox className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-DEFAULT">No tickets yet</p>
                    <p className="mt-1 text-xs text-muted">When you submit a request, its progress will appear here.</p>
                  </div>
                  <button
                    onClick={() => setTab('create')}
                    className="btn-primary"
                  >
                    Create your first ticket
                  </button>
                </div>
              )}

              {!isLoadingTickets && tickets.map((t) => (
                <button
                  type="button"
                  key={t.id}
                  onClick={() => setSelectedTicket(t)}
                  className="panel-flat group flex w-full items-center justify-between p-4 text-left transition-[border-color,box-shadow] hover:border-primary/40 hover:shadow-sm"
                >
                  <div className="space-y-1.5 flex-1 pr-4">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-[11px] font-semibold text-DEFAULT">{t.ticketNumber}</span>
                      <span className={`status-chip ${
                        t.status === 'RESOLVED' || t.status === 'CLOSED'
                          ? 'bg-success/10 text-success border-success/20'
                          : t.status === 'READY_FOR_AGENT' || t.status === 'IN_PROGRESS'
                          ? 'bg-primary/10 text-primary border-primary/20'
                          : 'bg-surface-muted text-muted border-border'
                      }`}>
                        {t.status.replace(/_/g, ' ')}
                      </span>
                      {t.category && (
                        <span className="rounded-full bg-surface-muted px-2.5 py-1 text-[10px] font-medium text-muted">
                          {t.category}
                        </span>
                      )}
                    </div>
                    <p className="line-clamp-1 text-sm font-medium text-DEFAULT transition-colors group-hover:text-primary">
                      {t.subject}
                    </p>
                    <p className="text-[11px] text-muted">
                      Created {new Date(t.createdAt).toLocaleDateString()} at {new Date(t.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </p>
                  </div>
                  <div className="flex items-center space-x-2 text-xs font-medium text-primary flex-shrink-0">
                    <MessageSquare className="h-4 w-4" />
                    <span className="hidden sm:inline">View</span>
                    <ChevronRight className="w-4 h-4 text-muted group-hover:text-primary transition-colors" />
                  </div>
                </button>
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
          <section className="panel space-y-5 p-5 sm:p-7">
            <div className="max-w-2xl">
              <h2 className="flex items-center gap-2 text-lg font-semibold tracking-[-0.02em] text-DEFAULT">
                <BookOpen className="h-5 w-5 text-primary" />
                <span>What do you need help with?</span>
              </h2>
              <p className="mt-1.5 text-xs leading-5 text-muted">
                Search concise, verified guidance written and approved by our support team.
              </p>
            </div>

            <form onSubmit={(e) => handleHelpSearch(e)} className="flex flex-col gap-2 sm:flex-row">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-muted" />
                <input
                  type="search"
                  value={helpQuery}
                  onChange={(e) => setHelpQuery(e.target.value)}
                  placeholder="Search billing, login, integrations, or account access"
                  aria-label="Search help articles"
                  className="form-control h-11 pl-10"
                />
              </div>
              <button
                type="submit"
                disabled={isSearchingHelp || !helpQuery.trim()}
                className="btn-primary h-11 flex-none"
              >
                {isSearchingHelp && <Loader2 className="h-4 w-4 animate-spin" />}
                <span>{isSearchingHelp ? 'Searching…' : 'Search articles'}</span>
              </button>
            </form>

            {helpError && (
              <div role="alert" className="rounded-card border border-danger/20 bg-danger/10 p-3 text-xs text-danger">
                {helpError}
              </div>
            )}
          </section>

          {/* Search Results Display */}
          {helpResults && (
            <section className="panel space-y-4 p-5">
              <div className="flex items-center justify-between border-b border-border-subtle pb-4">
                <h3 className="flex items-center gap-2 text-sm font-semibold text-DEFAULT">
                  <Search className="h-4 w-4 text-primary" />
                  <span>{helpResults.length} matching articles</span>
                </h3>
                <button
                  onClick={() => { setHelpResults(null); setSelectedArticle(null); }}
                  className="btn-ghost min-h-8 text-xs"
                >
                  Clear Results
                </button>
              </div>

              {selectedArticle ? (
                /* Full Article Detail Card */
                <article className="space-y-3 rounded-card border border-border-subtle bg-surface-muted p-4">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-semibold uppercase tracking-[0.08em] text-primary">{selectedArticle.sourceType}</span>
                    <button
                      onClick={() => setSelectedArticle(null)}
                      className="text-xs text-primary font-semibold hover:underline"
                    >
                      Back to Results
                    </button>
                  </div>
                  <h4 className="text-base font-semibold text-DEFAULT">{selectedArticle.title}</h4>
                  <div className="whitespace-pre-wrap rounded-card border border-border-subtle bg-surface p-4 text-xs leading-5 text-DEFAULT">
                    {selectedArticle.citationText || selectedArticle.snippet}
                  </div>
                </article>
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
                <div className="divide-y divide-border-subtle">
                  {helpResults.map((art, idx) => (
                    <button
                      type="button"
                      key={idx}
                      onClick={() => setSelectedArticle(art)}
                      className="w-full py-4 text-left first:pt-0 last:pb-0"
                    >
                      <div className="flex items-center justify-between">
                        <h4 className="text-sm font-semibold text-DEFAULT hover:text-primary">{art.title}</h4>
                        {art.confidenceScore !== undefined && (
                          <span className="rounded-full bg-primary-soft px-2 py-1 font-mono text-[10px] text-primary">
                            {(art.confidenceScore * 100).toFixed(1)}% match
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-muted line-clamp-2">{art.snippet || art.citationText}</p>
                    </button>
                  ))}
                </div>
              )}
            </section>
          )}

          {/* Curated Help Topics */}
          <div>
            <h3 className="section-title mb-3">Browse popular topics</h3>
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              {curatedTopics.map((topic, idx) => {
                const IconComponent = topic.icon;
                return (
                  <button
                    type="button"
                    key={idx}
                    onClick={() => handleHelpSearch(undefined, topic.query)}
                    className="panel-flat group space-y-3 p-4 text-left transition-[border-color,box-shadow] hover:border-primary/40 hover:shadow-sm"
                  >
                    <div className="flex items-center space-x-3">
                      <div className="grid h-9 w-9 place-items-center rounded-input bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                        <IconComponent className="h-4 w-4" />
                      </div>
                      <h4 className="text-sm font-semibold text-DEFAULT group-hover:text-primary transition-colors">
                        {topic.title}
                      </h4>
                    </div>
                    <p className="text-xs leading-5 text-muted">{topic.desc}</p>
                    <div className="flex items-center gap-1 pt-1 text-xs font-medium text-primary">
                      <span>View guidance</span>
                      <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Still Need Help Banner */}
          <div className="panel-flat flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center">
            <div>
              <h4 className="text-sm font-semibold text-DEFAULT">Still need help?</h4>
              <p className="mt-1 text-xs text-muted">Send us the details and our support team will take it from here.</p>
            </div>
            <button
              onClick={() => setTab('create')}
              className="btn-primary flex-none"
            >
              Create a ticket
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
