package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(name = "integrations.telegram.mode", havingValue = "stub", matchIfMissing = true)
public class TelegramStubClient implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramStubClient.class);
    private final List<String> outbox = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendMessage(String chatId, String text) {
        String entry = "[telegram] -> " + chatId + " :: " + text;
        outbox.add(entry);
        log.info("STUB {}", entry);
    }

    public List<String> outbox() {
        return List.copyOf(outbox);
    }
}
