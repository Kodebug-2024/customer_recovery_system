-- Roll back V16 (password_reset_tokens).
DROP TABLE IF EXISTS password_reset_tokens;

DELETE FROM flyway_schema_history WHERE version = '16';
