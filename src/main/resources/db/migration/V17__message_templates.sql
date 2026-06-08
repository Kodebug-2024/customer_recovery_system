CREATE TABLE message_templates (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    channel VARCHAR(32) NOT NULL,         -- whatsapp | telegram | email | any
    event VARCHAR(32) NOT NULL,           -- auto_reply | new_lead_notify | follow_up | custom
    subject VARCHAR(255),                 -- email only
    body TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);
CREATE INDEX idx_templates_tenant ON message_templates(tenant_id);
CREATE INDEX idx_templates_lookup ON message_templates(tenant_id, channel, event);

-- Seed the existing auto_reply_template as a template per tenant, so the
-- editor has something to start with. (No-op if auto_reply_template is null.)
INSERT INTO message_templates (id, tenant_id, name, channel, event, body, is_default)
SELECT gen_random_uuid(), id, 'Default auto-reply', 'whatsapp', 'auto_reply',
       auto_reply_template, TRUE
  FROM tenants
 WHERE auto_reply_template IS NOT NULL
   AND auto_reply_template <> '';
