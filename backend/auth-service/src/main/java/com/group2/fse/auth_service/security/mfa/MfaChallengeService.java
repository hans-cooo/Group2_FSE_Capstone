package com.group2.fse.auth_service.security.mfa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.auth_service.security.mfa.delivery.OtpDeliveryService;
import com.group2.fse.auth_service.security.mfa.totp.TotpUtil;
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
    public static final String TOTP_SECRET_PREFIX = "mfa:totp:secret:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final Duration TOTP_SECRET_TTL = Duration.ofDays(365);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final OtpDeliveryService otpDeliveryService;

    /**
     * Backward-compatible challenge creation defaulting to SMS.
     */
    public MfaChallenge createChallenge(Long userId, String username, List<String> roles, String userType) {
        return createChallenge(userId, username, roles, userType, "SMS", "Mobile Device");
    }

    /**
     * Generates a multi-channel OTP/TOTP challenge, stores it in Redis,
     * and dispatches the code via OtpDeliveryService (with console fallback).
     */
    public MfaChallenge createChallenge(Long userId, String username, List<String> roles, String userType,
                                        String channel, String destination) {
        String mfaToken = UUID.randomUUID().toString();
        String normalizedChannel = (channel != null && !channel.isBlank()) ? channel.trim().toUpperCase() : "SMS";

        String code;
        String totpSecret = null;
        String totpUri = null;

        if ("TOTP".equals(normalizedChannel)) {
            // Retrieve or generate user's persistent TOTP secret
            String secretKey = TOTP_SECRET_PREFIX + userId;
            totpSecret = stringRedisTemplate.opsForValue().get(secretKey);
            if (totpSecret == null || totpSecret.isBlank()) {
                totpSecret = TotpUtil.generateSecret();
                stringRedisTemplate.opsForValue().set(secretKey, totpSecret, TOTP_SECRET_TTL);
            }
            code = TotpUtil.generateCurrentCode(totpSecret);
            totpUri = TotpUtil.getOtpAuthUri("CoreBank", username, totpSecret);
        } else {
            // Random 6-digit code for SMS and EMAIL
            int randomCode = 100000 + SECURE_RANDOM.nextInt(900000);
            code = String.valueOf(randomCode);
        }

        MfaChallenge challenge = MfaChallenge.builder()
                .mfaToken(mfaToken)
                .userId(userId)
                .username(username)
                .roles(roles)
                .userType(userType)
                .code(code)
                .channel(normalizedChannel)
                .destination(destination)
                .totpSecret(totpSecret)
                .createdAt(Instant.now())
                .build();

        try {
            String json = objectMapper.writeValueAsString(challenge);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + mfaToken, json, CHALLENGE_TTL);
            log.info("Created MFA challenge: user={}, token={}, channel={}", username, mfaToken, normalizedChannel);

            // Dispatch to delivery service (prints to console log + external provider attempt)
            otpDeliveryService.deliverOtp(username, destination, normalizedChannel, code, totpUri);

            return challenge;
        } catch (Exception e) {
            log.error("Failed to store MFA challenge in Redis: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to store MFA challenge in Redis", e);
        }
    }

    /**
     * Verifies the submitted OTP or TOTP code against the challenge.
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
            String inputCode = code.trim();

            boolean matched = false;
            if ("TOTP".equalsIgnoreCase(challenge.getChannel())) {
                // Check if matches the code generated at challenge time OR the current rolling TOTP window
                matched = challenge.getCode().equals(inputCode)
                        || (challenge.getTotpSecret() != null && TotpUtil.verifyCode(challenge.getTotpSecret(), inputCode));
            } else {
                matched = challenge.getCode().equals(inputCode);
            }

            if (matched) {
                // Consume challenge so it cannot be re-used
                consumeChallenge(mfaToken);
                return Optional.of(challenge);
            } else {
                log.warn("MFA code mismatch for user {}: provided={}, expected={}",
                        challenge.getUsername(), inputCode, challenge.getCode());
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
