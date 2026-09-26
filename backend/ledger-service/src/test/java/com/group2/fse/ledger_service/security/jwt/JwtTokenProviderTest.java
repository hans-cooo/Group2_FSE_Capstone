package com.group2.fse.ledger_service.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // 256-bit test secret (Base64)
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid token and extract claims correctly")
    void shouldGenerateAndExtractClaims() {
        String token = jwtTokenProvider.generateToken("teller_jane", 1001L, List.of("ROLE_TELLER", "ROLE_CUSTOMER"), "jti-uuid-1234", 3600000L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("teller_jane");
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1001L);
        assertThat(jwtTokenProvider.getJti(token)).isEqualTo("jti-uuid-1234");
        assertThat(jwtTokenProvider.getRoles(token)).containsExactlyInAnyOrder("ROLE_TELLER", "ROLE_CUSTOMER");

        Authentication auth = jwtTokenProvider.getAuthentication(token);
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("teller_jane");
        assertThat(principal.getUserId()).isEqualTo(1001L);
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_TELLER", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should fail validation on expired token")
    void shouldFailValidationOnExpiredToken() {
        // Expiration in the past: -1000ms
        String expiredToken = jwtTokenProvider.generateToken("teller_jane", 1001L, List.of("ROLE_TELLER"), "jti-expired", -1000L);

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should fail validation on tampered token")
    void shouldFailValidationOnTamperedToken() {
        String token = jwtTokenProvider.generateToken("teller_jane", 1001L, List.of("ROLE_TELLER"), "jti-tampered", 3600000L);
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("Should fail validation on malformed token")
    void shouldFailValidationOnMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not-a-valid-token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
