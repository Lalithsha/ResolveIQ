import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle, BookOpen, ChevronLeft, ChevronRight, Download, Inbox, Loader2, Paperclip,
  RefreshCw, Search, Send, ShieldCheck, Sparkles, ThumbsDown, ThumbsUp, Users,
} from 'lucide-react';
import { api } from '../api/client';
import {
  AgentTicketContext, AiSuggestion, Attachment, Citation, Role, RoutingAgent, Team, TicketPriority,
  TicketQueueResponse, TicketStatus, User,
} from '../types';
import { useAuth } from '../context/AuthContext';

interface AgentWorkspaceProps { activeTab?: string; role?: Role; }
const EMPTY_PAGE: TicketQueueResponse = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

export const AgentWorkspace: React.FC<AgentWorkspaceProps> = ({ activeTab = 'my-queue', role = 'AGENT' }) => {
  const { user } = useAuth();
  const [queue, setQueue] = useState<TicketQueueResponse>(EMPTY_PAGE);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [context, setContext] = useState<AgentTicketContext | null>(null);
  const [customer, setCustomer] = useState<User | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [agents, setAgents] = useState<RoutingAgent[]>([]);
  const [selectedTeam, setSelectedTeam] = useState('');
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [activeDraft, setActiveDraft] = useState('');
  const [feedbackGiven, setFeedbackGiven] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [loadingQueue, setLoadingQueue] = useState(true);
  const [loadingContext, setLoadingContext] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [showRejection, setShowRejection] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [search, setSearch] = useState('');
  const [priority, setPriority] = useState('');
  const [status, setStatus] = useState('');
  const [sort, setSort] = useState('createdAt');
  const [page, setPage] = useState(0);

  const scope = useMemo<'mine' | 'team' | 'all' | 'sla-risk'>(() => {
    if (role === 'ADMIN' || role === 'AUDITOR') return activeTab === 'sla-risk' ? 'sla-risk' : 'all';
    if (activeTab === 'team-queue' || role === 'TEAM_LEAD') return 'team';
    if (activeTab === 'sla-risk') return 'sla-risk';
    return 'mine';
  }, [activeTab, role]);
  const canAssign = role === 'TEAM_LEAD' || role === 'ADMIN';
  const readOnly = role === 'AUDITOR';

  useEffect(() => {
    Promise.all([api.listTeams(), api.listRoutingAgents()])
      .then(([teamData, agentData]) => {
        setTeams(teamData); setAgents(agentData);
        const ownTeam = agentData.find(agent => agent.id === user?.id)?.teamId;
        setSelectedTeam(previous => previous || ownTeam || teamData[0]?.id || '');
      })
      .catch(failure => setStatusMessage(failure instanceof Error ? failure.message : 'Unable to load routing directory'));
  }, [user?.id]);

  const loadQueue = useCallback(async (keepSelection = true) => {
    if ((scope === 'team' || scope === 'sla-risk') && role !== 'ADMIN' && role !== 'AUDITOR' && !selectedTeam && teams.length > 0) return;
    setLoadingQueue(true);
    try {
      const result = await api.searchAgentQueue({
        scope, teamId: selectedTeam || undefined, status: status || undefined, priority: priority || undefined,
        query: search || undefined, sort, direction: 'desc', page, size: 20,
      });
      setQueue(result);
      setSelectedId(previous => keepSelection && result.items.some(ticket => ticket.id === previous)
        ? previous : result.items[0]?.id || null);
    } catch (failure) {
      setQueue(EMPTY_PAGE); setSelectedId(null);
      setStatusMessage(failure instanceof Error ? failure.message : 'Unable to load ticket queue');
    } finally { setLoadingQueue(false); }
  }, [scope, selectedTeam, status, priority, search, sort, page, role, teams.length]);

  useEffect(() => { void loadQueue(false); }, [loadQueue]);
  useEffect(() => {
    if (!selectedId) { setContext(null); setCustomer(null); setAttachments([]); setActiveDraft(''); return; }
    setLoadingContext(true); setFeedbackGiven(null);
    Promise.all([api.getAgentTicketContext(selectedId), api.listAgentAttachments(selectedId)])
      .then(async ([ticketContext, files]) => {
        setContext(ticketContext); setAttachments(files);
        const latest = ticketContext.suggestions[0]; setActiveDraft(latest?.suggestedResponse || '');
        if (latest && latest.status !== 'PENDING_REVIEW') setFeedbackGiven(latest.status);
        try { setCustomer(await api.getDirectoryUser(ticketContext.ticket.customerId)); } catch { setCustomer(null); }
      })
      .catch(failure => setStatusMessage(failure instanceof Error ? failure.message : 'Unable to load ticket context'))
      .finally(() => setLoadingContext(false));
  }, [selectedId]);

  useEffect(() => {
    const controller = new AbortController();
    void api.followTicketEvents(() => void loadQueue(true), controller.signal)
      .catch(error => { if (!controller.signal.aborted) setStatusMessage(error instanceof Error ? error.message : 'Live updates unavailable'); });
    return () => controller.abort();
  }, [loadQueue]);

  const suggestion = context?.suggestions[0];
  const citations = useMemo(() => parseCitations(suggestion?.citations), [suggestion?.citations]);
  const similarCases = citations.filter(value => value.sourceType === 'RESOLVED_CASE');
  const teamName = teams.find(team => team.id === context?.ticket.teamId)?.name;
  const agentName = agents.find(agent => agent.id === context?.ticket.assignedAgentId)?.name;

  const handleFeedback = async (action: 'ACCEPTED' | 'EDITED' | 'REJECTED', rejectionReasonOverride?: string) => {
    if (!context || !suggestion || feedbackGiven) return;
    if (action === 'REJECTED' && !rejectionReasonOverride?.trim()) {
      setShowRejection(true);
      return;
    }
    try {
      await api.recordFeedback(context.ticket.id, { suggestionId: suggestion.id, action,
        rejectionReason: rejectionReasonOverride?.trim() || undefined, editedContent: action === 'EDITED' ? activeDraft : undefined });
      setFeedbackGiven(action); setStatusMessage(`Feedback recorded: ${action}`); setShowRejection(false); setRejectionReason('');
    } catch (failure) { setStatusMessage(failure instanceof Error ? failure.message : 'Unable to record feedback'); }
  };

  const handleApproveAndSend = async () => {
    if (!context || !activeDraft.trim() || readOnly) return;
    setIsSending(true); setStatusMessage(null);
    try {
      if (suggestion && !feedbackGiven) {
        const action = activeDraft === suggestion.suggestedResponse ? 'ACCEPTED' : 'EDITED';
        await api.recordFeedback(context.ticket.id, { suggestionId: suggestion.id, action,
          editedContent: action === 'EDITED' ? activeDraft : undefined }); setFeedbackGiven(action);
      }
      await api.addAgentMessage(context.ticket.id, activeDraft, false);
      await api.updateTicketStatus(context.ticket.id, 'WAITING_ON_CUSTOMER', 'Agent response approved and sent.');
      setStatusMessage('Response sent. Ticket moved to WAITING_ON_CUSTOMER.'); await loadQueue(true);
    } catch (failure) { setStatusMessage(failure instanceof Error ? failure.message : 'Unable to send response'); }
    finally { setIsSending(false); }
  };

  async function handleUpload(file?: File): Promise<void> {
    if (!context || !file) return;
    setIsUploading(true);
    try {
      const uploaded = await api.uploadAgentAttachment(context.ticket.id, file);
      setAttachments(previous => [...previous, uploaded]);
    }
    catch (failure) { setStatusMessage(failure instanceof Error ? failure.message : 'Unable to upload attachment'); }
    finally { setIsUploading(false); }
  }

  const handleAssign = async (teamId: string, agentId: string) => {
    if (!context || !canAssign) return;
    try {
      const updated = await api.assignTicket(context.ticket.id, teamId || undefined, agentId || undefined);
      setContext(previous => previous ? { ...previous, ticket: updated } : previous);
      setStatusMessage('Ticket assignment updated.'); await loadQueue(true);
    } catch (failure) { setStatusMessage(failure instanceof Error ? failure.message : 'Assignment failed'); }
  };

  return <div className="flex min-h-[calc(100vh-4rem)] flex-col overflow-x-hidden bg-background xl:grid xl:grid-cols-[300px_minmax(0,1fr)] 2xl:h-[calc(100vh-4rem)] 2xl:grid-cols-[300px_minmax(0,1fr)_340px] 2xl:overflow-hidden">
    <aside className="flex max-h-[420px] w-full flex-col border-b border-border-subtle bg-surface xl:max-h-none xl:border-b-0 xl:border-r">
      <div className="p-3 border-b border-border space-y-2">
        <div className="flex items-center justify-between"><span className="text-sm font-bold flex items-center gap-2">{scope === 'team' ? <Users className="w-4 h-4" /> : <Inbox className="w-4 h-4" />} Queue</span><button aria-label="Refresh queue" onClick={() => void loadQueue(true)}><RefreshCw className={`w-4 h-4 text-muted ${loadingQueue ? 'animate-spin' : ''}`} /></button></div>
        <div className="relative"><Search className="w-3.5 h-3.5 text-muted absolute left-2.5 top-2.5" /><input value={search} onChange={event => { setSearch(event.target.value); setPage(0); }} placeholder="Search tickets" className="w-full pl-8 pr-2 py-2 text-xs bg-surface-muted border border-border rounded-input" /></div>
        <div className="grid grid-cols-2 gap-2"><select value={priority} onChange={event => { setPriority(event.target.value); setPage(0); }} className="text-xs bg-surface border border-border rounded-input px-2 py-1.5"><option value="">All priorities</option>{(['LOW','MEDIUM','HIGH','CRITICAL'] as TicketPriority[]).map(value => <option key={value}>{value}</option>)}</select><select value={status} onChange={event => { setStatus(event.target.value); setPage(0); }} className="text-xs bg-surface border border-border rounded-input px-2 py-1.5"><option value="">All statuses</option>{(['NEW','READY_FOR_AGENT','IN_PROGRESS','WAITING_ON_CUSTOMER','RESOLVED','TRIAGE_FAILED'] as TicketStatus[]).map(value => <option key={value}>{value}</option>)}</select></div>
        {(scope === 'team' || scope === 'sla-risk') && teams.length > 0 && <select value={selectedTeam} onChange={event => { setSelectedTeam(event.target.value); setPage(0); }} className="w-full text-xs bg-surface border border-border rounded-input px-2 py-1.5">{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select>}
        <select value={sort} onChange={event => setSort(event.target.value)} className="w-full text-xs bg-surface border border-border rounded-input px-2 py-1.5"><option value="createdAt">Newest first</option><option value="updatedAt">Recently updated</option><option value="firstResponseDueAt">SLA deadline</option><option value="priority">Priority</option></select>
      </div>
      <div className="flex-1 overflow-y-auto divide-y divide-border-subtle">{loadingQueue && <div className="p-8 text-xs text-muted text-center"><Loader2 className="w-4 h-4 animate-spin mx-auto mb-2" />Loading queue…</div>}{!loadingQueue && queue.items.length === 0 && <div className="p-8 text-xs text-muted text-center">No authorized tickets match these filters.</div>}{queue.items.map(ticket => <button key={ticket.id} onClick={() => setSelectedId(ticket.id)} className={`w-full text-left p-3 space-y-1 hover:bg-surface-muted ${selectedId === ticket.id ? 'bg-primary-soft border-l-2 border-primary' : ''}`}><div className="flex items-center justify-between"><span className="font-mono text-[11px] font-bold">{ticket.ticketNumber}</span><span className={`text-[10px] font-bold ${ticket.priority === 'CRITICAL' || ticket.priority === 'HIGH' ? 'text-danger' : 'text-muted'}`}>{ticket.priority}</span></div><p className="text-xs font-semibold line-clamp-2">{ticket.subject}</p><div className="flex justify-between text-[10px] text-muted"><span>{ticket.status.replace(/_/g, ' ')}</span><span>{slaLabel(ticket.firstResponseDueAt)}</span></div></button>)}</div>
      <div className="p-2 border-t border-border flex items-center justify-between text-xs text-muted"><button disabled={page === 0} onClick={() => setPage(value => Math.max(0, value - 1))}><ChevronLeft className="w-4 h-4" /></button><span>{queue.totalElements} tickets · page {queue.page + 1}/{Math.max(queue.totalPages, 1)}</span><button disabled={page + 1 >= queue.totalPages} onClick={() => setPage(value => value + 1)}><ChevronRight className="w-4 h-4" /></button></div>
    </aside>

    <main className="flex min-h-[680px] min-w-0 flex-1 flex-col">
      {statusMessage && <div className="m-3 mb-0 p-3 bg-primary/10 border border-primary/20 rounded-card text-xs flex justify-between"><span>{statusMessage}</span><button onClick={() => setStatusMessage(null)} className="font-bold">Dismiss</button></div>}
      {!selectedId && !loadingQueue && <div className="flex-1 grid place-items-center text-sm text-muted">Select a ticket from the authorized queue.</div>}{loadingContext && <div className="flex-1 grid place-items-center"><Loader2 className="w-5 h-5 animate-spin" /></div>}
      {context && !loadingContext && <><div className="space-y-4 border-b border-border-subtle bg-surface p-5"><div className="flex justify-between gap-3"><div><div className="font-mono text-[11px] font-semibold text-muted">{context.ticket.ticketNumber}</div><h1 className="mt-1 text-lg font-semibold tracking-[-0.02em]">{context.ticket.subject}</h1></div><div className="text-right text-xs"><span className="status-chip border-danger/20 bg-danger/10 text-danger">{context.ticket.priority}</span><p className="text-muted mt-2">{context.ticket.status.replace(/_/g, ' ')}</p></div></div><div className="grid gap-3 text-xs sm:grid-cols-3"><Info label="Customer" value={customer?.fullName || `Customer ${context.ticket.customerId.slice(0, 8)}`} /><Info label="Team / Assignee" value={`${teamName || 'Not assigned'} / ${agentName || 'Not assigned'}`} /><Info label="First-response SLA" value={slaLabel(context.ticket.firstResponseDueAt)} /></div>{canAssign && <div className="flex flex-wrap gap-2"><select value={context.ticket.teamId || ''} onChange={event => void handleAssign(event.target.value, '')} className="form-control h-9 w-auto text-xs"><option value="">Unassigned team</option>{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select><select value={context.ticket.assignedAgentId || ''} onChange={event => void handleAssign(context.ticket.teamId || '', event.target.value)} className="form-control h-9 w-auto text-xs"><option value="">Unassigned agent</option>{agents.filter(agent => !context.ticket.teamId || agent.teamId === context.ticket.teamId).map(agent => <option key={agent.id} value={agent.id}>{agent.name} ({agent.activeTicketCount})</option>)}</select></div>}</div>
        <div className="flex-1 p-5 overflow-y-auto space-y-3">{context.messages.length === 0 && <div className="p-4 bg-surface border border-border rounded-card text-sm">{context.ticket.description}</div>}{context.messages.map(message => <div key={message.id} className={`max-w-2xl p-3 rounded-card border text-sm ${message.senderRole === 'CUSTOMER' ? 'bg-surface border-border' : 'ml-auto bg-primary/5 border-primary/20'}`}><div className="flex justify-between text-[11px] text-muted mb-1"><b className="text-DEFAULT">{message.senderRole === 'CUSTOMER' ? customer?.fullName || 'Customer' : 'Support Agent'}</b><span>{new Date(message.createdAt).toLocaleString()}</span></div><p className="whitespace-pre-wrap">{message.content}</p></div>)}<div className="max-w-2xl space-y-2"><div className="flex justify-between"><span className="text-xs font-semibold">Attachments</span>{!readOnly && <label className="text-xs font-semibold cursor-pointer flex gap-1"><Paperclip className="w-3.5 h-3.5" />{isUploading ? 'Scanning…' : 'Attach'}<input type="file" className="hidden" disabled={isUploading} accept=".pdf,.png,.jpg,.jpeg,.txt,.json" onChange={event => void handleUpload(event.target.files?.[0])} /></label>}</div>{attachments.length === 0 ? <p className="text-xs text-muted">No clean attachments.</p> : attachments.map(file => <button key={file.id} onClick={() => void api.downloadAttachment('agent', context.ticket.id, file.id, file.fileName)} className="w-full flex justify-between p-2 bg-surface border border-border rounded-btn text-xs"><span>{file.fileName}</span><span className="text-success flex gap-1">{file.scanStatus}<Download className="w-3.5 h-3.5" /></span></button>)}</div></div>
        {!readOnly && <div className="space-y-3 border-t border-border-subtle bg-surface p-4"><div className="flex justify-between text-xs"><b>Response</b><span className="text-muted">Human approval required</span></div><textarea rows={5} value={activeDraft} onChange={event => setActiveDraft(event.target.value)} placeholder={suggestion ? 'Review the grounded draft' : 'No AI draft is available; write a response'} className="form-control min-h-32 resize-y p-3.5 leading-6" /><div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><span className="text-xs text-muted">No message is sent until you approve it.</span><button disabled={isSending || !activeDraft.trim()} onClick={() => void handleApproveAndSend()} className="btn-primary">{isSending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}Approve & send</button></div></div>}</>}
    </main>

    <aside className="w-full space-y-5 overflow-y-auto border-t border-border-subtle bg-surface p-5 xl:col-span-2 2xl:col-span-1 2xl:border-l 2xl:border-t-0">{!context ? <p className="text-sm text-muted">Ticket evidence appears after selection.</p> : <><div><div className="flex justify-between"><span className="text-sm font-semibold text-ai flex gap-2"><Sparkles className="w-4 h-4" />AI evidence</span><span className="rounded-full bg-ai-soft px-2 py-1 text-[10px] font-mono font-semibold text-ai">{suggestion?.confidenceScore != null ? `${(suggestion.confidenceScore * 100).toFixed(1)}%` : 'No score'}</span></div><p className="text-[11px] text-muted mt-1">{suggestion ? `${suggestion.modelName} · ${suggestion.promptVersion}` : 'No persisted suggestion'}</p></div><div><span className="eyebrow mb-2 text-muted">Classification</span><div className="flex flex-wrap gap-1.5">{[context.ticket.intent, context.ticket.category, context.ticket.sentiment, context.ticket.urgency].filter(Boolean).map(value => <span key={value} className="rounded-full border border-border-subtle bg-surface-muted px-2.5 py-1 text-[10px] font-medium text-muted">{value}</span>)}{!context.ticket.intent && !context.ticket.category && <span className="text-xs text-muted">No classification persisted.</span>}</div></div><div><span className="eyebrow mb-2 text-muted">Grounded citations ({citations.length})</span><div className="space-y-2">{citations.length === 0 ? <p className="text-xs text-muted">No grounded citations were returned.</p> : citations.map((citation, index) => <div key={`${citation.sourceId}-${index}`} className="rounded-card border border-border-subtle bg-surface-muted p-3 text-xs"><b className="flex gap-1.5"><BookOpen className="w-3.5 h-3.5 text-primary" />{citation.title || citation.sourceType}</b><p className="text-muted mt-1.5 line-clamp-4 leading-5">{citation.snippet || citation.citationText}</p></div>)}</div></div><div><span className="eyebrow mb-2 text-muted">Similar resolved cases ({similarCases.length})</span>{similarCases.length === 0 ? <p className="text-xs text-muted">No similar approved case.</p> : similarCases.map(value => <p key={value.sourceId} className="mt-2 rounded-btn bg-surface-muted p-2 text-xs">{value.title}</p>)}</div><div className="flex gap-2 rounded-card border border-success/20 bg-success/10 p-3 text-xs"><ShieldCheck className="w-4 h-4 text-success shrink-0" /><span>Human approval is enforced. AI cannot send this response.</span></div>{!readOnly && suggestion && <div className="border-t border-border-subtle pt-3 space-y-2"><span className="text-xs text-muted">Was this suggestion helpful?</span><div className="grid grid-cols-3 gap-2"><FeedbackButton label="Accept" active={feedbackGiven === 'ACCEPTED'} disabled={!!feedbackGiven} onClick={() => void handleFeedback('ACCEPTED')} icon={<ThumbsUp className="w-3.5 h-3.5" />} /><FeedbackButton label="Edit" active={feedbackGiven === 'EDITED'} disabled={!!feedbackGiven} onClick={() => void handleFeedback('EDITED')} /><FeedbackButton label="Reject" active={feedbackGiven === 'REJECTED'} disabled={!!feedbackGiven} onClick={() => void handleFeedback('REJECTED')} icon={<ThumbsDown className="w-3.5 h-3.5" />} /></div></div>}{showRejection && <div className="space-y-2 rounded-card border border-danger/20 bg-danger/5 p-3"><label htmlFor="suggestion-rejection" className="text-xs font-semibold text-DEFAULT">What should improve?</label><textarea id="suggestion-rejection" rows={3} value={rejectionReason} onChange={event => setRejectionReason(event.target.value)} placeholder="Missing context, unsafe advice, or the wrong source…" className="form-control p-2.5 text-xs" /><div className="flex justify-end gap-2"><button onClick={() => { setShowRejection(false); setRejectionReason(''); }} className="btn-ghost min-h-8 text-xs">Cancel</button><button disabled={!rejectionReason.trim()} onClick={() => void handleFeedback('REJECTED', rejectionReason)} className="min-h-8 rounded-btn bg-danger px-3 text-xs font-semibold text-white disabled:opacity-50">Save feedback</button></div></div>}{context.ticket.status === 'TRIAGE_FAILED' && <div className="p-3 bg-danger/10 border border-danger/20 text-danger text-xs flex gap-2"><AlertTriangle className="w-4 h-4" />Triage failed; no AI evidence should be assumed.</div>}</>}</aside>
  </div>;
};

const Info: React.FC<{ label: string; value: string }> = ({ label, value }) => <div><span className="text-muted">{label}</span><p className="font-semibold mt-0.5 truncate">{value}</p></div>;
const FeedbackButton: React.FC<{ label: string; active: boolean; disabled: boolean; onClick: () => void; icon?: React.ReactNode }> = ({ label, active, disabled, onClick, icon }) => <button disabled={disabled} onClick={onClick} className={`py-1.5 px-2 border rounded-btn text-xs font-semibold flex justify-center gap-1 disabled:opacity-60 ${active ? 'bg-primary text-white border-primary' : 'border-border'}`}>{icon}{label}</button>;

function parseCitations(value: AiSuggestion['citations'] | undefined): Citation[] {
  if (!value) return [];
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value;
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item: Record<string, unknown>, index: number) => ({
      sourceType: (item.sourceType as Citation['sourceType']) || (String(item.source || '').toLowerCase().includes('case') ? 'RESOLVED_CASE' : 'KNOWLEDGE_ARTICLE'),
      sourceId: String(item.sourceId || item.chunkId || `citation-${index}`), versionId: item.versionId ? String(item.versionId) : undefined,
      chunkId: item.chunkId ? String(item.chunkId) : undefined, title: String(item.title || item.source || 'Grounded source'),
      citationText: String(item.citationText || item.snippet || ''), snippet: item.snippet ? String(item.snippet) : undefined,
      score: typeof item.score === 'number' ? item.score : undefined,
    }));
  } catch { return []; }
}

function slaLabel(timestamp?: string): string {
  if (!timestamp) return 'No SLA deadline';
  const delta = new Date(timestamp).getTime() - Date.now(); const minutes = Math.round(Math.abs(delta) / 60000);
  if (delta < 0) return `Breached ${minutes}m ago`; if (minutes < 60) return `Due in ${minutes}m`;
  return `Due in ${Math.round(minutes / 60)}h`;
}
