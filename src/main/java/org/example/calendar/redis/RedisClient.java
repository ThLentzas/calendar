package org.example.calendar.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.example.calendar.auth.dto.RefreshToken;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisClient {
    private final RedisTemplate<String, RefreshToken> redisTemplate;

    public void set(String key, RefreshToken value, long ttl, TimeUnit timeUnit) {
        this.redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    public RefreshToken get(String key) {
        return this.redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        this.redisTemplate.delete(key);
    }
}
