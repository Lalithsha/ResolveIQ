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
  userId?: string;
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
  intent?: string;
  sentiment?: string;
  urgency?: string;
  triageConfidence?: number;
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
  citations: Citation[] | string;
  status: 'PENDING_REVIEW' | 'ACCEPTED' | 'EDITED' | 'REJECTED' | 'INVALIDATED';
  createdAt: string;
}

export interface Citation {
  sourceType: 'KNOWLEDGE_ARTICLE' | 'RESOLVED_CASE';
  sourceId: string;
  versionId?: string;
  chunkId?: string;
  title: string;
  citationText: string;
  snippet?: string;
  confidenceScore?: number;
  score?: number;
}

export interface TicketMessage {
  id: string;
  ticketId: string;
  senderId: string;
  senderRole: 'CUSTOMER' | 'AGENT' | 'SYSTEM';
  content: string;
  isInternal: boolean;
  createdAt: string;
}

export interface TicketQueueResponse {
  items: Ticket[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AgentTicketContext {
  ticket: Ticket;
  messages: TicketMessage[];
  suggestions: AiSuggestion[];
}

export interface Attachment {
  id: string;
  ticketId: string;
  uploaderId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  scanStatus: string;
  sha256: string;
  scanEngine: string;
  createdAt: string;
}

export interface KnowledgeDocument {
  id: string;
  tenantId: string;
  title: string;
  category: string;
  product?: string;
  language: string;
  status: 'DRAFT' | 'IN_REVIEW' | 'PUBLISHED' | 'ARCHIVED';
  activeVersionId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  content: string;
  summary?: string;
  status: 'DRAFT' | 'IN_REVIEW' | 'PUBLISHED' | 'SUPERSEDED' | 'REJECTED';
  createdByUserId?: string;
  submittedAt?: string;
  reviewedByUserId?: string;
  reviewedAt?: string;
  reviewNote?: string;
  publishedAt?: string;
  createdAt: string;
}

export interface Team { id: string; tenantId: string; name: string; description?: string; maxActiveTickets: number; }
export interface RoutingAgent { id: string; tenantId: string; teamId?: string; name: string; email: string; status: string; activeTicketCount: number; }
export interface RoutingRule { id: string; tenantId: string; name: string; version: string; conditions: string; targetTeamId: string; priorityOrder: number; active: boolean; }
export interface SlaPolicy { id: string; tenantId: string; name: string; priority: string; firstResponseTargetMinutes: number; resolutionTargetMinutes: number; businessHoursOnly: boolean; }
export interface ResolvedCase { id: string; tenantId: string; originalTicketId: string; sanitizedSubject: string; sanitizedDescription: string; sanitizedResolution: string; category?: string; approvedByUserId: string; approvedAt: string; }
export interface WorkflowInstance { id: string; ticketId: string; workflowType: string; status: string; currentStep?: string; createdAt: string; updatedAt: string; }
export interface SecurityAuditEvent { id: string; tenantId: string; userId?: string; eventType: string; status: string; ipAddress?: string; userAgent?: string; occurredAt: string; }
export interface AnalysisTrace { id: string; ticketId: string; intent: string; category: string; modelName: string; promptVersion: string; validationOutcome: string; guardrailOutcome: string; guardrailFindings: string; inputTokens: number; outputTokens: number; estimatedCostMicros: number; latencyMs: number; createdAt: string; }
export interface AnalysisGovernanceSummary { totalInvocations: number; validInvocations: number; blockedInvocations: number; fallbackInvocations: number; inputTokens: number; outputTokens: number; estimatedCostMicros: number; recentTraces: AnalysisTrace[]; }
export interface OutboxSummary { PENDING: number; RETRY: number; DEAD: number; PUBLISHED: number; }
