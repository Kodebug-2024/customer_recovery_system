-- Anti-abuse: per-user failed login counter and lock window.
ALTER TABLE users ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;

-- Email verification.
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(80);
ALTER TABLE users ADD COLUMN email_verification_expires_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token)
    WHERE email_verification_token IS NOT NULL;

-- Existing demo user is grandfathered as verified so dev login still works.
UPDATE users SET email_verified_at = NOW() WHERE email_verified_at IS NULL;
