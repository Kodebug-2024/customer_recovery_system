package com.codezilla.crm.integration;

import java.util.List;

public interface WhatsAppClient {

    /** Free-form text. Meta only allows this within the 24h customer-service window. */
    void sendText(String toPhone, String body);

    /**
     * Pre-approved template message. Required for sends outside the 24h window.
     * {@code parameters} are positional placeholders (`{{1}}`, `{{2}}`, …) in
     * the template body as configured in Meta Business Manager.
     */
    void sendTemplate(String toPhone, String templateName, String languageCode, List<String> parameters);
}
