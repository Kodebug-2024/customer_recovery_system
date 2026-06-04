package com.codezilla.crm.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integrations.openai.mode", havingValue = "stub", matchIfMissing = true)
public class OpenAIStubClient implements OpenAIClient {

    @Override
    public String complete(String systemPrompt, String userMessage) {
        return "Hi! Thanks for reaching out 😊 May I know what you are looking for?";
    }
}
