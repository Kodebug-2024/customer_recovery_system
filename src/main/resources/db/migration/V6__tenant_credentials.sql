-- Per-tenant integration credentials. Sensitive values stored encrypted as bytea.
ALTER TABLE tenants ADD COLUMN whatsapp_phone_number_id VARCHAR(64);
ALTER TABLE tenants ADD COLUMN whatsapp_access_token_enc BYTEA;
ALTER TABLE tenants ADD COLUMN whatsapp_verify_token_enc BYTEA;
ALTER TABLE tenants ADD COLUMN telegram_bot_token_enc BYTEA;
ALTER TABLE tenants ADD COLUMN telegram_chat_id VARCHAR(64);
ALTER TABLE tenants ADD COLUMN openai_api_key_enc BYTEA;
