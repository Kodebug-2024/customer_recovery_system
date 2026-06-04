package com.codezilla.crm.notification;

import com.codezilla.crm.integration.TelegramClient;
import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.tenant.Tenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final TelegramClient telegram;
    private final String defaultChatId;

    public NotificationService(TelegramClient telegram,
                               @Value("${integrations.telegram.default-chat-id:}") String defaultChatId) {
        this.telegram = telegram;
        this.defaultChatId = defaultChatId;
    }

    public void notifyNewLead(Tenant tenant, Lead lead) {
        if (defaultChatId == null || defaultChatId.isBlank()) return;
        String text = """
                🚨 New Lead [%s]
                Name: %s
                Phone: %s
                Source: %s
                Message: %s
                """.formatted(
                tenant.getName(),
                nullSafe(lead.getName()),
                nullSafe(lead.getPhone()),
                nullSafe(lead.getSource()),
                nullSafe(lead.getMessage()));
        telegram.sendMessage(defaultChatId, text);
    }

    private static String nullSafe(String s) { return s == null ? "-" : s; }
}
