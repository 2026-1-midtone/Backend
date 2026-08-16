package com.midtone.backend.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LogoutRepository {

    private static final String KEY_PREFIX = "logout-at:";

    private final StringRedisTemplate redisTemplate;

    public LogoutRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(long userId, Instant loggedOutAt, Duration expiration) {
        redisTemplate.opsForValue().set(key(userId), String.valueOf(loggedOutAt.toEpochMilli()), expiration);
    }

    public Optional<Instant> findByUserId(long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)))
                .map(Long::parseLong)
                .map(Instant::ofEpochMilli);
    }

    public void deleteByUserId(long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(long userId) {
        return KEY_PREFIX + userId;
    }
}
