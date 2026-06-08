-- Knowledge base for RAG. Embeddings stored as JSON text for portability
-- (works with any Postgres + with H2 in tests). Cosine similarity is computed
-- in Java. Switch to pgvector + IVFFLAT index when corpus exceeds ~10k docs.
CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_knowledge_documents_tenant ON knowledge_documents(tenant_id);
