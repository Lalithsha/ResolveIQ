-- ============================================================================
-- ResolveIQ V5: Support Incident Radar & Proactive Communication Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS support_incidents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_number VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    owner_user_id UUID,
    created_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    summary TEXT,
    root_cause TEXT,
    resolution_summary TEXT,
    detection_algorithm_version VARCHAR(64),
    cluster_confidence DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_support_incidents_tenant_number UNIQUE (tenant_id, incident_number)
);

CREATE INDEX IF NOT EXISTS idx_support_incidents_tenant_status ON support_incidents(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_support_incidents_tenant_detected ON support_incidents(tenant_id, detected_at DESC);

CREATE TABLE IF NOT EXISTS incident_clusters (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_id UUID REFERENCES support_incidents(id) ON DELETE SET NULL,
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    centroid_reference VARCHAR(128),
    centroid_hash VARCHAR(64),
    ticket_count INT NOT NULL,
    baseline_count INT NOT NULL,
    anomaly_score DOUBLE PRECISION NOT NULL,
    dominant_category VARCHAR(64) NOT NULL,
    product VARCHAR(128) NOT NULL,
    region VARCHAR(64),
    error_fingerprints JSONB,
    status VARCHAR(32) NOT NULL,
    explanation TEXT,
    algorithm_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incident_clusters_tenant_status ON incident_clusters(tenant_id, status);

CREATE TABLE IF NOT EXISTS incident_ticket_links (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES support_incidents(id) ON DELETE CASCADE,
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    link_source VARCHAR(32) NOT NULL,
    similarity_score DOUBLE PRECISION NOT NULL,
    linked_by UUID NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    unlinked_at TIMESTAMP WITH TIME ZONE,
    unlink_reason TEXT,
    CONSTRAINT uq_incident_ticket_links UNIQUE (incident_id, ticket_id)
);

CREATE INDEX IF NOT EXISTS idx_incident_ticket_links_ticket ON incident_ticket_links(ticket_id);

CREATE TABLE IF NOT EXISTS incident_components (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_id UUID NOT NULL REFERENCES support_incidents(id) ON DELETE CASCADE,
    component_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    impact_summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incident_components_tenant_incident ON incident_components(tenant_id, incident_id);

CREATE TABLE IF NOT EXISTS incident_updates (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_id UUID NOT NULL REFERENCES support_incidents(id) ON DELETE CASCADE,
    update_number INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    audience_type VARCHAR(32) NOT NULL,
    audience_count INT NOT NULL DEFAULT 0,
    author_id UUID NOT NULL,
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    content_digest VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incident_updates_tenant_incident ON incident_updates(tenant_id, incident_id);

CREATE TABLE IF NOT EXISTS customer_impacts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_id UUID NOT NULL REFERENCES support_incidents(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL,
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    impact_level VARCHAR(32) NOT NULL,
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customer_impacts UNIQUE (incident_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_customer_impacts_tenant_customer ON customer_impacts(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS notification_subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_id UUID NOT NULL REFERENCES support_incidents(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,
    subscribed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_subscriptions UNIQUE (incident_id, customer_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_notification_subscriptions_customer ON notification_subscriptions(tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS notification_deliveries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    update_id UUID NOT NULL REFERENCES incident_updates(id) ON DELETE CASCADE,
    recipient_customer_id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    delivery_key VARCHAR(128) NOT NULL,
    provider_message_id VARCHAR(128),
    failure_reason TEXT,
    attempt_count INT NOT NULL DEFAULT 0,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_deliveries UNIQUE (tenant_id, update_id, recipient_customer_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_notification_deliveries_lookup ON notification_deliveries(tenant_id, update_id, status);
