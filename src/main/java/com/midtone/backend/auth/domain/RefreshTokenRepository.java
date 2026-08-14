package com.midtone.backend.auth.domain;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(long userId, String refreshToken, Duration expiration) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, expiration);
    }

    public Optional<String> findByUserId(long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    public void deleteByUserId(long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(long userId) {
        return KEY_PREFIX + userId;
    }
}
