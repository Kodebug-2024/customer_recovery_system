-- Roll back V18 (webhook_subscriptions).
DROP TABLE IF EXISTS webhook_subscriptions;
DELETE FROM flyway_schema_history WHERE version = '18';
