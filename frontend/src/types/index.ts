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

export interface SupportIncident {
  id: string;
  tenantId: string;
  title: string;
  summary: string;
  status: 'INVESTIGATING' | 'IDENTIFIED' | 'MONITORING' | 'RESOLVED';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  affectedComponent: string;
  detectedAt: string;
  resolvedAt?: string | null;
  ticketCount: number;
  affectedCustomerCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface IncidentCluster {
  id: string;
  clusterKey: string;
  title: string;
  summary: string;
  suggestedSeverity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  affectedComponent: string;
  ticketCount: number;
  status: 'PROPOSED' | 'CONFIRMED' | 'DISMISSED';
  sampleTicketIds: string[];
  createdAt: string;
}

export interface IncidentUpdate {
  id: string;
  incidentId: string;
  updateType: 'INVESTIGATING' | 'IDENTIFIED' | 'MONITORING' | 'RESOLVED';
  summary: string;
  customerFacingMessage: string;
  authorId: string;
  approverId?: string | null;
  status: 'DRAFT' | 'APPROVED' | 'PUBLISHED';
  createdAt: string;
  publishedAt?: string | null;
}

export interface CustomerImpact {
  id: string;
  customerId: string;
  incidentId: string;
  customerEmail: string;
  customerName: string;
  linkedTicketId: string;
  createdAt: string;
}

export interface ActiveCustomerIncident {
  incidentId: string;
  title: string;
  summary: string;
  severity: string;
  status: string;
  affectedComponent: string;
  latestCustomerMessage?: string;
  publishedAt?: string;
}

export interface PolicyDecisionDto {
  decision: 'ALLOWED_IMMEDIATE' | 'REQUIRES_APPROVAL' | 'DENIED';
  matchedRules: string[];
  requiredPermissions: string[];
  requiredApprovalCount: number;
  financialLimitCents?: number | null;
  reasonCodes: string[];
  evaluatedAt: string;
}

export interface ActionApprovalDto {
  id: string;
  actorId: string;
  actorRole: string;
  decision: string;
  approvedDigest: string;
  authenticationTime?: string;
  comment?: string;
  createdAt: string;
}

export interface ActionReconciliationDto {
  id: string;
  status: 'RECONCILED' | 'MISMATCH_MANUAL_REVIEW';
  expectedState: Record<string, any>;
  observedState: Record<string, any>;
  notes?: string;
  reconciledAt: string;
}

export interface ActionProposalResponse {
  id: string;
  tenantId: string;
  ticketId: string;
  actionType: 'REFUND_DUPLICATE_CHARGE' | 'UNLOCK_ACCOUNT';
  status: 'DRAFT' | 'PROPOSED' | 'POLICY_DENIED' | 'AWAITING_APPROVAL' | 'APPROVED' | 'EXECUTING' | 'SUCCEEDED' | 'RECONCILED' | 'REJECTED' | 'EXPIRED' | 'EXECUTION_UNKNOWN' | 'FAILED_RETRYABLE' | 'FAILED_FINAL' | 'MANUAL_REVIEW';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  input: Record<string, any>;
  inputHash: string;
  canonicalDigest: string;
  canonicalBytes: string;
  aiRationale?: string;
  evidenceIds?: string;
  policyDecision?: PolicyDecisionDto;
  approvals: ActionApprovalDto[];
  version: number;
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface ActionExecutionResponse {
  executionId: string;
  proposalId: string;
  status: string;
  provider: string;
  providerReference?: string;
  sanitizedResponse?: Record<string, any>;
  errorMessage?: string;
  reconciliation?: ActionReconciliationDto;
  startedAt: string;
  completedAt?: string;
}

export interface CompensationResponse {
  isSupported: boolean;
  status: string;
  reason: string;
}

// Omnichannel Continuity and Handoff Types
export type ChannelType = 'PORTAL' | 'EMAIL';
export type ChannelDirection = 'INBOUND' | 'OUTBOUND' | 'INTERNAL';
export type DeliveryStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'BOUNCED' | 'FAILED' | 'UNKNOWN';
export type HandoffState = 'NONE' | 'REQUESTED' | 'QUEUED' | 'ASSIGNED' | 'REJECTED';

export interface TimelineMessageItem {
  messageId: string;
  conversationId: string;
  senderId: string;
  senderRole: 'CUSTOMER' | 'AGENT' | 'SYSTEM' | 'ADMIN';
  content: string;
  isInternal: boolean;
  channel: ChannelType;
  direction: ChannelDirection;
  deliveryStatus: DeliveryStatus;
  senderAddress?: string;
  recipientAddress?: string;
  createdAt: string;
}

export interface HandoffSummary {
  id: string;
  conversationId: string;
  ticketId: string;
  issueSummary: string;
  verifiedFacts: string;
  attemptedSteps: string;
  promisedActions: string;
  sentiment: string;
  openQuestions: string;
  createdAt: string;
}

export interface TimelineResponse {
  conversationId: string;
  ticketId: string;
  status: string;
  handoffState: HandoffState;
  preferredChannel: ChannelType;
  messages: TimelineMessageItem[];
  latestHandoff?: HandoffSummary;
}

export interface HandoffResponse {
  conversationId: string;
  state: HandoffState;
  summary?: HandoffSummary;
  message: string;
}

export interface ChannelIdentity {
  id: string;
  customerId: string;
  channel: ChannelType;
  displayAddress: string;
  isVerified: boolean;
  verifiedAt?: string;
  confidence: number;
}

export interface CustomerPreferences {
  customerId: string;
  preferredChannel: ChannelType;
  emailNotificationsEnabled: boolean;
  marketingConsent: boolean;
  updatedAt: string;
}

export interface EmailChallengeResponse {
  email: string;
  challengeToken?: string;
  expiresAt?: string;
  message: string;
}

export interface EmailVerifyResponse {
  channelIdentityId?: string;
  email: string;
  isVerified: boolean;
  linkedPendingIntakes: number;
  message: string;
}

// Multimodal Evidence Types
export type EvidencePipelineStatus =
  | 'QUEUED'
  | 'EXTRACTING'
  | 'REDACTING'
  | 'ANALYZING'
  | 'READY'
  | 'PARTIAL'
  | 'FAILED'
  | 'BLOCKED_REDACTION'
  | 'TOMBSTONED';

export interface EvidenceJobResponse {
  id: string;
  tenantId: string;
  ticketId: string;
  attachmentId: string;
  fileName: string;
  mediaType: string;
  fileSizeBytes: number;
  pipelineStatus: EvidencePipelineStatus;
  consentGranted: boolean;
  retentionClass: string;
  attempt: number;
  errorDetails?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EvidenceArtifactResponse {
  id: string;
  jobId: string;
  ticketId: string;
  artifactType: string;
  redactedObjectKey: string;
  pageOrFrame?: number;
  timestampSeconds?: number;
  checksumSha256: string;
  sensitivityClass: string;
  redactedContent: string;
  createdAt: string;
}

export interface EvidenceObservationResponse {
  id: string;
  jobId: string;
  ticketId: string;
  observationType: string;
  codeOrKey: string;
  summary: string;
  confidence: number;
  sourceCoordinates?: string;
  createdAt: string;
}


