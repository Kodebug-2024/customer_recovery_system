package com.codezilla.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fixed-window rate limit on POST /auth/login: counts attempts per IP and per
 * email within the current minute. Fails open if Redis is unreachable rather
 * than locking users out due to infra problems.
 *
 * Reads the body once and caches it via a CachedBodyHttpServletRequest so the
 * downstream controller can still deserialize the JSON.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final long limitPerMinute;

    public LoginRateLimitFilter(StringRedisTemplate redis,
                                ObjectMapper json,
                                @Value("${app.ratelimit.login.requests-per-minute:10}") long limitPerMinute) {
        this.redis = redis;
        this.json = json;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        com.codezilla.crm.webhook.CachedBodyHttpServletRequest cached =
                new com.codezilla.crm.webhook.CachedBodyHttpServletRequest(req);

        String ip = clientIp(cached);
        String email = extractEmail(cached);
        long minute = System.currentTimeMillis() / 60_000L;

        if (overLimit("rl:login:ip:" + ip + ":" + minute)
                || (email != null && overLimit("rl:login:email:" + email + ":" + minute))) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"error\":\"Too many attempts. Try again in a minute.\"}");
            return;
        }
        chain.doFilter(cached, res);
    }

    private boolean overLimit(String key) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) redis.expire(key, Duration.ofMinutes(2));
            return count != null && count > limitPerMinute;
        } catch (Exception ex) {
            log.debug("Redis unavailable for rate-limit key {}, failing open", key, ex);
            return false;
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(com.codezilla.crm.webhook.CachedBodyHttpServletRequest req) {
        try (BufferedReader r = new BufferedReader(new java.io.InputStreamReader(
                new java.io.ByteArrayInputStream(req.body()), StandardCharsets.UTF_8))) {
            Map<String, Object> body = json.readValue(r, HashMap.class);
            Object e = body.get("email");
            return e == null ? null : e.toString().toLowerCase();
        } catch (Exception ex) {
            return null;
        }
    }
}
