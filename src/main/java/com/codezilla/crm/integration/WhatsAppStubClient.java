package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(name = "integrations.whatsapp.mode", havingValue = "stub", matchIfMissing = true)
public class WhatsAppStubClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppStubClient.class);
    private final List<String> outbox = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendText(String toPhone, String body) {
        String entry = "[whatsapp] -> " + toPhone + " :: " + body;
        outbox.add(entry);
        log.info("STUB {}", entry);
    }

    public List<String> outbox() {
        return List.copyOf(outbox);
    }
}
