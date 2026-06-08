CREATE TABLE webhook_subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    target_url VARCHAR(2048) NOT NULL,
    secret_enc BYTEA NOT NULL,
    events TEXT NOT NULL,                  -- comma-separated event names
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failure_count INT NOT NULL DEFAULT 0,
    last_success_at TIMESTAMP WITH TIME ZONE,
    last_failure_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_webhook_subs_tenant ON webhook_subscriptions(tenant_id);
CREATE INDEX idx_webhook_subs_enabled ON webhook_subscriptions(enabled) WHERE enabled;
