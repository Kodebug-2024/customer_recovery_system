package com.codezilla.crm.ai;

import com.codezilla.crm.integration.OpenAIClient;
import com.codezilla.crm.tenant.Tenant;
import org.springframework.stereotype.Service;

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

    private final OpenAIClient ai;

    public AiReplyService(OpenAIClient ai) {
        this.ai = ai;
    }

    public String generate(Tenant tenant, String customerMessage) {
        String industry = tenant.getIndustry() == null ? "general" : tenant.getIndustry();
        String system = String.format(SYSTEM_TEMPLATE, industry);
        return ai.complete(system, customerMessage == null ? "" : customerMessage);
    }
}
