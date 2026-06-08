package com.codezilla.crm.knowledge;

import com.codezilla.crm.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Vector retrieval over knowledge_documents using cosine similarity computed
 * in Java. Sufficient up to ~10k docs per tenant. To scale further, swap to
 * pgvector + IVFFLAT index.
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final TypeReference<List<Float>> FLOAT_LIST = new TypeReference<>() {};

    private final KnowledgeDocumentRepository docs;
    private final EmbeddingClient embedder;
    private final ObjectMapper json;

    public KnowledgeService(KnowledgeDocumentRepository docs, EmbeddingClient embedder, ObjectMapper json) {
        this.docs = docs;
        this.embedder = embedder;
        this.json = json;
    }

    @Transactional
    public KnowledgeDocument createOrReplace(String title, String content) {
        KnowledgeDocument d = new KnowledgeDocument();
        d.setTitle(title);
        d.setContent(content);
        float[] emb = embedder.embed(title + "\n\n" + content);
        d.setEmbedding(emb == null ? null : encode(emb));
        return docs.save(d);
    }

    @Transactional
    public boolean reindex(UUID id) {
        return docs.findById(id).map(d -> {
            if (!d.getTenantId().equals(TenantContext.require())) return false;
            float[] emb = embedder.embed(d.getTitle() + "\n\n" + d.getContent());
            d.setEmbedding(emb == null ? null : encode(emb));
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean delete(UUID id) {
        return docs.findById(id).map(d -> {
            if (!d.getTenantId().equals(TenantContext.require())) return false;
            docs.delete(d);
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> list() {
        return docs.findAllByTenantIdOrderByCreatedAtDesc(TenantContext.require());
    }

    /**
     * Find the top-k most relevant docs for the given query in the active tenant.
     * Returns empty list if embeddings are unavailable.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> retrieve(String query, int k) {
        if (query == null || query.isBlank()) return List.of();
        float[] q = embedder.embed(query);
        if (q == null) return List.of();
        List<KnowledgeDocument> all = docs.findAllByTenantId(TenantContext.require());
        record Scored(KnowledgeDocument d, double score) {}
        List<Scored> scored = new ArrayList<>(all.size());
        for (KnowledgeDocument d : all) {
            if (d.getEmbedding() == null) continue;
            float[] e = decode(d.getEmbedding());
            if (e == null || e.length != q.length) continue;
            scored.add(new Scored(d, cosine(q, e)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.stream().limit(Math.max(1, k)).map(Scored::d).toList();
    }

    private String encode(float[] v) {
        try { return json.writeValueAsString(v); }
        catch (Exception e) { throw new IllegalStateException("encode failed", e); }
    }

    private float[] decode(String s) {
        try {
            List<Float> list = json.readValue(s, FLOAT_LIST);
            float[] out = new float[list.size()];
            for (int i = 0; i < out.length; i++) out[i] = list.get(i);
            return out;
        } catch (Exception e) {
            log.warn("Failed to decode embedding", e);
            return null;
        }
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
