package com.codezilla.crm.ai;

import com.codezilla.crm.knowledge.KnowledgeDocument;
import com.codezilla.crm.knowledge.KnowledgeService;
import com.codezilla.crm.tenant.Tenant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AiReplyService {

    private static final String SYSTEM_TEMPLATE = """
            You are a sales assistant for an SME business.
            Business type: %s
            Rules:
            - Keep replies short
            - Be polite
            - Ask 1 follow-up question
            - Focus on conversion
            """;

    private static final String CONTEXT_TEMPLATE = """

            Use the following business knowledge to answer accurately. If a question
            cannot be answered from this context, say so politely and offer to follow up.

            === KNOWLEDGE ===
            %s
            === END KNOWLEDGE ===
            """;

    private final LlmRouter router;
    private final KnowledgeService knowledge;

    public AiReplyService(LlmRouter router, KnowledgeService knowledge) {
        this.router = router;
        this.knowledge = knowledge;
    }

    /** Returns the generated reply text, or empty string when no provider could answer. */
    public String generate(Tenant tenant, String customerMessage) {
        String industry = tenant.getIndustry() == null ? "general" : tenant.getIndustry();
        StringBuilder system = new StringBuilder(String.format(SYSTEM_TEMPLATE, industry));

        // Retrieval-augmented: pull top-3 relevant knowledge docs (silently disabled
        // when embedding provider isn't configured).
        List<KnowledgeDocument> ctx = knowledge.retrieve(customerMessage, 3);
        if (!ctx.isEmpty()) {
            StringBuilder block = new StringBuilder();
            for (KnowledgeDocument d : ctx) {
                block.append("# ").append(d.getTitle()).append("\n")
                     .append(d.getContent()).append("\n\n");
            }
            system.append(String.format(CONTEXT_TEMPLATE, block));
        }

        Optional<LlmRouter.Reply> reply = router.respond(tenant, system.toString(), customerMessage);
        return reply.map(LlmRouter.Reply::text).orElse("");
    }
}
