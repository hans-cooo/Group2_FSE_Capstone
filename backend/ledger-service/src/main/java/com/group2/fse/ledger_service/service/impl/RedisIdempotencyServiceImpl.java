package com.group2.fse.ledger_service.service.impl;

import com.group2.fse.ledger_service.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Task: FSE-301
 * Assigned to: Carl
 * Redis implementation of IdempotencyService utilizing atomic SETNX and response caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyServiceImpl implements IdempotencyService {

    private static final String KEY_PREFIX = "idemp:";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryAcquire(String idempotencyKey, Duration lockTtl) {
        String key = formatKey(idempotencyKey);
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, STATUS_PROCESSING, lockTtl);
        boolean success = Boolean.TRUE.equals(acquired);
        if (success) {
            log.debug("Idempotency lock acquired for key: {}", idempotencyKey);
        } else {
            log.warn("Duplicate request detected for idempotency key: {}", idempotencyKey);
        }
        return success;
    }

    @Override
    public boolean isProcessing(String idempotencyKey) {
        String key = formatKey(idempotencyKey);
        String val = stringRedisTemplate.opsForValue().get(key);
        return STATUS_PROCESSING.equals(val);
    }

    @Override
    public Optional<String> getCachedResponse(String idempotencyKey) {
        String key = formatKey(idempotencyKey);
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val != null && !STATUS_PROCESSING.equals(val)) {
            return Optional.of(val);
        }
        return Optional.empty();
    }

    @Override
    public void complete(String idempotencyKey, String responseBodyJson, Duration cacheTtl) {
        String key = formatKey(idempotencyKey);
        stringRedisTemplate.opsForValue().set(key, responseBodyJson, cacheTtl);
        log.info("Idempotency key marked completed with cached response: {}", idempotencyKey);
    }

    @Override
    public void release(String idempotencyKey) {
        String key = formatKey(idempotencyKey);
        stringRedisTemplate.delete(key);
        log.warn("Idempotency lock released due to transaction rollback/failure: {}", idempotencyKey);
    }

    private String formatKey(String rawKey) {
        return KEY_PREFIX + rawKey.trim();
    }
}
