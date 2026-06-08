-- Per-tenant subscription state. Plan defaults to FREE; UPGRADE creates a subscription row.
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    plan VARCHAR(32) NOT NULL DEFAULT 'FREE',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    stripe_customer_id VARCHAR(80),
    stripe_subscription_id VARCHAR(80),
    current_period_end TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_subscriptions_stripe_sub ON subscriptions(stripe_subscription_id) WHERE stripe_subscription_id IS NOT NULL;
CREATE INDEX idx_subscriptions_stripe_cust ON subscriptions(stripe_customer_id) WHERE stripe_customer_id IS NOT NULL;
