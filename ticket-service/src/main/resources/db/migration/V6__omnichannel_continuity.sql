-- V6: Omnichannel Continuity and Intelligent Handoff
-- Canonical conversation model, channel identities, message metadata, and handoff tables

-- 1. Canonical Conversations table
CREATE TABLE IF NOT EXISTS ticket_schema.conversations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    primary_customer_id UUID NOT NULL,
    assigned_agent_id UUID,
    handoff_state VARCHAR(50) NOT NULL DEFAULT 'NONE', -- NONE, REQUESTED, QUEUED, ASSIGNED, REJECTED
    handoff_requested_at TIMESTAMPTZ,
    preferred_channel VARCHAR(50) NOT NULL DEFAULT 'PORTAL', -- PORTAL, EMAIL
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conversation_tenant_ticket UNIQUE (tenant_id, ticket_id)
);

CREATE INDEX IF NOT EXISTS idx_conversations_tenant_customer ON ticket_schema.conversations(tenant_id, primary_customer_id);
CREATE INDEX IF NOT EXISTS idx_conversations_tenant_agent ON ticket_schema.conversations(tenant_id, assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_conversations_tenant_handoff ON ticket_schema.conversations(tenant_id, handoff_state);

-- 2. Channel Identities
CREATE TABLE IF NOT EXISTS ticket_schema.channel_identities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID,
    channel VARCHAR(50) NOT NULL, -- EMAIL, PORTAL
    address_hmac VARCHAR(128) NOT NULL,
    display_address VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_challenge_hash VARCHAR(128),
    challenge_expires_at TIMESTAMPTZ,
    challenge_attempts INT NOT NULL DEFAULT 0,
    verified_at TIMESTAMPTZ,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_channel_identity_address UNIQUE (tenant_id, channel, address_hmac)
);

CREATE INDEX IF NOT EXISTS idx_channel_identities_tenant_customer ON ticket_schema.channel_identities(tenant_id, customer_id);

-- 3. Conversation Participants
CREATE TABLE IF NOT EXISTS ticket_schema.conversation_participants (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES ticket_schema.conversations(id) ON DELETE CASCADE,
    user_id UUID,
    role VARCHAR(50) NOT NULL, -- CUSTOMER, AGENT, SYSTEM, UNVERIFIED_EXTERNAL
    channel_identity_id UUID REFERENCES ticket_schema.channel_identities(id) ON DELETE SET NULL,
    verification_state VARCHAR(50) NOT NULL DEFAULT 'VERIFIED', -- VERIFIED, UNVERIFIED, PENDING_CHALLENGE
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conv_participants_tenant_conv ON ticket_schema.conversation_participants(tenant_id, conversation_id);

-- 4. Channel Message Metadata (1-to-1 extension of ticket_messages)
CREATE TABLE IF NOT EXISTS ticket_schema.channel_message_metadata (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    message_id UUID NOT NULL UNIQUE REFERENCES ticket_schema.ticket_messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES ticket_schema.conversations(id) ON DELETE CASCADE,
    channel VARCHAR(50) NOT NULL, -- PORTAL, EMAIL
    direction VARCHAR(50) NOT NULL, -- INBOUND, OUTBOUND, INTERNAL
    external_message_id VARCHAR(255),
    sender_address VARCHAR(255),
    recipient_address VARCHAR(255),
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'DELIVERED', -- PENDING, DELIVERED, BOUNCED, FAILED, UNKNOWN
    idempotency_key VARCHAR(255),
    provider_timestamp TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_msg_meta_tenant_conv ON ticket_schema.channel_message_metadata(tenant_id, conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_msg_meta_external ON ticket_schema.channel_message_metadata(tenant_id, external_message_id);

-- 5. Delivery Attempts
CREATE TABLE IF NOT EXISTS ticket_schema.delivery_attempts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    message_id UUID NOT NULL REFERENCES ticket_schema.ticket_messages(id) ON DELETE CASCADE,
    channel VARCHAR(50) NOT NULL,
    recipient_address VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- DELIVERED, BOUNCED, FAILED, RETRYING
    provider_message_id VARCHAR(255),
    error_details TEXT,
    attempt_number INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_delivery_attempts_msg ON ticket_schema.delivery_attempts(tenant_id, message_id);

-- 6. Conversation Merge Records
CREATE TABLE IF NOT EXISTS ticket_schema.conversation_merge_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    source_conversation_id UUID NOT NULL REFERENCES ticket_schema.conversations(id) ON DELETE CASCADE,
    target_conversation_id UUID NOT NULL REFERENCES ticket_schema.conversations(id) ON DELETE CASCADE,
    merged_by_user_id UUID NOT NULL,
    reason TEXT NOT NULL,
    split_at TIMESTAMPTZ,
    split_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Customer Channel Preferences
CREATE TABLE IF NOT EXISTS ticket_schema.customer_preferences (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    preferred_channel VARCHAR(50) NOT NULL DEFAULT 'PORTAL',
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_preferences UNIQUE (tenant_id, customer_id)
);

-- 8. Consent Records
CREATE TABLE IF NOT EXISTS ticket_schema.consent_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    consented BOOLEAN NOT NULL DEFAULT TRUE,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_or_source VARCHAR(100) NOT NULL DEFAULT 'PORTAL'
);

CREATE INDEX IF NOT EXISTS idx_consent_tenant_customer ON ticket_schema.consent_records(tenant_id, customer_id);

-- 9. Handoff Summaries
CREATE TABLE IF NOT EXISTS ticket_schema.handoff_summaries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES ticket_schema.conversations(id) ON DELETE CASCADE,
    ticket_id UUID NOT NULL REFERENCES ticket_schema.tickets(id) ON DELETE CASCADE,
    issue_summary TEXT NOT NULL,
    verified_facts TEXT NOT NULL,
    attempted_steps TEXT NOT NULL,
    promised_actions TEXT NOT NULL,
    sentiment VARCHAR(50) NOT NULL,
    open_questions TEXT NOT NULL,
    created_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_handoff_tenant_conv ON ticket_schema.handoff_summaries(tenant_id, conversation_id);

-- 10. Webhook Inbox Events (Replay and Deduplication)
CREATE TABLE IF NOT EXISTS ticket_schema.webhook_inbox_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider VARCHAR(100) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_webhook_inbox_provider_event UNIQUE (tenant_id, provider, external_event_id)
);

-- 11. Unverified Email Intakes (Pending Quarantined Intake)
CREATE TABLE IF NOT EXISTS ticket_schema.unverified_email_intakes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_hmac VARCHAR(128) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_text TEXT NOT NULL,
    external_message_id VARCHAR(255) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED_LINKED, EXPIRED, REJECTED
    promoted_ticket_id UUID,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days')
);

CREATE INDEX IF NOT EXISTS idx_unverified_intake_tenant_hmac ON ticket_schema.unverified_email_intakes(tenant_id, sender_hmac);

-- 12. Deterministic Backfill of Existing Tickets and Messages
-- Backfill tickets into conversations
INSERT INTO ticket_schema.conversations (
    id, tenant_id, ticket_id, status, primary_customer_id, assigned_agent_id, handoff_state, preferred_channel, version, created_at, updated_at
)
SELECT t.id, t.tenant_id, t.id,
       CASE WHEN t.status = 'CLOSED' THEN 'CLOSED' ELSE 'ACTIVE' END,
       t.customer_id, t.assigned_agent_id, 'NONE', 'PORTAL', 0, t.created_at, t.updated_at
FROM ticket_schema.tickets t
ON CONFLICT (id) DO NOTHING;

-- Backfill ticket_messages into channel_message_metadata
INSERT INTO ticket_schema.channel_message_metadata (
    id, tenant_id, message_id, conversation_id, channel, direction, delivery_status, created_at
)
SELECT m.id, m.tenant_id, m.id, m.ticket_id, 'PORTAL',
       CASE WHEN m.is_internal = TRUE THEN 'INTERNAL'
            WHEN m.sender_role = 'CUSTOMER' THEN 'INBOUND'
            ELSE 'OUTBOUND' END,
       'DELIVERED', m.created_at
FROM ticket_schema.ticket_messages m
ON CONFLICT (message_id) DO NOTHING;
