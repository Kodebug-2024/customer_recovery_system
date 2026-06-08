-- Per-tenant public booking page slug.
ALTER TABLE tenants ADD COLUMN booking_slug VARCHAR(64) UNIQUE;
ALTER TABLE tenants ADD COLUMN booking_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenants ADD COLUMN booking_blurb TEXT;

-- Confirmed/requested appointments for leads.
CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 30,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_appointments_tenant_starts ON appointments(tenant_id, starts_at);
CREATE INDEX idx_appointments_lead ON appointments(lead_id);
