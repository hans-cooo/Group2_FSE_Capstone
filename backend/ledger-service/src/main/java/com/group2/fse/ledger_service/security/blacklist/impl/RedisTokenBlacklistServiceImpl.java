package com.group2.fse.ledger_service.security.blacklist.impl;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.group2.fse.ledger_service.security.blacklist.TokenBlacklistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Task: FSE-404
 * Assigned to: Gabriel
 * Reuses the StringRedisTemplate bean Carl already configured in
 * RedisConfig.java (FSE-301) -- no new Redis wiring needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist:jti:";
    private static final Duration MIN_TTL = Duration.ofSeconds(1);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void revokeToken(String jti, Duration remainingTtl) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("jti must not be blank");
        }
        // Floor the TTL so an already-expired/near-expired token still gets
        // a key written (defensive -- Redis SET with TTL <= 0 would no-op).
        Duration ttl = (remainingTtl == null || remainingTtl.compareTo(MIN_TTL) < 0)
            ? MIN_TTL
            : remainingTtl;

        stringRedisTemplate.opsForValue().set(KEY_PREFIX + jti, "revoked", ttl);
        log.info("Token revoked and blacklisted: jti={}, ttl={}", jti, ttl);
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + jti));
    }
}