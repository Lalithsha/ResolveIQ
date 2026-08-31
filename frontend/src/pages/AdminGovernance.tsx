import React, { useCallback, useEffect, useState } from 'react';
import {
  AlertTriangle, Cpu, Database, Loader2, RefreshCw, Route, ShieldCheck,
  ToggleLeft, ToggleRight, Users,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
  AnalysisGovernanceSummary, OutboxSummary, Role, RoutingRule, SecurityAuditEvent, SlaPolicy, Team,
  User, WorkflowInstance,
} from '../types';

interface Props { activeTab?: string; role?: Role; }
const ASSIGNABLE_ROLES: Role[] = ['CUSTOMER', 'AGENT', 'TEAM_LEAD', 'KNOWLEDGE_MANAGER', 'ADMIN', 'AUDITOR'];

export const AdminGovernance: React.FC<Props> = ({ activeTab = 'overview', role = 'ADMIN' }) => {
  const { user: currentUser } = useAuth();
  const readOnly = role === 'AUDITOR';
  const [workflows, setWorkflows] = useState<WorkflowInstance[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [rules, setRules] = useState<RoutingRule[]>([]);
  const [policies, setPolicies] = useState<SlaPolicy[]>([]);
  const [events, setEvents] = useState<SecurityAuditEvent[]>([]);
  const [governance, setGovernance] = useState<AnalysisGovernanceSummary | null>(null);
  const [outbox, setOutbox] = useState<OutboxSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const [retryingId, setRetryingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setMessage(null);
    const results = await Promise.allSettled([
      api.listFailedWorkflows(), api.listAdminUsers(), api.listTeams(), api.listRoutingRules(),
      api.listSlaPolicies(), api.listSecurityAuditEvents(), api.getAnalysisGovernance(),
      api.getTicketOutboxSummary(), api.getWorkflowOutboxSummary(),
    ]);
    if (results[0].status === 'fulfilled') setWorkflows(results[0].value);
    if (results[1].status === 'fulfilled') setUsers(results[1].value.items);
    if (results[2].status === 'fulfilled') setTeams(results[2].value);
    if (results[3].status === 'fulfilled') setRules(results[3].value);
    if (results[4].status === 'fulfilled') setPolicies(results[4].value);
    if (results[5].status === 'fulfilled') setEvents(results[5].value.content);
    if (results[6].status === 'fulfilled') setGovernance(results[6].value);
    if (results[7].status === 'fulfilled' && results[8].status === 'fulfilled') {
      setOutbox({
        PENDING: results[7].value.PENDING + results[8].value.PENDING,
        RETRY: results[7].value.RETRY + results[8].value.RETRY,
        DEAD: results[7].value.DEAD + results[8].value.DEAD,
        PUBLISHED: results[7].value.PUBLISHED + results[8].value.PUBLISHED,
      });
    }
    const failures = results.filter(result => result.status === 'rejected').length;
    if (failures) setMessage(`${failures} operational data source${failures === 1 ? '' : 's'} unavailable. Available panels remain accurate.`);
    setLoading(false);
  }, []);

  useEffect(() => { void load(); }, [load]);

  const retry = async (workflowId: string) => {
    if (readOnly) return;
    setRetryingId(workflowId); setMessage(null);
    try {
      await api.retryWorkflow(workflowId, 'Administrator replay from ResolveIQ operations console');
      setWorkflows(previous => previous.filter(item => item.id !== workflowId));
      setMessage('Workflow replay accepted.');
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : 'Workflow retry failed'); }
    finally { setRetryingId(null); }
  };

  const toggleRule = async (rule: RoutingRule) => {
    if (readOnly) return;
    try {
      const updated = await api.setRoutingRuleActive(rule.id, !rule.active);
      setRules(previous => previous.map(item => item.id === updated.id ? updated : item));
      setMessage(`Routing rule ${updated.name} is now ${updated.active ? 'active' : 'inactive'}.`);
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : 'Rule update failed'); }
  };

  const updateRole = async (target: User, nextRole: Role) => {
    if (readOnly || target.id === currentUser?.id) return;
    try {
      const updated = await api.updateUserRoles(target.id, [nextRole]);
      setUsers(previous => previous.map(item => item.id === updated.id ? updated : item));
      setMessage(`Roles updated for ${updated.fullName}.`);
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : 'Role update failed'); }
  };

  const createStaff = async (data: { email: string; password: string; fullName: string; role: Role }): Promise<boolean> => {
    if (readOnly || !currentUser) return false;
    try {
      const created = await api.createStaffUser({ tenantId: currentUser.tenantId, email: data.email, password: data.password, fullName: data.fullName, roles: [data.role] });
      setUsers(previous => [...previous, created]); setMessage(`Created ${created.fullName}.`); return true;
    } catch (failure) { setMessage(failure instanceof Error ? failure.message : 'User creation failed'); return false; }
  };

  if (loading) return <div className="app-page grid min-h-[60vh] place-items-center text-sm text-muted"><Loader2 className="h-5 w-5 animate-spin" />Loading operational data…</div>;

  const panel = activeTab === 'audit' ? <Audit events={events} />
    : activeTab === 'routing' ? <Routing teams={teams} rules={rules} policies={policies} readOnly={readOnly} onToggle={toggleRule} />
    : activeTab === 'users' ? <UsersPanel users={users} readOnly={readOnly} currentUserId={currentUser?.id} onRole={updateRole} onCreate={createStaff} />
    : activeTab === 'workflows' ? <Workflows workflows={workflows} readOnly onRetry={retry} retryingId={retryingId} />
    : activeTab === 'governance' ? <Governance data={governance} />
    : <Overview workflows={workflows} users={users} rules={rules} events={events} governance={governance} outbox={outbox} readOnly={readOnly} onRetry={retry} retryingId={retryingId} />;

  return <div className="app-page">
    <header className="page-header"><div><span className="eyebrow">{readOnly ? 'Read-only evidence' : 'Administration'}</span><h1 className="page-title">{title(activeTab)}</h1><p className="page-description">Persisted operational state only. Empty and unavailable states are shown explicitly.</p></div><button onClick={() => void load()} className="btn-secondary"><RefreshCw className="h-4 w-4" />Refresh</button></header>
    {message && <div className="rounded-card border border-warning/20 bg-warning/10 px-4 py-3 text-xs text-warning">{message}</div>}
    {panel}
  </div>;
};

interface OverviewProps {
  workflows: WorkflowInstance[]; users: User[]; rules: RoutingRule[]; events: SecurityAuditEvent[];
  governance: AnalysisGovernanceSummary | null; outbox: OutboxSummary | null; readOnly: boolean; retryingId: string | null;
  onRetry: (id: string) => Promise<void>;
}
const Overview: React.FC<OverviewProps> = props => {
  const cards = [
    ['Failed workflows', props.workflows.length, AlertTriangle, props.workflows.length ? 'text-danger' : 'text-success'],
    ['Tenant users', props.users.length, Users, 'text-primary'],
    ['Active routing rules', props.rules.filter(rule => rule.active).length, Route, 'text-primary'],
    ['AI invocations', props.governance?.totalInvocations ?? 'Unavailable', Cpu, 'text-ai'],
    ['Outbox pending / dead', props.outbox ? `${props.outbox.PENDING + props.outbox.RETRY} / ${props.outbox.DEAD}` : 'Unavailable', Database, props.outbox?.DEAD ? 'text-danger' : 'text-success'],
    ['Security events', props.events.length, ShieldCheck, 'text-info'],
  ] as const;
  return <><section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">{cards.map(([label, value, Icon, tone]) => <article key={label} className="panel p-4"><div className="flex justify-between text-xs text-muted"><span>{label}</span><Icon className={`h-4 w-4 ${tone}`} /></div><strong className="mt-4 block text-2xl">{value}</strong></article>)}</section><Workflows workflows={props.workflows} readOnly={props.readOnly} onRetry={props.onRetry} retryingId={props.retryingId} /></>;
};

const Workflows: React.FC<{ workflows: WorkflowInstance[]; readOnly: boolean; retryingId: string | null; onRetry: (id: string) => Promise<void> }> = ({ workflows, readOnly, retryingId, onRetry }) => <section className="panel overflow-hidden"><PanelHeader title="Failed workflows" subtitle="Failed workflow instances owned by the current tenant." count={workflows.length} /><Table headers={['Workflow', 'Ticket', 'Step', 'Updated', 'Action']}>{workflows.length === 0 ? <Empty columns={5} text="No failed workflows." /> : workflows.map(item => <tr key={item.id} className="border-t border-border-subtle"><Mono value={item.id} /><Mono value={item.ticketId} /><Cell>{item.currentStep || 'Unknown'}</Cell><Cell>{formatDate(item.updatedAt)}</Cell><Cell right>{readOnly ? <span className="text-muted">Read only</span> : <button disabled={retryingId === item.id} onClick={() => void onRetry(item.id)} className="btn-secondary min-h-8 px-3 text-xs"><RefreshCw className={`h-3.5 w-3.5 ${retryingId === item.id ? 'animate-spin' : ''}`} />Retry</button>}</Cell></tr>)}</Table></section>;

const Governance: React.FC<{ data: AnalysisGovernanceSummary | null }> = ({ data }) => {
  if (!data) return <Unavailable text="AI governance endpoint is unavailable; no values are being invented." />;
  const validRate = data.totalInvocations ? ((data.validInvocations / data.totalInvocations) * 100).toFixed(1) : '0.0';
  return <><section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{[['Invocations', data.totalInvocations], ['Valid output', `${validRate}%`], ['Guardrail blocks', data.blockedInvocations], ['Estimated cost', `$${(data.estimatedCostMicros / 1_000_000).toFixed(4)}`]].map(([label, value]) => <article key={label} className="panel p-4"><span className="text-xs text-muted">{label}</span><strong className="mt-3 block text-2xl">{value}</strong></article>)}</section><section className="panel overflow-hidden"><PanelHeader title="Recent model traces" subtitle="Sanitized invocation metadata; raw provider output is not stored." count={data.recentTraces.length} /><Table headers={['Ticket', 'Model / prompt', 'Tokens', 'Guardrail', 'Validation', 'Latency']}>{data.recentTraces.length === 0 ? <Empty columns={6} text="No model invocations recorded." /> : data.recentTraces.map(trace => <tr key={trace.id} className="border-t border-border-subtle"><Mono value={trace.ticketId} /><Cell>{trace.modelName}<br/><span className="text-[10px] text-muted">{trace.promptVersion}</span></Cell><Cell>{trace.inputTokens} in / {trace.outputTokens} out</Cell><Cell>{trace.guardrailOutcome}</Cell><Cell>{trace.validationOutcome}</Cell><Cell>{trace.latencyMs} ms</Cell></tr>)}</Table></section></>;
};

const Routing: React.FC<{ teams: Team[]; rules: RoutingRule[]; policies: SlaPolicy[]; readOnly: boolean; onToggle: (rule: RoutingRule) => Promise<void> }> = ({ teams, rules, policies, readOnly, onToggle }) => <div className="grid gap-5 xl:grid-cols-2"><section className="panel overflow-hidden"><PanelHeader title="Routing rules" subtitle="Priority-ordered, tenant-scoped routing configuration." count={rules.length} /><div className="divide-y divide-border-subtle">{rules.length === 0 ? <div className="p-6 text-sm text-muted">No routing rules configured.</div> : rules.map(rule => <div key={rule.id} className="flex items-center justify-between gap-3 p-4"><div><b className="text-sm">{rule.name}</b><p className="text-xs text-muted">Priority {rule.priorityOrder} · target {teamName(teams, rule.targetTeamId)}</p></div><button disabled={readOnly} aria-label={`Set ${rule.name} ${rule.active ? 'inactive' : 'active'}`} onClick={() => void onToggle(rule)} className="text-primary disabled:text-muted">{rule.active ? <ToggleRight className="h-7 w-7" /> : <ToggleLeft className="h-7 w-7" />}</button></div>)}</div></section><section className="space-y-5"><section className="panel p-5"><h2 className="section-title">Teams ({teams.length})</h2><div className="mt-3 space-y-2">{teams.map(team => <div key={team.id} className="rounded-btn bg-surface-muted p-3 text-xs"><b>{team.name}</b><span className="float-right text-muted">Capacity {team.maxActiveTickets}</span></div>)}{teams.length === 0 && <p className="text-sm text-muted">No teams configured.</p>}</div></section><section className="panel p-5"><h2 className="section-title">SLA policies ({policies.length})</h2><div className="mt-3 space-y-2">{policies.map(policy => <div key={policy.id} className="rounded-btn bg-surface-muted p-3 text-xs"><b>{policy.name}</b><p className="mt-1 text-muted">{policy.priority}: first response {policy.firstResponseTargetMinutes}m · resolution {policy.resolutionTargetMinutes}m</p></div>)}{policies.length === 0 && <p className="text-sm text-muted">No SLA policies configured.</p>}</div></section></section></div>;

interface UsersPanelProps { users: User[]; readOnly: boolean; currentUserId?: string; onRole: (user: User, role: Role) => Promise<void>; onCreate: (data: { email: string; password: string; fullName: string; role: Role }) => Promise<boolean>; }
const UsersPanel: React.FC<UsersPanelProps> = ({ users, readOnly, currentUserId, onRole, onCreate }) => {
  const [fullName, setFullName] = useState(''); const [email, setEmail] = useState('');
  const [password, setPassword] = useState(''); const [newRole, setNewRole] = useState<Role>('AGENT');
  const [saving, setSaving] = useState(false);
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setSaving(true);
    try { if (await onCreate({ fullName, email, password, role: newRole })) { setFullName(''); setEmail(''); setPassword(''); } }
    finally { setSaving(false); }
  };
  return <div className="space-y-5">{!readOnly && <form onSubmit={event => void submit(event)} className="panel grid gap-3 p-5 md:grid-cols-5"><input required value={fullName} onChange={event => setFullName(event.target.value)} className="form-control h-10" placeholder="Full name" /><input required type="email" value={email} onChange={event => setEmail(event.target.value)} className="form-control h-10" placeholder="Email" /><input required minLength={12} type="password" value={password} onChange={event => setPassword(event.target.value)} className="form-control h-10" placeholder="Temporary password" /><select value={newRole} onChange={event => setNewRole(event.target.value as Role)} className="form-control h-10">{ASSIGNABLE_ROLES.filter(item => item !== 'CUSTOMER').map(item => <option key={item}>{item}</option>)}</select><button disabled={saving} className="btn-primary">{saving ? 'Creating…' : 'Create staff user'}</button></form>}<section className="panel overflow-hidden"><PanelHeader title="Users and roles" subtitle="Tenant-scoped identities. Self-role changes are intentionally disabled." count={users.length} /><Table headers={['User', 'Email', 'Current roles', 'Change primary role']}>{users.length === 0 ? <Empty columns={4} text="No tenant users returned." /> : users.map(user => <tr key={user.id} className="border-t border-border-subtle"><Cell><b>{user.fullName}</b></Cell><Cell>{user.email}</Cell><Cell>{user.roles.join(', ')}</Cell><Cell>{readOnly || user.id === currentUserId ? <span className="text-muted">{user.id === currentUserId ? 'Current user' : 'Read only'}</span> : <select defaultValue="" onChange={event => { if (event.target.value) void onRole(user, event.target.value as Role); }} className="form-control h-9"><option value="">Select role…</option>{ASSIGNABLE_ROLES.map(item => <option key={item}>{item}</option>)}</select>}</Cell></tr>)}</Table></section></div>;
};

const Audit: React.FC<{ events: SecurityAuditEvent[] }> = ({ events }) => <section className="panel overflow-hidden"><PanelHeader title="Security audit trail" subtitle="Authentication and administrative security events." count={events.length} /><Table headers={['Time', 'Event', 'Status', 'User', 'Source IP']}>{events.length === 0 ? <Empty columns={5} text="No security events recorded." /> : events.map(event => <tr key={event.id} className="border-t border-border-subtle"><Cell>{formatDate(event.occurredAt)}</Cell><Cell>{event.eventType}</Cell><Cell><span className="status-chip border-border-subtle">{event.status}</span></Cell><Mono value={event.userId || 'System'} /><Cell>{event.ipAddress || 'Unavailable'}</Cell></tr>)}</Table></section>;

const PanelHeader: React.FC<{ title: string; subtitle: string; count: number }> = ({ title, subtitle, count }) => <div className="flex items-center justify-between border-b border-border-subtle px-5 py-4"><div><h2 className="section-title">{title}</h2><p className="mt-1 text-[11px] text-muted">{subtitle}</p></div><span className="rounded-full bg-surface-muted px-2.5 py-1 text-[10px] font-semibold">{count}</span></div>;
const Table: React.FC<{ headers: string[]; children: React.ReactNode }> = ({ headers, children }) => <div className="overflow-x-auto"><table className="w-full min-w-[720px] text-left text-xs"><thead className="bg-surface-muted text-[10px] uppercase tracking-wider text-muted"><tr>{headers.map(value => <th key={value} className="px-5 py-3">{value}</th>)}</tr></thead><tbody>{children}</tbody></table></div>;
const Cell: React.FC<{ children: React.ReactNode; right?: boolean }> = ({ children, right }) => <td className={`px-5 py-4 ${right ? 'text-right' : ''}`}>{children}</td>;
const Mono: React.FC<{ value: string }> = ({ value }) => <td className="max-w-[190px] truncate px-5 py-4 font-mono text-[11px]" title={value}>{value}</td>;
const Empty: React.FC<{ columns: number; text: string }> = ({ columns, text }) => <tr><td colSpan={columns} className="px-5 py-10 text-center text-sm text-muted">{text}</td></tr>;
const Unavailable: React.FC<{ text: string }> = ({ text }) => <div className="panel flex items-center gap-3 p-6 text-sm text-muted"><Database className="h-5 w-5" />{text}</div>;
const title = (tab: string) => ({ overview: 'Operations overview', routing: 'Teams and routing', users: 'Users and roles', audit: 'Security audit', workflows: 'Workflow audit', governance: 'AI governance' }[tab] || 'Operations');
const teamName = (teams: Team[], id: string) => teams.find(team => team.id === id)?.name || `Team ${id.slice(0, 8)}`;
const formatDate = (value: string) => new Date(value).toLocaleString();
