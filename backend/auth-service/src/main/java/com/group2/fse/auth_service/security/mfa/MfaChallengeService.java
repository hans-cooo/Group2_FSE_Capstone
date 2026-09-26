package com.group2.fse.auth_service.security.mfa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaChallengeService {

    public static final String KEY_PREFIX = "mfa:challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Generates a 6-digit OTP challenge and stores it in Redis with 5-minute TTL.
     */
    public MfaChallenge createChallenge(Long userId, String username, List<String> roles, String userType) {
        String mfaToken = UUID.randomUUID().toString();
        // Generate secure 6-digit code: 100000 to 999999
        int randomCode = 100000 + SECURE_RANDOM.nextInt(900000);
        String code = String.valueOf(randomCode);

        MfaChallenge challenge = MfaChallenge.builder()
                .mfaToken(mfaToken)
                .userId(userId)
                .username(username)
                .roles(roles)
                .userType(userType)
                .code(code)
                .createdAt(Instant.now())
                .build();

        try {
            String json = objectMapper.writeValueAsString(challenge);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + mfaToken, json, CHALLENGE_TTL);
            log.info("Created MFA challenge for user {}: token={}, code={}", username, mfaToken, code);
            return challenge;
        } catch (Exception e) {
            log.error("Failed to store MFA challenge in Redis: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to store MFA challenge in Redis", e);
        }
    }

    /**
     * Verifies the submitted OTP against the challenge stored in Redis.
     */
    public Optional<MfaChallenge> verifyChallenge(String mfaToken, String code) {
        if (mfaToken == null || mfaToken.isBlank() || code == null || code.isBlank()) {
            return Optional.empty();
        }
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + mfaToken);
        if (json == null) {
            log.warn("MFA challenge token not found or expired: {}", mfaToken);
            return Optional.empty();
        }
        try {
            MfaChallenge challenge = objectMapper.readValue(json, MfaChallenge.class);
            if (challenge.getCode().equals(code.trim())) {
                // Consume challenge so it cannot be re-used
                consumeChallenge(mfaToken);
                return Optional.of(challenge);
            } else {
                log.warn("MFA code mismatch for user {}: provided={}, expected={}", 
                        challenge.getUsername(), code, challenge.getCode());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to parse MFA challenge from Redis: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Consumes and deletes the challenge from Redis.
     */
    public void consumeChallenge(String mfaToken) {
        if (mfaToken != null && !mfaToken.isBlank()) {
            stringRedisTemplate.delete(KEY_PREFIX + mfaToken);
            log.info("Consumed MFA challenge token: {}", mfaToken);
        }
    }
}
