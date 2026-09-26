package com.group2.fse.auth_service.security.mfa;

import com.group2.fse.auth_service.security.mfa.totp.TotpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Multi-Channel (SMS, EMAIL, TOTP) & Console Fallback MFA Tests")
class MultiChannelMfaIntegrationTest {

    @Autowired
    private MfaChallengeService mfaChallengeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("SMS Channel: Generates 6-digit code, stores challenge, and verifies successfully")
    void shouldGenerateAndVerifySmsChallenge() {
        Long userId = 901L;
        String username = "sms_user";
        List<String> roles = List.of("ROLE_CUSTOMER");

        MfaChallenge challenge = mfaChallengeService.createChallenge(
                userId, username, roles, "CUSTOMER", "SMS", "+639171234567");

        assertThat(challenge.getMfaToken()).isNotBlank();
        assertThat(challenge.getChannel()).isEqualTo("SMS");
        assertThat(challenge.getCode()).matches("^\\d{6}$");
        assertThat(challenge.getDestination()).isEqualTo("+639171234567");

        // Verify with the valid code
        Optional<MfaChallenge> verified = mfaChallengeService.verifyChallenge(
                challenge.getMfaToken(), challenge.getCode());

        assertThat(verified).isPresent();
        assertThat(verified.get().getUsername()).isEqualTo(username);

        // Challenge must be consumed (single-use)
        Optional<MfaChallenge> secondAttempt = mfaChallengeService.verifyChallenge(
                challenge.getMfaToken(), challenge.getCode());
        assertThat(secondAttempt).isEmpty();
    }

    @Test
    @DisplayName("EMAIL Channel: Generates 6-digit code and verifies successfully")
    void shouldGenerateAndVerifyEmailChallenge() {
        Long userId = 902L;
        String username = "email_user";
        List<String> roles = List.of("ROLE_CUSTOMER");

        MfaChallenge challenge = mfaChallengeService.createChallenge(
                userId, username, roles, "CUSTOMER", "EMAIL", "user@corebank.ph");

        assertThat(challenge.getMfaToken()).isNotBlank();
        assertThat(challenge.getChannel()).isEqualTo("EMAIL");
        assertThat(challenge.getCode()).matches("^\\d{6}$");
        assertThat(challenge.getDestination()).isEqualTo("user@corebank.ph");

        // Verify rejection with wrong code
        Optional<MfaChallenge> wrongAttempt = mfaChallengeService.verifyChallenge(
                challenge.getMfaToken(), "000000");
        assertThat(wrongAttempt).isEmpty();

        // Verify with the real code
        Optional<MfaChallenge> verified = mfaChallengeService.verifyChallenge(
                challenge.getMfaToken(), challenge.getCode());
        assertThat(verified).isPresent();
    }

    @Test
    @DisplayName("TOTP Channel: Generates rolling code, stores Base32 secret, and verifies via Authenticator algorithm")
    void shouldGenerateAndVerifyTotpChallenge() {
        Long userId = 903L;
        String username = "totp_user";
        List<String> roles = List.of("ROLE_CUSTOMER");

        MfaChallenge challenge = mfaChallengeService.createChallenge(
                userId, username, roles, "CUSTOMER", "TOTP", "Google Authenticator");

        assertThat(challenge.getMfaToken()).isNotBlank();
        assertThat(challenge.getChannel()).isEqualTo("TOTP");
        assertThat(challenge.getTotpSecret()).isNotBlank();
        assertThat(challenge.getCode()).matches("^\\d{6}$");

        // Verify secret persisted in Redis for future logins
        String savedSecret = stringRedisTemplate.opsForValue().get(MfaChallengeService.TOTP_SECRET_PREFIX + userId);
        assertThat(savedSecret).isEqualTo(challenge.getTotpSecret());

        // Simulate Authenticator App calculating current code from the secret
        String appCalculatedCode = TotpUtil.generateCurrentCode(challenge.getTotpSecret());

        Optional<MfaChallenge> verified = mfaChallengeService.verifyChallenge(
                challenge.getMfaToken(), appCalculatedCode);

        assertThat(verified).isPresent();
        assertThat(verified.get().getUsername()).isEqualTo(username);
    }
}
