-- Demo tenant. The admin user is provisioned by DemoDataBootstrap on first startup
-- so the BCrypt password hash matches the configured PasswordEncoder.
INSERT INTO tenants (id, name, industry, api_key, ai_enabled, auto_reply_template)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Demo SME',
    'general',
    'dev-tenant-key',
    FALSE,
    'Hi {{name}}, thanks for contacting Demo SME. We will get back to you shortly.'
)
ON CONFLICT (id) DO NOTHING;
