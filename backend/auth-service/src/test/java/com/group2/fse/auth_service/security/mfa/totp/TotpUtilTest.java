package com.group2.fse.auth_service.security.mfa.totp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RFC 6238 TOTP Utility Tests")
class TotpUtilTest {

    @Test
    @DisplayName("Should generate valid Base32 secret")
    void shouldGenerateValidSecret() {
        String secret = TotpUtil.generateSecret();
        assertThat(secret).isNotBlank();
        assertThat(secret.length()).isGreaterThanOrEqualTo(26);
        assertThat(secret).matches("^[A-Z2-7]+$");
    }

    @Test
    @DisplayName("Should generate and immediately verify 6-digit TOTP code")
    void shouldGenerateAndVerifyCode() {
        String secret = TotpUtil.generateSecret();
        String code = TotpUtil.generateCurrentCode(secret);

        assertThat(code).hasSize(6);
        assertThat(code).matches("^\\d{6}$");

        boolean isValid = TotpUtil.verifyCode(secret, code);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid or incorrect TOTP code")
    void shouldRejectInvalidCode() {
        String secret = TotpUtil.generateSecret();
        String wrongCode = "000000";

        // Unless 000000 happens to be the exact current code, verify rejection
        String currentCode = TotpUtil.generateCurrentCode(secret);
        if (currentCode.equals(wrongCode)) {
            wrongCode = "999999";
        }

        assertThat(TotpUtil.verifyCode(secret, wrongCode)).isFalse();
        assertThat(TotpUtil.verifyCode(secret, "short")).isFalse();
        assertThat(TotpUtil.verifyCode(secret, null)).isFalse();
        assertThat(TotpUtil.verifyCode(null, currentCode)).isFalse();
    }

    @Test
    @DisplayName("Should format valid otpauth URI for QR codes")
    void shouldFormatOtpAuthUri() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = TotpUtil.getOtpAuthUri("CoreBank", "john_doe", secret);

        assertThat(uri).startsWith("otpauth://totp/CoreBank:john_doe?");
        assertThat(uri).contains("secret=JBSWY3DPEHPK3PXP");
        assertThat(uri).contains("issuer=CoreBank");
        assertThat(uri).contains("digits=6");
        assertThat(uri).contains("period=30");
    }
}
