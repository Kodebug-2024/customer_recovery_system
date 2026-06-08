-- Soft delete on leads
ALTER TABLE leads ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_leads_not_deleted ON leads(tenant_id, deleted_at) WHERE deleted_at IS NULL;

-- Audit log
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor VARCHAR(255),
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(32) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_tenant_entity ON audit_events(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_tenant_created ON audit_events(tenant_id, created_at DESC);
