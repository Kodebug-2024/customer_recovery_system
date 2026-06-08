-- Replace the legacy free-text assigned_to column with a real FK to users(id).
-- Allow NULL = unassigned. ON DELETE SET NULL: if a user is removed, leads remain but become unassigned.
ALTER TABLE leads DROP COLUMN IF EXISTS assigned_to;
ALTER TABLE leads ADD COLUMN assigned_to_user_id UUID
    REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_leads_assigned_to_user ON leads(tenant_id, assigned_to_user_id)
    WHERE assigned_to_user_id IS NOT NULL;
