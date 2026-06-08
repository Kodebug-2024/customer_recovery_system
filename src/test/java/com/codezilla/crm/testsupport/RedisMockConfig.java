package com.codezilla.crm.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides a no-op StringRedisTemplate so the rate-limit filter wires up in tests
 * without requiring a running Redis instance.
 */
@TestConfiguration
public class RedisMockConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate t = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(t.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);
        return t;
    }
}
