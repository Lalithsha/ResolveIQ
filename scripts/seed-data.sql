-- ========================================================
-- ResolveIQ Comprehensive Demo & Benchmark Seed Dataset
-- ========================================================

-- 1. Tenants
INSERT INTO auth_schema.tenants (id, name, domain, status, created_at, updated_at)
VALUES 
  ('00000000-0000-0000-0000-000000000001', 'ACME Global Enterprise', 'acme.resolveiq.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. Demo Users (Password for all fictional users: ResolveIQ2026! => BCrypt hash)
INSERT INTO auth_schema.users (id, tenant_id, email, normalized_email, password_hash, full_name, status, failed_login_attempts, created_at, updated_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000001', 'alex.morgan@acme.com', 'alex.morgan@acme.com', '$2a$10$idQEPDOdriw/Gfbfew3fFu2RGVaUdL1hO8ta9P55hfrNRK2FH5j2u', 'Alex Morgan', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000001', 'sarah.chen@resolveiq.local', 'sarah.chen@resolveiq.local', '$2a$10$idQEPDOdriw/Gfbfew3fFu2RGVaUdL1hO8ta9P55hfrNRK2FH5j2u', 'Sarah Chen', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('33333333-3333-3333-3333-333333333333', '00000000-0000-0000-0000-000000000001', 'marcus.vance@resolveiq.local', 'marcus.vance@resolveiq.local', '$2a$10$idQEPDOdriw/Gfbfew3fFu2RGVaUdL1hO8ta9P55hfrNRK2FH5j2u', 'Marcus Vance', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('44444444-4444-4444-4444-444444444444', '00000000-0000-0000-0000-000000000001', 'elena.rostova@resolveiq.local', 'elena.rostova@resolveiq.local', '$2a$10$idQEPDOdriw/Gfbfew3fFu2RGVaUdL1hO8ta9P55hfrNRK2FH5j2u', 'Elena Rostova', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('55555555-5555-5555-5555-555555555555', '00000000-0000-0000-0000-000000000001', 'admin@resolveiq.local', 'admin@resolveiq.local', '$2a$10$idQEPDOdriw/Gfbfew3fFu2RGVaUdL1hO8ta9P55hfrNRK2FH5j2u', 'David Kross', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('66666666-6666-6666-6666-666666666666', '00000000-0000-0000-0000-000000000001', 'auditor@resolveiq.local', 'auditor@resolveiq.local', '$2a$10$idQEPDOdriw/Gfbfew3fFu2RGVaUdL1hO8ta9P55hfrNRK2FH5j2u', 'Priya Nair', 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- User Roles Map
INSERT INTO auth_schema.user_roles_map (user_id, role_name) VALUES
  ('11111111-1111-1111-1111-111111111111', 'CUSTOMER'),
  ('22222222-2222-2222-2222-222222222222', 'AGENT'),
  ('33333333-3333-3333-3333-333333333333', 'TEAM_LEAD'),
  ('44444444-4444-4444-4444-444444444444', 'KNOWLEDGE_MANAGER'),
  ('55555555-5555-5555-5555-555555555555', 'ADMIN'),
  ('66666666-6666-6666-6666-666666666666', 'AUDITOR')
ON CONFLICT (user_id, role_name) DO NOTHING;

-- 3. Teams & Routing
INSERT INTO routing_schema.teams (id, tenant_id, name, description, max_active_tickets, created_at, updated_at)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '00000000-0000-0000-0000-000000000001', 'Billing Tier 2', 'Handles invoice disputes, payment gateway reconciliation, and refund authorizations.', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '00000000-0000-0000-0000-000000000001', 'Authentication & Identity', 'Handles SAML, Okta SSO, OAuth2, and security access permissions.', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', '00000000-0000-0000-0000-000000000001', 'Logistics & Fulfillment', 'Handles tracking discrepancies, lost shipments, and carrier investigations.', 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO routing_schema.agents (id, tenant_id, team_id, name, email, status, active_ticket_count, created_at, updated_at)
VALUES
  ('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Sarah Chen', 'sarah.chen@resolveiq.local', 'ONLINE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('33333333-3333-3333-3333-333333333333', '00000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Marcus Vance', 'marcus.vance@resolveiq.local', 'ONLINE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Ticket-service access projection of routing team membership.
INSERT INTO ticket_schema.staff_team_memberships (id, tenant_id, user_id, team_id, role, active, updated_at)
VALUES
  ('a1111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'AGENT', TRUE, CURRENT_TIMESTAMP),
  ('a3333333-3333-3333-3333-333333333333', '00000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TEAM_LEAD', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, user_id, team_id) DO UPDATE SET active = TRUE, role = EXCLUDED.role, updated_at = CURRENT_TIMESTAMP;

-- Routing Rules
INSERT INTO routing_schema.routing_rules (id, tenant_id, name, version, conditions, target_team_id, priority_order, active, created_at)
VALUES
  ('71000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Billing Category Rule', 'v1.0', '{"category": "BILLING"}', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1, TRUE, CURRENT_TIMESTAMP),
  ('71000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'SSO Auth Rule', 'v1.0', '{"intent": "authentication_issue"}', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 2, TRUE, CURRENT_TIMESTAMP),
  ('71000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Delivery Rule', 'v1.0', '{"category": "DELIVERY"}', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 3, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, name, version) DO UPDATE SET
  conditions = EXCLUDED.conditions,
  target_team_id = EXCLUDED.target_team_id,
  priority_order = EXCLUDED.priority_order,
  active = EXCLUDED.active;

-- SLA Policies
INSERT INTO routing_schema.sla_policies (id, tenant_id, name, priority, first_response_target_minutes, resolution_target_minutes, business_hours_only, created_at)
VALUES
  ('72000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Critical SLA Policy', 'CRITICAL', 60, 240, FALSE, CURRENT_TIMESTAMP),
  ('72000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'High SLA Policy', 'HIGH', 120, 480, TRUE, CURRENT_TIMESTAMP),
  ('72000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Medium SLA Policy', 'MEDIUM', 240, 1440, TRUE, CURRENT_TIMESTAMP),
  ('72000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'Low SLA Policy', 'LOW', 480, 2880, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, priority) DO UPDATE SET
  name = EXCLUDED.name,
  first_response_target_minutes = EXCLUDED.first_response_target_minutes,
  resolution_target_minutes = EXCLUDED.resolution_target_minutes,
  business_hours_only = EXCLUDED.business_hours_only;

-- 4. Knowledge is intentionally not inserted with SQL. scripts/seed-data.sh
-- authenticates as a knowledge manager and drives draft -> review -> publish,
-- which is the same ingestion/indexing boundary used by the product.

-- 5. Seed Tickets
INSERT INTO ticket_schema.tickets (id, ticket_number, tenant_id, customer_id, team_id, assigned_agent_id, subject, description, language, status, priority, category, channel, ai_triage_status, created_at, updated_at)
VALUES
  ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'RIQ-2026-000412', '00000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-2222-2222-2222-222222222222',
   'Payment Failed Double Charge', 'I noticed my credit card was charged twice for invoice #INV-9812. The dashboard shows payment pending and my account is locked out of premium features. Please fix this immediately.',
   'en', 'READY_FOR_AGENT', 'HIGH', 'BILLING', 'WEB', 'SUCCESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO ticket_schema.ticket_messages (id, ticket_id, tenant_id, sender_id, sender_role, content, is_internal, created_at)
VALUES
  ('73000000-0000-0000-0000-000000000001', 'ffffffff-ffff-ffff-ffff-ffffffffffff', '00000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'CUSTOMER',
   'I noticed my credit card was charged twice for invoice #INV-9812. The dashboard shows payment pending and my account is locked out of premium features. Please fix this immediately.',
   FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO ticket_schema.ai_suggestions (id, ticket_id, tenant_id, suggested_response, confidence_score, model_name, prompt_version, citations, status, created_at)
VALUES
  ('74000000-0000-0000-0000-000000000001', 'ffffffff-ffff-ffff-ffff-ffffffffffff', '00000000-0000-0000-0000-000000000001',
   'Hello Alex, thank you for contacting support. I have verified your account and identified that the payment retry mechanism encountered a temporary gateway timeout. I have manually triggered a balance reconciliation and your invoice status has now updated to Paid. Please let us know if you need any additional assistance.',
   0.94, 'mock-chat-v1', 'triage-v1.0',
   '[{"source": "KB-104: Payment Reconciliation", "snippet": "When a gateway timeout causes duplicate pending records, reconcile balance from payment processor..."}]',
   'PENDING_REVIEW', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
