package com.codezilla.crm.webhook.outbound;

import com.codezilla.crm.security.SecretCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Asynchronous outbound webhook delivery with HMAC-SHA256 signing.
 *
 * Each delivery posts JSON to the subscriber's target URL. The body is signed
 * with the subscriber's encrypted shared secret; the signature appears in the
 * {@code X-Crm-Signature-256} header (`sha256=<hex>` — same format as Stripe
 * and Meta). On failure we bump {@code failureCount}; after 10 consecutive
 * failures the subscription auto-disables to stop wasting resources.
 *
 * Retry strategy is deliberately simple: one attempt per event, but with a
 * 5s timeout so a slow subscriber can't back-pressure us. For production-grade
 * retries swap this for a queue (Redis Streams / Kafka).
 */
@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    private final WebhookSubscriptionRepository subs;
    private final SecretCipher cipher;
    private final ObjectMapper json;
    private final WebClient client;

    public WebhookDeliveryService(WebhookSubscriptionRepository subs,
                                  SecretCipher cipher,
                                  ObjectMapper json,
                                  WebClient.Builder builder) {
        this.subs = subs;
        this.cipher = cipher;
        this.json = json;
        this.client = builder.build();
    }

    /**
     * Fire-and-forget delivery. Looks up every enabled subscription for the
     * tenant that handles {@code eventName} and POSTs the payload.
     */
    @Async
    @Transactional
    public void publish(UUID tenantId, String eventName, Object payload) {
        for (WebhookSubscription s : subs.findAllByTenantIdAndEnabledTrue(tenantId)) {
            if (!s.handles(eventName)) continue;
            deliver(s, eventName, payload);
        }
    }

    private void deliver(WebhookSubscription s, String eventName, Object payload) {
        try {
            String body = json.writeValueAsString(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "event", eventName,
                    "tenantId", s.getTenantId().toString(),
                    "createdAt", Instant.now().toString(),
                    "data", payload));
            String secret = cipher.decrypt(s.getSecretEnc());
            String sig = "sha256=" + hmac(secret, body);
            client.post()
                    .uri(s.getTargetUrl())
                    .header("Content-Type", "application/json")
                    .header("X-Crm-Event", eventName)
                    .header("X-Crm-Signature-256", sig)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();
            s.setLastSuccessAt(Instant.now());
            s.setFailureCount(0);
            s.setLastError(null);
            subs.save(s);
        } catch (Exception e) {
            int count = s.getFailureCount() + 1;
            s.setFailureCount(count);
            s.setLastFailureAt(Instant.now());
            s.setLastError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            if (count >= MAX_CONSECUTIVE_FAILURES) {
                s.setEnabled(false);
                log.warn("Auto-disabling webhook subscription {} after {} failures", s.getId(), count);
            } else {
                log.warn("Webhook delivery to {} failed ({}/{}): {}",
                        s.getTargetUrl(), count, MAX_CONSECUTIVE_FAILURES, e.toString());
            }
            subs.save(s);
        }
    }

    private static String hmac(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }
}
