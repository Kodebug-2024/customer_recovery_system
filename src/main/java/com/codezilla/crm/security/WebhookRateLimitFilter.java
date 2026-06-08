package com.codezilla.crm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Fixed-window rate limit on webhook ingestion: N requests per minute per API key.
 * Counter key: rl:webhook:{apiKey}:{minute}. Expires after 2 minutes.
 */
@Component
public class WebhookRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final long limitPerMinute;

    public WebhookRateLimitFilter(StringRedisTemplate redis,
                                  @Value("${app.ratelimit.webhook.requests-per-minute:60}") long limitPerMinute) {
        this.redis = redis;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/webhook/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = req.getHeader("X-Api-Key");
        if (apiKey == null || apiKey.isBlank()) {
            chain.doFilter(req, res);
            return;
        }
        long minute = System.currentTimeMillis() / 60_000L;
        String key = "rl:webhook:" + apiKey + ":" + minute;
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofMinutes(2));
            }
        } catch (Exception ex) {
            // Redis down: fail open rather than block the webhook
            chain.doFilter(req, res);
            return;
        }
        if (count != null && count > limitPerMinute) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.getWriter().write("{\"error\":\"rate limit exceeded\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
