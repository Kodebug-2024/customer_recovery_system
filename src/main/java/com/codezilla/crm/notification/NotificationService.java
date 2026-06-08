package com.codezilla.crm.notification;

import com.codezilla.crm.integration.EmailClient;
import com.codezilla.crm.integration.TelegramClient;
import com.codezilla.crm.integration.TenantCredentialResolver;
import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.tenant.Tenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final TelegramClient telegram;
    private final EmailClient email;
    private final TenantCredentialResolver creds;
    private final String ownerEmail;

    public NotificationService(TelegramClient telegram,
                               EmailClient email,
                               TenantCredentialResolver creds,
                               @Value("${integrations.email.owner:}") String ownerEmail) {
        this.telegram = telegram;
        this.email = email;
        this.creds = creds;
        this.ownerEmail = ownerEmail;
    }

    public void notifyNewLead(Tenant tenant, Lead lead) {
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

        String chatId = creds.telegram().chatId();
        if (chatId != null && !chatId.isBlank()) {
            telegram.sendMessage(chatId, text);
        }
        if (ownerEmail != null && !ownerEmail.isBlank()) {
            email.send(ownerEmail, "New lead: " + nullSafe(lead.getName()), text);
        }
    }

    private static String nullSafe(String s) { return s == null ? "-" : s; }
}
