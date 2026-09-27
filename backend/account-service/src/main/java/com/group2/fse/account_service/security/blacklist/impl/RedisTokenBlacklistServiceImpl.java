package com.group2.fse.account_service.security.blacklist.impl;

import com.group2.fse.account_service.security.blacklist.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception ex) {
            log.error("Failed to query Redis token blacklist for jti {}: {}", jti, ex.getMessage());
            return false;
        }
    }

    @Override
    public void blacklistToken(String jti, long ttlMillis) {
        if (jti == null || jti.isBlank() || ttlMillis <= 0) {
            return;
        }
        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            stringRedisTemplate.opsForValue().set(key, "revoked", ttlMillis, TimeUnit.MILLISECONDS);
            log.info("Blacklisted token jti: {} for {} ms", jti, ttlMillis);
        } catch (Exception ex) {
            log.error("Failed to write to Redis token blacklist for jti {}: {}", jti, ex.getMessage());
        }
    }
}
