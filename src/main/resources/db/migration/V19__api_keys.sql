CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    -- SHA-256 of the cleartext key. Cleartext is shown to the user exactly once.
    key_hash VARCHAR(128) NOT NULL UNIQUE,
    -- Last 4 chars of the cleartext, for UI display (e.g. "abcd").
    key_suffix VARCHAR(8) NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_api_keys_tenant_user ON api_keys(tenant_id, user_id);
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash) WHERE revoked_at IS NULL;
