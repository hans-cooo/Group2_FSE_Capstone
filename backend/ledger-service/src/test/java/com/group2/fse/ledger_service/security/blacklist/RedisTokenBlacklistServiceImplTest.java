package com.group2.fse.ledger_service.security.blacklist;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.group2.fse.ledger_service.security.blacklist.impl.RedisTokenBlacklistServiceImpl;

class RedisTokenBlacklistServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenBlacklistServiceImpl blacklistService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        blacklistService = new RedisTokenBlacklistServiceImpl(stringRedisTemplate);
    }

    @Test
    void revokeToken_writesKeyWithGivenTtl() {
        blacklistService.revokeToken("abc-123", Duration.ofMinutes(5));
        verify(valueOperations).set(eq("blacklist:jti:abc-123"), eq("revoked"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void revokeToken_flooredTtlWhenNullOrTooSmall() {
        blacklistService.revokeToken("abc-123", Duration.ofMillis(10));
        verify(valueOperations).set(eq("blacklist:jti:abc-123"), eq("revoked"), eq(Duration.ofSeconds(1)));
    }

    @Test
    void revokeToken_blankJti_throws() {
        assertThrows(IllegalArgumentException.class, () -> blacklistService.revokeToken(" ", Duration.ofMinutes(5)));
    }

    @Test
    void isRevoked_keyExists_true() {
        when(stringRedisTemplate.hasKey("blacklist:jti:abc-123")).thenReturn(true);
        assertTrue(blacklistService.isRevoked("abc-123"));
    }

    @Test
    void isRevoked_keyMissing_false() {
        when(stringRedisTemplate.hasKey("blacklist:jti:xyz")).thenReturn(false);
        assertFalse(blacklistService.isRevoked("xyz"));
    }

    @Test
    void isRevoked_nullJti_falseWithoutRedisCall() {
        assertFalse(blacklistService.isRevoked(null));
    }
}