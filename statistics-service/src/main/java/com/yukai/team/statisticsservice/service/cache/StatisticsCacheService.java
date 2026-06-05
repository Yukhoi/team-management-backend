package com.yukai.team.statisticsservice.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value == null ? Optional.empty() : Optional.of(objectMapper.convertValue(value, type));
        } catch (RuntimeException ex) {
            log.warn("Statistics cache read failed, key={}, reason={}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (RuntimeException ex) {
            log.warn("Statistics cache write failed, key={}, reason={}", key, ex.getMessage());
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Statistics cache delete failed, key={}, reason={}", key, ex.getMessage());
        }
    }

    public void deleteByPrefix(String prefix) {
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(1000)
                .build())) {
            List<String> keys = new ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ex) {
            log.warn("Statistics cache prefix delete failed, prefix={}, reason={}", prefix, ex.getMessage());
        }
    }
}
