package com.group2.fse.auth_service.security.session;

import com.group2.fse.auth_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.auth_service.security.blacklist.impl.RedisTokenBlacklistServiceImpl;
import com.group2.fse.auth_service.security.jwt.JwtTokenProvider;
import com.group2.fse.auth_service.security.mfa.MfaChallenge;
import com.group2.fse.auth_service.security.mfa.MfaChallengeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Phase 3: Redis Session State, Blacklist & MFA Challenge Tests")
class RedisSessionStateTest {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisRefreshTokenService redisRefreshTokenService;

    @Autowired
    private MfaChallengeService mfaChallengeService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Should blacklist token by jti and verify key in Redis")
    void shouldBlacklistTokenByJti() {
        String testJti = "jti-test-" + System.currentTimeMillis();

        assertThat(tokenBlacklistService.isRevoked(testJti)).isFalse();

        tokenBlacklistService.revokeToken(testJti, Duration.ofSeconds(30));

        assertThat(tokenBlacklistService.isRevoked(testJti)).isTrue();
        assertThat(stringRedisTemplate.hasKey(RedisTokenBlacklistServiceImpl.KEY_PREFIX + testJti)).isTrue();
    }

    @Test
    @DisplayName("Should blacklist token directly by passing signed JWT")
    void shouldBlacklistTokenByJwt() {
        String token = jwtTokenProvider.generateAccessToken("test_user", 999L, List.of("ROLE_CUSTOMER"), "CUSTOMER");
        String jti = jwtTokenProvider.getJti(token);

        assertThat(tokenBlacklistService.isRevoked(jti)).isFalse();

        tokenBlacklistService.revokeTokenByJwt("Bearer " + token);

        assertThat(tokenBlacklistService.isRevoked(jti)).isTrue();
    }

    @Test
    @DisplayName("Should create, retrieve, and revoke refresh token in Redis")
    void shouldManageRefreshTokenLifecycle() {
        Long userId = 201L;
        String username = "refresh_user";
        List<String> roles = List.of("ROLE_CUSTOMER");

        String refreshToken = redisRefreshTokenService.createRefreshToken(userId, username, roles, "CUSTOMER");
        assertThat(refreshToken).isNotBlank();

        Optional<RefreshTokenData> dataOpt = redisRefreshTokenService.getRefreshTokenData(refreshToken);
        assertThat(dataOpt).isPresent();
        assertThat(dataOpt.get().getUserId()).isEqualTo(userId);
        assertThat(dataOpt.get().getUsername()).isEqualTo(username);
        assertThat(dataOpt.get().getRoles()).containsExactly("ROLE_CUSTOMER");

        redisRefreshTokenService.revokeRefreshToken(refreshToken);
        Optional<RefreshTokenData> revokedOpt = redisRefreshTokenService.getRefreshTokenData(refreshToken);
        assertThat(revokedOpt).isEmpty();
    }

    @Test
    @DisplayName("Should create, verify, and consume MFA OTP challenge in Redis")
    void shouldManageMfaChallengeLifecycle() {
        Long userId = 301L;
        String username = "mfa_user";
        List<String> roles = List.of("ROLE_CUSTOMER");

        MfaChallenge challenge = mfaChallengeService.createChallenge(userId, username, roles, "CUSTOMER");
        assertThat(challenge.getMfaToken()).isNotBlank();
        assertThat(challenge.getCode()).hasSize(6);

        // Verify with invalid code should fail
        Optional<MfaChallenge> invalidVerify = mfaChallengeService.verifyChallenge(challenge.getMfaToken(), "000000");
        assertThat(invalidVerify).isEmpty();

        // Verify with correct code should succeed
        Optional<MfaChallenge> validVerify = mfaChallengeService.verifyChallenge(challenge.getMfaToken(), challenge.getCode());
        assertThat(validVerify).isPresent();
        assertThat(validVerify.get().getUsername()).isEqualTo(username);

        // Second verification should fail because challenge was consumed
        Optional<MfaChallenge> secondVerify = mfaChallengeService.verifyChallenge(challenge.getMfaToken(), challenge.getCode());
        assertThat(secondVerify).isEmpty();
    }
}
