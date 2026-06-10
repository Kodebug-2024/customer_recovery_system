-- AI provider routing + rule-based FAQ engine.
--
-- ai_provider on tenants:
--   'auto'   = use global default (controlled by AI_DEFAULT_PROVIDER env)
--   'faq'    = rule-based FAQ only; no LLM
--   'ollama' = local LLM (free; runs in the ollama container)
--   'openai' = OpenAI API (paid; requires Pro plan)
--
-- FAQ matching is dead-simple substring match on customer message, case-insensitive,
-- highest priority wins. Patterns are stored as plain text (not regex) for safety —
-- non-technical SME owners write them. The matcher tries each pattern in
-- (priority DESC, length DESC) order so "shipping cost to Singapore" beats "shipping".

ALTER TABLE tenants ADD COLUMN ai_provider VARCHAR(16) NOT NULL DEFAULT 'auto';

CREATE TABLE faqs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    pattern TEXT NOT NULL,
    reply TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    hit_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_faqs_tenant ON faqs(tenant_id, priority DESC);
