-- Rollback V20: ai_provider column + faqs table.
DROP TABLE IF EXISTS faqs;
ALTER TABLE tenants DROP COLUMN IF EXISTS ai_provider;
