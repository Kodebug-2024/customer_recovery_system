-- User management additions
ALTER TABLE users ADD COLUMN name VARCHAR(255);
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_users_tenant_email ON users(tenant_id, email);
