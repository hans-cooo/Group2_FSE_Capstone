package com.group2.fse.auth_service.security.blacklist.impl;

import com.group2.fse.auth_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.auth_service.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistServiceImpl implements TokenBlacklistService {

    public static final String KEY_PREFIX = "blacklist:jti:";
    private static final Duration MIN_TTL = Duration.ofSeconds(1);

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void revokeToken(String jti, Duration remainingTtl) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("jti must not be blank");
        }
        Duration ttl = (remainingTtl == null || remainingTtl.compareTo(MIN_TTL) < 0)
                ? MIN_TTL
                : remainingTtl;

        stringRedisTemplate.opsForValue().set(KEY_PREFIX + jti, "revoked", ttl);
        log.info("Token revoked and blacklisted: jti={}, ttl={}", jti, ttl);
    }

    @Override
    public void revokeTokenByJwt(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("JWT token must not be blank");
        }
        if (jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }
        String jti = jwtTokenProvider.getJti(jwt);
        Duration ttl = jwtTokenProvider.getRemainingTtl(jwt);
        revokeToken(jti, ttl);
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
