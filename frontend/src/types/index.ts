export type Role = 'CUSTOMER' | 'AGENT' | 'TEAM_LEAD' | 'KNOWLEDGE_MANAGER' | 'ADMIN' | 'AUDITOR';

export type TicketStatus = 
  | 'NEW'
  | 'TRIAGE_PENDING'
  | 'TRIAGE_IN_PROGRESS'
  | 'READY_FOR_AGENT'
  | 'IN_PROGRESS'
  | 'WAITING_ON_CUSTOMER'
  | 'RESOLVED'
  | 'CLOSED'
  | 'TRIAGE_FAILED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface User {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  roles: Role[];
}

export interface Ticket {
  id: string;
  ticketNumber: string;
  tenantId: string;
  customerId: string;
  teamId?: string;
  assignedAgentId?: string;
  subject: string;
  description: string;
  language: string;
  status: TicketStatus;
  priority: TicketPriority;
  category?: string;
  channel: string;
  slaPolicyId?: string;
  firstResponseDueAt?: string;
  resolutionDueAt?: string;
  aiTriageStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
  latestSuggestionId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiSuggestion {
  id: string;
  ticketId: string;
  suggestedResponse: string;
  confidenceScore: number;
  modelName: string;
  promptVersion: string;
  citations: Citation[];
  status: 'PENDING_REVIEW' | 'ACCEPTED' | 'EDITED' | 'REJECTED' | 'INVALIDATED';
  createdAt: string;
}

export interface Citation {
  sourceType: 'KNOWLEDGE_ARTICLE' | 'RESOLVED_CASE';
  sourceId: string;
  title: string;
  citationText: string;
  confidenceScore?: number;
}
