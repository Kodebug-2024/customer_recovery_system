package com.codezilla.crm.integration;

public interface OpenAIClient {
    String complete(String systemPrompt, String userMessage);
}
