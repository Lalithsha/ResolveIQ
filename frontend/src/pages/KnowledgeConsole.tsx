import React, { useCallback, useEffect, useState } from 'react';
import { Archive, BookOpen, CheckCircle2, FileCheck2, Loader2, Plus, RotateCcw, Search, Send, XCircle } from 'lucide-react';
import { api } from '../api/client';
import { Citation, KnowledgeDocument, KnowledgeVersion, ResolvedCase, Role } from '../types';

interface Props { activeTab?: string; role?: Role; }

export const KnowledgeConsole: React.FC<Props> = ({ activeTab = 'articles', role = 'KNOWLEDGE_MANAGER' }) => {
  const searchOnly = activeTab === 'knowledge-search' || activeTab === 'embeddings';
  const resolvedOnly = activeTab === 'resolved-cases';
  const readOnly = role === 'AUDITOR';
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [selected, setSelected] = useState<KnowledgeDocument | null>(null);
  const [versions, setVersions] = useState<KnowledgeVersion[]>([]);
  const [resolvedCases, setResolvedCases] = useState<ResolvedCase[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [newVersionFor, setNewVersionFor] = useState<string | null>(null);
  const [rejectingVersionId, setRejectingVersionId] = useState<string | null>(null);
  const [rejectionNote, setRejectionNote] = useState('');
  const [title, setTitle] = useState(''); const [category, setCategory] = useState('GENERAL');
  const [product, setProduct] = useState(''); const [summary, setSummary] = useState(''); const [content, setContent] = useState('');
  const [query, setQuery] = useState(''); const [searching, setSearching] = useState(false);
  const [results, setResults] = useState<Citation[] | null>(null);

  const loadDocuments = useCallback(async () => {
    if (searchOnly || resolvedOnly) return;
    setLoading(true);
    try {
      const data = await api.listKnowledgeDocuments(); setDocuments(data);
      setSelected(previous => data.find(doc => doc.id === previous?.id) || data[0] || null);
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : 'Unable to load knowledge'); }
    finally { setLoading(false); }
  }, [resolvedOnly, searchOnly]);

  useEffect(() => { void loadDocuments(); }, [loadDocuments]);
  useEffect(() => {
    if (!selected) { setVersions([]); return; }
    void api.listKnowledgeVersions(selected.id).then(setVersions)
      .catch(failure => setMessage(failure instanceof Error ? failure.message : 'Unable to load version history'));
  }, [selected]);
  useEffect(() => {
    if (!resolvedOnly) return;
    setLoading(true); void api.listResolvedCases().then(setResolvedCases)
      .catch(failure => setMessage(failure instanceof Error ? failure.message : 'Unable to load approved cases'))
      .finally(() => setLoading(false));
  }, [resolvedOnly]);

  const refreshSelected = async (documentId: string) => {
    const [docs, history] = await Promise.all([api.listKnowledgeDocuments(), api.listKnowledgeVersions(documentId)]);
    setDocuments(docs); setSelected(docs.find(doc => doc.id === documentId) || null); setVersions(history);
  };

  const submitForm = async (event: React.FormEvent) => {
    event.preventDefault(); setLoading(true); setMessage(null);
    try {
      if (newVersionFor) {
        await api.createKnowledgeVersion(newVersionFor, content, summary); await refreshSelected(newVersionFor);
        setMessage('New draft version created.');
      } else {
        const document = await api.createKnowledgeDocument({ title, category, product: product || undefined, language: 'en', content, summary });
        await refreshSelected(document.id); setMessage('Draft article created. It is not searchable until reviewed and published.');
      }
      setFormOpen(false); setNewVersionFor(null); setTitle(''); setProduct(''); setSummary(''); setContent('');
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : 'Unable to save draft'); }
    finally { setLoading(false); }
  };

  const act = async (action: 'submit' | 'publish' | 'reject' | 'rollback', version: KnowledgeVersion) => {
    if (!selected) return; setLoading(true); setMessage(null);
    try {
      if (action === 'submit') await api.submitKnowledgeVersion(selected.id, version.id);
      if (action === 'publish') await api.publishKnowledgeVersion(selected.id, version.id, 'Reviewed and approved in Knowledge Console');
      if (action === 'reject') {
        if (!rejectionNote.trim()) { setRejectingVersionId(version.id); return; }
        await api.rejectKnowledgeVersion(selected.id, version.id, rejectionNote.trim());
      }
      if (action === 'rollback') await api.rollbackKnowledgeVersion(selected.id, version.id);
      await refreshSelected(selected.id); setMessage(`Version ${version.versionNumber} ${action} completed.`);
      setRejectingVersionId(null); setRejectionNote('');
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : `Unable to ${action} version`); }
    finally { setLoading(false); }
  };

  const runSearch = async (event: React.FormEvent) => {
    event.preventDefault(); if (!query.trim()) return; setSearching(true); setMessage(null);
    try { setResults((await api.searchKnowledge(query.trim(), 5)).citations); }
    catch (failure) { setResults(null); setMessage(failure instanceof Error ? failure.message : 'Knowledge search failed'); }
    finally { setSearching(false); }
  };

  if (resolvedOnly) return <Shell title="Sanitized resolved cases" subtitle="Only explicitly approved, privacy-sanitized cases participate in retrieval." message={message}>
    {loading ? <Loading /> : resolvedCases.length === 0 ? <Empty text="No sanitized resolved cases have been approved." /> : <div className="grid gap-3 md:grid-cols-2">{resolvedCases.map(value => <article key={value.id} className="panel space-y-3 p-5"><div className="flex justify-between gap-3"><b className="text-sm font-semibold">{value.sanitizedSubject}</b><span className="status-chip h-fit border-success/20 bg-success/10 text-success"><FileCheck2 className="h-3 w-3" />Approved</span></div><p className="text-xs leading-5 text-muted">{value.sanitizedDescription}</p><p className="rounded-input bg-surface-muted p-3 text-xs leading-5"><b>Resolution:</b> {value.sanitizedResolution}</p><p className="text-[10px] text-muted">Source ticket {value.originalTicketId.slice(0, 8)} · {new Date(value.approvedAt).toLocaleString()}</p></article>)}</div>}
  </Shell>;

  if (searchOnly) return <Shell title="Knowledge search" subtitle="Test tenant-scoped keyword and vector retrieval with metadata filtering and ranked fusion." message={message}>
    <SearchPanel query={query} setQuery={setQuery} searching={searching} results={results} runSearch={runSearch} />
  </Shell>;

  return <Shell title="Knowledge lifecycle" subtitle="Author, review, publish, supersede, archive, and roll back approved support knowledge." message={message}>
    {!readOnly && <div className="flex justify-end"><button onClick={() => { setNewVersionFor(null); setFormOpen(true); }} className="btn-primary"><Plus className="h-4 w-4" />New article</button></div>}
    {formOpen && <form onSubmit={event => void submitForm(event)} className="panel space-y-4 border-primary/25 p-5">
      <div><span className="eyebrow">Draft workflow</span><h2 className="section-title">{newVersionFor ? 'Create new draft version' : 'Create draft article'}</h2></div>
      {!newVersionFor && <div className="grid gap-3 md:grid-cols-3"><input required value={title} onChange={event => setTitle(event.target.value)} placeholder="Article title" aria-label="Article title" className="form-control h-10" /><select value={category} onChange={event => setCategory(event.target.value)} aria-label="Category" className="form-control h-10">{['BILLING','ACCOUNT','TECHNICAL','DELIVERY','GENERAL'].map(value => <option key={value}>{value}</option>)}</select><input value={product} onChange={event => setProduct(event.target.value)} placeholder="Product (optional)" aria-label="Product" className="form-control h-10" /></div>}
      <input value={summary} onChange={event => setSummary(event.target.value)} placeholder="Short summary" aria-label="Short summary" className="form-control h-10" />
      <textarea required rows={7} value={content} onChange={event => setContent(event.target.value)} placeholder="Approved troubleshooting or policy content" aria-label="Article content" className="form-control resize-y p-3.5 leading-6" />
      <div className="flex justify-end gap-2"><button type="button" onClick={() => { setFormOpen(false); setNewVersionFor(null); }} className="btn-secondary">Cancel</button><button disabled={loading} className="btn-primary">Save draft</button></div>
    </form>}
    <div className="grid min-h-[480px] gap-4 lg:grid-cols-[280px_minmax(0,1fr)]">
      <div className="panel-flat max-h-[360px] overflow-y-auto lg:max-h-none">{loading && documents.length === 0 ? <Loading /> : documents.length === 0 ? <Empty text="No knowledge articles exist. Create the first draft." /> : documents.map(document => <button key={document.id} onClick={() => setSelected(document)} className={`w-full border-b border-border-subtle p-3.5 text-left last:border-b-0 ${selected?.id === document.id ? 'border-l-2 border-l-primary bg-primary-soft' : 'hover:bg-surface-muted'}`}><div className="flex justify-between gap-2"><b className="line-clamp-2 text-xs">{document.title}</b><Status value={document.status} /></div><p className="mt-1.5 text-[10px] text-muted">{document.category}{document.product ? ` · ${document.product}` : ''}</p></button>)}</div>
      <div className="panel-flat space-y-4 p-5">{!selected ? <Empty text="Select an article to inspect its versions." /> : <><div className="flex flex-col justify-between gap-3 border-b border-border-subtle pb-4 sm:flex-row"><div><h2 className="font-semibold tracking-[-0.01em]">{selected.title}</h2><p className="mt-1 text-xs text-muted">{selected.category} · active version {selected.activeVersionId?.slice(0, 8) || 'none'}</p></div>{!readOnly && selected.status !== 'ARCHIVED' && <div className="flex gap-2"><button onClick={() => { setNewVersionFor(selected.id); setFormOpen(true); setContent(''); setSummary(''); }} className="btn-secondary min-h-9 px-3 text-xs">New version</button><button onClick={() => void api.archiveKnowledgeDocument(selected.id).then(() => refreshSelected(selected.id))} className="btn-secondary min-h-9 px-3 text-xs"><Archive className="h-3.5 w-3.5" />Archive</button></div>}</div><div className="space-y-3">{versions.map(version => <article key={version.id} className="space-y-3 rounded-card border border-border-subtle p-4"><div className="flex flex-col justify-between gap-2 sm:flex-row"><div className="flex items-center gap-2"><b className="text-sm">Version {version.versionNumber}</b><Status value={version.status} /></div><span className="text-[10px] text-muted">{new Date(version.createdAt).toLocaleString()}</span></div><p className="text-xs text-muted">{version.summary || 'No summary provided.'}</p><details className="text-xs"><summary className="cursor-pointer font-medium text-primary">View content</summary><p className="mt-2 whitespace-pre-wrap rounded-input bg-surface-muted p-3 leading-5">{version.content}</p></details>{version.reviewNote && <p className="rounded-input bg-warning/10 p-2.5 text-xs text-warning">Review note: {version.reviewNote}</p>}{rejectingVersionId === version.id && <div className="space-y-2 rounded-card border border-danger/20 bg-danger/5 p-3"><label htmlFor={`version-rejection-${version.id}`} className="text-xs font-semibold text-DEFAULT">What must the author correct?</label><textarea id={`version-rejection-${version.id}`} rows={3} value={rejectionNote} onChange={event => setRejectionNote(event.target.value)} placeholder="Give the author a specific, actionable reason…" className="form-control p-2.5 text-xs" /><div className="flex justify-end gap-2"><button onClick={() => { setRejectingVersionId(null); setRejectionNote(''); }} className="btn-ghost min-h-8 text-xs">Cancel</button><button disabled={!rejectionNote.trim()} onClick={() => void act('reject', version)} className="min-h-8 rounded-btn bg-danger px-3 text-xs font-semibold text-white disabled:opacity-50">Reject version</button></div></div>}{!readOnly && <div className="flex flex-wrap justify-end gap-2">{version.status === 'DRAFT' && <Action icon={<Send className="w-3.5 h-3.5" />} label="Submit for review" onClick={() => void act('submit', version)} />}{version.status === 'IN_REVIEW' && <><Action icon={<XCircle className="w-3.5 h-3.5" />} label="Reject" onClick={() => void act('reject', version)} /><Action icon={<CheckCircle2 className="w-3.5 h-3.5" />} label="Publish" primary onClick={() => void act('publish', version)} /></>}{version.status === 'SUPERSEDED' && <Action icon={<RotateCcw className="w-3.5 h-3.5" />} label="Rollback" onClick={() => void act('rollback', version)} />}</div>}</article>)}</div></>}</div>
    </div>
  </Shell>;
};

const Shell: React.FC<{ title: string; subtitle: string; message: string | null; children: React.ReactNode }> = ({ title, subtitle, message, children }) => <div className="app-page max-w-6xl"><header><span className="eyebrow">Knowledge operations</span><h1 className="page-title">{title}</h1><p className="page-description">{subtitle}</p></header>{message && <div className="rounded-card border border-primary/20 bg-primary-soft p-3.5 text-xs text-primary">{message}</div>}{children}</div>;
const Loading = () => <div className="p-8 text-xs text-muted text-center"><Loader2 className="w-4 h-4 animate-spin mx-auto mb-2" />Loading…</div>;
const Empty: React.FC<{ text: string }> = ({ text }) => <div className="p-8 text-xs text-muted text-center">{text}</div>;
const Status: React.FC<{ value: string }> = ({ value }) => <span className={`status-chip flex-none ${value === 'PUBLISHED' ? 'border-success/20 bg-success/10 text-success' : value === 'IN_REVIEW' ? 'border-warning/20 bg-warning/10 text-warning' : value === 'REJECTED' || value === 'ARCHIVED' ? 'border-danger/20 bg-danger/10 text-danger' : 'border-border-subtle bg-surface-muted text-muted'}`}>{value.replace(/_/g, ' ')}</span>;
const Action: React.FC<{ icon: React.ReactNode; label: string; primary?: boolean; onClick: () => void }> = ({ icon, label, primary, onClick }) => <button onClick={onClick} className={`${primary ? 'btn-primary' : 'btn-secondary'} min-h-9 px-3 text-xs`}>{icon}{label}</button>;

const SearchPanel: React.FC<{ query: string; setQuery: (value: string) => void; searching: boolean; results: Citation[] | null; runSearch: (event: React.FormEvent) => Promise<void> }> = ({ query, setQuery, searching, results, runSearch }) => <div className="space-y-4"><form onSubmit={event => void runSearch(event)} className="panel flex flex-col gap-2 p-3 sm:flex-row"><div className="relative flex-1"><Search className="pointer-events-none absolute left-3.5 top-3 h-4 w-4 text-muted" /><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Ask a natural support question" aria-label="Search approved knowledge" className="form-control h-10 pl-10" /></div><button disabled={searching || !query.trim()} className="btn-primary h-10">{searching ? 'Searching…' : 'Search index'}</button></form>{results && <div className="space-y-3">{results.length === 0 ? <Empty text="No approved active knowledge matched this query." /> : results.map((citation, index) => <article key={`${citation.sourceId}-${index}`} className="panel p-4"><div className="flex flex-col justify-between gap-2 sm:flex-row"><b className="flex gap-2 text-sm"><BookOpen className="h-4 w-4 flex-none text-primary" />{citation.title}</b><span className="font-mono text-[10px] text-primary">RRF {((citation.score ?? citation.confidenceScore ?? 0) * 100).toFixed(2)}</span></div><p className="mt-2 text-xs leading-5 text-muted">{citation.snippet || citation.citationText}</p></article>)}</div>}</div>;
