-- Roll back V17 (message_templates). Run manually if you need to undo.
-- Keeps tenants.auto_reply_template intact since V17 only seeded from it (additive).
DROP TABLE IF EXISTS message_templates;

DELETE FROM flyway_schema_history WHERE version = '17';
