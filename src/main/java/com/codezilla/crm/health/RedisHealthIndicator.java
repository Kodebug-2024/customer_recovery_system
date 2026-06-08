package com.codezilla.crm.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Reports Redis liveness in the actuator health response. Spring Boot
 * already includes one, but the auto-configured RedisHealthIndicator is
 * picky about Lettuce vs Jedis; this is a minimal PING-based check that
 * works regardless of the underlying client.
 *
 * Skipped when there is no RedisConnectionFactory bean (e.g. in tests where
 * Redis autoconfig is disabled).
 */
@Component("redis")
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory factory;

    public RedisHealthIndicator(RedisConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Health health() {
        try (var conn = factory.getConnection()) {
            String reply = conn.ping();
            if ("PONG".equalsIgnoreCase(reply)) {
                return Health.up().withDetail("ping", reply).build();
            }
            return Health.down().withDetail("ping", String.valueOf(reply)).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
