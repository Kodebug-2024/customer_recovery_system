package com.codezilla.crm.observability;

import com.codezilla.crm.tenant.TenantContext;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Tags every Sentry event with the active tenant so we can filter errors per
 * customer in the Sentry UI. Strips request bodies — they may contain PII
 * (lead messages, phone numbers).
 *
 * Only wires up when SENTRY_DSN is set; otherwise the starter is inert and
 * this bean is harmless.
 */
@Configuration
@ConditionalOnProperty(name = "sentry.dsn")
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback(@Value("${sentry.environment:}") String env) {
        return (SentryEvent event, io.sentry.Hint hint) -> {
            // Don't ship request bodies — could contain customer messages, phone numbers, etc.
            if (event.getRequest() != null) {
                event.getRequest().setData(null);
                event.getRequest().setCookies(null);
            }

            // Attach tenant ID as a tag (useful for "errors for tenant X" queries).
            UUID tenantId = safeTenant();
            if (tenantId != null) {
                event.setTag("tenant_id", tenantId.toString());
                User user = event.getUser() == null ? new User() : event.getUser();
                user.setId(tenantId.toString()); // not the real user; helps grouping
                event.setUser(user);
            }
            return event;
        };
    }

    private static UUID safeTenant() {
        try { return TenantContext.get(); } catch (Exception e) { return null; }
    }
}
