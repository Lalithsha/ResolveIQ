import {
  Ticket, TicketMessage, AiSuggestion, Citation, Role, TicketQueueResponse, AgentTicketContext,
  Attachment, KnowledgeDocument, KnowledgeVersion, User, Team, RoutingAgent, RoutingRule, SlaPolicy, ResolvedCase,
  AnalysisGovernanceSummary, OutboxSummary, SecurityAuditEvent, WorkflowInstance,
} from '../types';

const API_BASE = '/api/v1';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  roles: Role[];
}

class ApiClient {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
  }

  getToken(): string | null {
    return this.token;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}, retryAuthentication = true): Promise<T> {
    const headers: Record<string, string> = { ...(options.headers as Record<string, string>) };
    if (!(options.body instanceof FormData) && !headers['Content-Type']) headers['Content-Type'] = 'application/json';

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
      ...options,
      headers,
      credentials: 'include',
    });

    if (response.status === 401 && retryAuthentication && !endpoint.startsWith('/auth/')) {
      await this.refresh();
      return this.request<T>(endpoint, options, false);
    }

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} ${response.statusText}`;
      try {
        const errorBody = await response.json();
        errorMessage = errorBody.detail || errorBody.title || errorBody.message || errorMessage;
      } catch {
        // use default status message
      }
      throw new Error(errorMessage);
    }

    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }

  // Auth APIs
  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    this.setToken(res.accessToken);
    return res;
  }

  async register(email: string, password: string, fullName: string): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, fullName }),
    });
    this.setToken(res.accessToken);
    return res;
  }

  async refresh(): Promise<AuthResponse> {
    const response = await this.request<AuthResponse>('/auth/refresh', { method: 'POST' }, false);
    this.setToken(response.accessToken);
    return response;
  }

  async logout(): Promise<void> {
    try {
      await this.request<void>('/auth/logout', { method: 'POST' }, false);
    } finally {
      this.setToken(null);
    }
  }

  // Customer Ticket APIs
  async listCustomerTickets(): Promise<Ticket[]> {
    return this.request<Ticket[]>('/customer/tickets');
  }

  async getCustomerTicket(id: string): Promise<Ticket> {
    return this.request<Ticket>(`/customer/tickets/${id}`);
  }

  async createCustomerTicket(data: {
    subject: string;
    description: string;
    category?: string;
    priority?: string;
  }, idempotencyKey: string = crypto.randomUUID()): Promise<Ticket> {
    return this.request<Ticket>('/customer/tickets', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(data),
    });
  }

  async addCustomerMessage(ticketId: string, content: string): Promise<TicketMessage> {
    return this.request<TicketMessage>(`/customer/tickets/${ticketId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content, isInternal: false }),
    });
  }

  async getCustomerMessages(ticketId: string): Promise<TicketMessage[]> {
    return this.request<TicketMessage[]>(`/customer/tickets/${ticketId}/messages`);
  }

  // Agent Ticket APIs
  async listAgentTickets(teamId?: string): Promise<Ticket[]> {
    const query = teamId ? `?teamId=${teamId}` : '';
    return this.request<Ticket[]>(`/agent/tickets${query}`);
  }

  async searchAgentQueue(params: {
    scope: 'mine' | 'team' | 'all' | 'sla-risk'; teamId?: string; status?: string; priority?: string;
    query?: string; sort?: string; direction?: 'asc' | 'desc'; page?: number; size?: number;
  }): Promise<TicketQueueResponse> {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') query.set(key, String(value));
    });
    return this.request<TicketQueueResponse>(`/agent/tickets/queue?${query}`);
  }

  async getAgentTicketContext(id: string): Promise<AgentTicketContext> {
    return this.request<AgentTicketContext>(`/agent/tickets/${id}/context`);
  }

  async getAgentTicket(id: string): Promise<Ticket> {
    return this.request<Ticket>(`/agent/tickets/${id}`);
  }

  async getTicketSuggestions(ticketId: string): Promise<AiSuggestion[]> {
    return this.request<AiSuggestion[]>(`/agent/tickets/${ticketId}/suggestions`);
  }

  async addAgentMessage(ticketId: string, content: string, isInternal: boolean = false): Promise<any> {
    return this.request(`/agent/tickets/${ticketId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content, isInternal }),
    });
  }

  async updateTicketStatus(ticketId: string, status: string, reason?: string): Promise<Ticket> {
    return this.request<Ticket>(`/agent/tickets/${ticketId}/status`, {
      method: 'POST',
      body: JSON.stringify({ status, reason }),
    });
  }

  async recordFeedback(ticketId: string, feedback: {
    suggestionId: string;
    action: string;
    rejectionReason?: string;
    editedContent?: string;
    rating?: number;
  }): Promise<void> {
    return this.request(`/agent/tickets/${ticketId}/feedback`, {
      method: 'POST',
      body: JSON.stringify(feedback),
    });
  }

  async assignTicket(ticketId: string, teamId?: string, agentId?: string): Promise<Ticket> {
    return this.request<Ticket>(`/agent/tickets/${ticketId}/assign`, {
      method: 'POST', body: JSON.stringify({ teamId: teamId || null, agentId: agentId || null, reason: 'Queue assignment' }),
    });
  }

  async listAgentAttachments(ticketId: string): Promise<Attachment[]> {
    return this.request<Attachment[]>(`/agent/tickets/${ticketId}/attachments`);
  }

  async uploadAgentAttachment(ticketId: string, file: File): Promise<Attachment> {
    const body = new FormData(); body.append('file', file);
    return this.request<Attachment>(`/agent/tickets/${ticketId}/attachments`, { method: 'POST', body });
  }

  async listCustomerAttachments(ticketId: string): Promise<Attachment[]> {
    return this.request<Attachment[]>(`/customer/tickets/${ticketId}/attachments`);
  }

  async uploadCustomerAttachment(ticketId: string, file: File): Promise<Attachment> {
    const body = new FormData(); body.append('file', file);
    return this.request<Attachment>(`/customer/tickets/${ticketId}/attachments`, { method: 'POST', body });
  }

  async downloadAttachment(scope: 'agent' | 'customer', ticketId: string, attachmentId: string, fileName: string): Promise<void> {
    const response = await fetch(`${API_BASE}/${scope}/tickets/${ticketId}/attachments/${attachmentId}/content`, {
      headers: this.token ? { Authorization: `Bearer ${this.token}` } : {}, credentials: 'include',
    });
    if (!response.ok) throw new Error(`Attachment download failed (${response.status})`);
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a'); link.href = url; link.download = fileName; link.click();
    URL.revokeObjectURL(url);
  }

  async followTicketEvents(onEvent: () => void, signal: AbortSignal): Promise<void> {
    const response = await fetch(`${API_BASE}/agent/tickets/stream`, {
      headers: this.token ? { Authorization: `Bearer ${this.token}`, Accept: 'text/event-stream' } : { Accept: 'text/event-stream' },
      credentials: 'include', signal,
    });
    if (!response.ok || !response.body) throw new Error(`Ticket event stream failed (${response.status})`);
    const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = '';
    while (!signal.aborted) {
      const { value, done } = await reader.read(); if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split('\n\n'); buffer = frames.pop() || '';
      frames.forEach(frame => { if (frame.includes('event:ticket.')) onEvent(); });
    }
  }

  // Knowledge & Retrieval APIs
  async listKnowledgeDocuments(): Promise<KnowledgeDocument[]> {
    return this.request<KnowledgeDocument[]>('/knowledge/articles');
  }

  async searchKnowledge(queryText: string, topK: number = 5): Promise<{ citations: Citation[] }> {
    return this.request('/retrieval/search', {
      method: 'POST',
      body: JSON.stringify({ queryText, topK }),
    });
  }

  async createKnowledgeDocument(data: { title: string; category: string; product?: string; language?: string; content: string; summary?: string }): Promise<KnowledgeDocument> {
    return this.request<KnowledgeDocument>('/knowledge/articles', { method: 'POST', body: JSON.stringify(data) });
  }

  async listKnowledgeVersions(documentId: string): Promise<KnowledgeVersion[]> {
    return this.request<KnowledgeVersion[]>(`/knowledge/articles/${documentId}/versions`);
  }

  async createKnowledgeVersion(documentId: string, content: string, summary?: string): Promise<KnowledgeVersion> {
    return this.request<KnowledgeVersion>(`/knowledge/articles/${documentId}/versions`, {
      method: 'POST', body: JSON.stringify({ content, summary }),
    });
  }

  async submitKnowledgeVersion(documentId: string, versionId: string): Promise<KnowledgeVersion> {
    return this.request<KnowledgeVersion>(`/knowledge/articles/${documentId}/versions/${versionId}/submit`, { method: 'POST' });
  }

  async publishKnowledgeVersion(documentId: string, versionId: string, note?: string): Promise<KnowledgeDocument> {
    return this.request<KnowledgeDocument>(`/knowledge/articles/${documentId}/versions/${versionId}/publish`, {
      method: 'POST', body: JSON.stringify({ note }),
    });
  }

  async rejectKnowledgeVersion(documentId: string, versionId: string, note: string): Promise<KnowledgeVersion> {
    return this.request<KnowledgeVersion>(`/knowledge/articles/${documentId}/versions/${versionId}/reject`, {
      method: 'POST', body: JSON.stringify({ note }),
    });
  }

  async rollbackKnowledgeVersion(documentId: string, versionId: string): Promise<KnowledgeDocument> {
    return this.request<KnowledgeDocument>(`/knowledge/articles/${documentId}/rollback/${versionId}`, {
      method: 'POST', body: JSON.stringify({ note: 'Rollback approved in Knowledge Console' }),
    });
  }

  async archiveKnowledgeDocument(documentId: string): Promise<KnowledgeDocument> {
    return this.request<KnowledgeDocument>(`/knowledge/articles/${documentId}/archive`, { method: 'POST' });
  }
  async listResolvedCases(): Promise<ResolvedCase[]> { return this.request<ResolvedCase[]>('/knowledge/resolved-cases'); }

  async getDirectoryUser(userId: string): Promise<User> {
    return normalizeUser(await this.request<User & { userId?: string }>(`/directory/users/${userId}`));
  }
  async listTeams(): Promise<Team[]> { return this.request<Team[]>('/routing/teams'); }
  async listRoutingAgents(): Promise<RoutingAgent[]> { return this.request<RoutingAgent[]>('/routing/agents'); }
  async listRoutingRules(): Promise<RoutingRule[]> { return this.request<RoutingRule[]>('/routing/rules'); }
  async listSlaPolicies(): Promise<SlaPolicy[]> { return this.request<SlaPolicy[]>('/routing/sla-policies'); }
  async setRoutingRuleActive(id: string, active: boolean): Promise<RoutingRule> {
    return this.request<RoutingRule>(`/routing/rules/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) });
  }

  async listAdminUsers(): Promise<{ items: User[]; page: number; totalElements: number }> {
    const page = await this.request<{ items: Array<User & { userId?: string }>; page: number; totalElements: number }>('/admin/users?size=100');
    return { ...page, items: page.items.map(normalizeUser) };
  }

  async updateUserRoles(userId: string, roles: Role[]): Promise<User> {
    return normalizeUser(await this.request<User & { userId?: string }>(`/admin/users/${userId}/roles`, {
      method: 'PATCH', body: JSON.stringify({ roles }),
    }));
  }

  async createStaffUser(data: { tenantId: string; email: string; password: string; fullName: string; roles: Role[] }): Promise<User> {
    return normalizeUser(await this.request<User & { userId?: string }>('/auth/users', { method: 'POST', body: JSON.stringify(data) }));
  }

  async listSecurityAuditEvents(): Promise<{ content: SecurityAuditEvent[] }> {
    return this.request('/audit/security-events?size=100');
  }

  async getAnalysisGovernance(): Promise<AnalysisGovernanceSummary> {
    return this.request('/analysis/governance/summary');
  }

  async getTicketOutboxSummary(): Promise<OutboxSummary> {
    return this.request('/admin/ticket-operations/outbox-summary');
  }

  async getWorkflowOutboxSummary(): Promise<OutboxSummary> {
    return this.request('/workflows/operations/outbox-summary');
  }

  // Workflows & DLQ APIs
  async listFailedWorkflows(): Promise<WorkflowInstance[]> {
    return this.request<WorkflowInstance[]>('/workflows/failed');
  }

  async retryWorkflow(workflowId: string, reason: string): Promise<any> {
    return this.request(`/workflows/${workflowId}/retry`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }
}

export const api = new ApiClient();

function normalizeUser<T extends User & { userId?: string }>(value: T): User {
  return { ...value, id: value.id || value.userId || '' };
}
