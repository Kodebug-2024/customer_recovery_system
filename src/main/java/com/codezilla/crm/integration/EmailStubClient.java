package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "integrations.email.mode", havingValue = "stub", matchIfMissing = true)
public class EmailStubClient implements EmailClient {
    private static final Logger log = LoggerFactory.getLogger(EmailStubClient.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[STUB EMAIL] to={} subject={} body={}", to, subject, body);
    }
}
