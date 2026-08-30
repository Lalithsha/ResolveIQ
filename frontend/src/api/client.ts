import { Ticket, TicketMessage, AiSuggestion, Citation, Role } from '../types';

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
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

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

  // Knowledge & Retrieval APIs
  async listKnowledgeDocuments(): Promise<any[]> {
    return this.request<any[]>('/knowledge/documents');
  }

  async searchKnowledge(queryText: string, topK: number = 5): Promise<{ citations: Citation[] }> {
    return this.request('/retrieval/search', {
      method: 'POST',
      body: JSON.stringify({ queryText, topK }),
    });
  }

  // Workflows & DLQ APIs
  async listFailedWorkflows(): Promise<any[]> {
    return this.request<any[]>('/workflows/failed');
  }

  async retryWorkflow(workflowId: string, reason: string): Promise<any> {
    return this.request(`/workflows/${workflowId}/retry`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  }
}

export const api = new ApiClient();
