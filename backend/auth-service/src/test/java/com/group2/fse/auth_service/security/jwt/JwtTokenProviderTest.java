package com.group2.fse.auth_service.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Phase 3: JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Should generate valid JWT access token with all required claims")
    void shouldGenerateValidAccessToken() {
        String username = "john_doe";
        Long userId = 101L;
        List<String> roles = List.of("ROLE_CUSTOMER");
        String userType = "CUSTOMER";

        String token = jwtTokenProvider.generateAccessToken(username, userId, roles, userType);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo(username);
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getRoles(token)).containsExactly("ROLE_CUSTOMER");
        assertThat(jwtTokenProvider.getUserType(token)).isEqualTo("CUSTOMER");
        assertThat(jwtTokenProvider.getJti(token)).isNotBlank();

        Duration remainingTtl = jwtTokenProvider.getRemainingTtl(token);
        assertThat(remainingTtl.getSeconds()).isPositive();
    }

    @Test
    @DisplayName("Should generate valid staff JWT token with multiple roles")
    void shouldGenerateStaffTokenWithMultipleRoles() {
        String username = "admin";
        Long userId = 1L;
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_TELLER");
        String userType = "STAFF";

        String token = jwtTokenProvider.generateAccessToken(username, userId, roles, userType);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo(username);
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getRoles(token)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_TELLER");
        assertThat(jwtTokenProvider.getUserType(token)).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("Should reject tampered JWT token")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken("john_doe", 101L, List.of("ROLE_CUSTOMER"), "CUSTOMER");
        String tamperedToken = token + "corrupted";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void shouldRejectExpiredToken() throws InterruptedException {
        // Generate a token with 10ms expiration
        String shortLivedToken = jwtTokenProvider.generateToken(
                "john_doe", 101L, List.of("ROLE_CUSTOMER"), "CUSTOMER", "test-jti-1", 10L);

        // Wait for token to expire
        Thread.sleep(50L);

        assertThat(jwtTokenProvider.validateToken(shortLivedToken)).isFalse();
    }

    @Test
    @DisplayName("Should extract raw Claims object from signed token")
    void shouldExtractRawClaims() {
        String token = jwtTokenProvider.generateAccessToken("maria_santos", 102L, List.of("ROLE_CUSTOMER"), "CUSTOMER");
        Claims claims = jwtTokenProvider.getClaims(token);

        assertThat(claims.getSubject()).isEqualTo("maria_santos");
        assertThat(claims.get("userId", Long.class)).isEqualTo(102L);
        assertThat(claims.get("userType", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isNotNull();
    }
}
