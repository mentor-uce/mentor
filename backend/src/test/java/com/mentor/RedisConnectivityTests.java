package com.mentor;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import({PostgreSqlTestConfiguration.class, MongoDbTestConfiguration.class, RedisTestConfiguration.class, JwtTestConfiguration.class})
class RedisConnectivityTests {
    @Autowired
    private ApplicationContext context;

    @Test
    void ephemeralValueCanBeWrittenReadAndRemoved() {
        var redis = context.getBean(StringRedisTemplate.class);
        var key = "mentor:test:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "connected", Duration.ofMinutes(1));
            assertEquals("connected", redis.opsForValue().get(key));
            long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            assertTrue(ttl > 0 && ttl <= 60, "Cache entries must have a finite lifetime");
            try (var connection = redis.getConnectionFactory().getConnection()) {
                assertEquals("PONG", connection.ping());
            }
        } finally {
            redis.delete(key);
        }
    }
}