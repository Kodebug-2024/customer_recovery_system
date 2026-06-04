-- Demo seed data for local dev. Safe to delete in production.
-- Tenant API key: dev-tenant-key
-- Admin login: admin@demo.local / password123 (bcrypt hash below)

INSERT INTO tenants (id, name, industry, api_key, ai_enabled, auto_reply_template)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Demo SME',
    'retail',
    'dev-tenant-key',
    FALSE,
    'Hi {{name}}, thanks for contacting Demo SME. We will get back to you shortly.'
)
ON CONFLICT (id) DO NOTHING;

-- bcrypt of "password123"
INSERT INTO users (id, tenant_id, email, password_hash, role)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'admin@demo.local',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoO5nN9Q0V8Xo4t5p2jKjY1fY1qZ0Y0uXa',
    'ADMIN'
)
ON CONFLICT (id) DO NOTHING;
