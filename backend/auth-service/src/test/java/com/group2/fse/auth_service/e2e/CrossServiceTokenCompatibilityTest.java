package com.group2.fse.auth_service.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.auth_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.auth_service.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Phase 6: Cross-Service E2E JWT & Redis Contract Verification")
class CrossServiceTokenCompatibilityTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should issue JWT conforming to Ledger-Service claims contract")
    void shouldConformToLedgerServiceJwtClaimsContract() throws Exception {
        String username = "customer_juan";
        Long userId = 101L;
        List<String> roles = List.of("ROLE_CUSTOMER");
        String userType = "CUSTOMER";

        String token = jwtTokenProvider.generateAccessToken(username, userId, roles, userType);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        // 1. Verify claims parsed by JJWT
        Claims claims = jwtTokenProvider.getClaims(token);
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.get("userId", Long.class)).isEqualTo(userId);
        assertThat(claims.get("userType", String.class)).isEqualTo(userType);
        @SuppressWarnings("unchecked")
        List<String> extractedRoles = (List<String>) claims.get("roles", List.class);
        assertThat(extractedRoles).containsExactly("ROLE_CUSTOMER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());

        // 2. Verify raw JSON payload matches Ledger-Service RegEx & JSON parsing contract
        String[] parts = token.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode jsonNode = objectMapper.readTree(payloadJson);

        assertThat(jsonNode.hasNonNull("sub")).isTrue();
        assertThat(jsonNode.get("sub").asText()).isEqualTo(username);
        assertThat(jsonNode.hasNonNull("jti")).isTrue();
        assertThat(jsonNode.get("jti").asText()).isEqualTo(claims.getId());
        assertThat(jsonNode.hasNonNull("userId")).isTrue();
        assertThat(jsonNode.get("userId").asLong()).isEqualTo(userId);
        assertThat(jsonNode.hasNonNull("roles")).isTrue();
        assertThat(jsonNode.get("roles").isArray()).isTrue();
        assertThat(jsonNode.hasNonNull("userType")).isTrue();
        assertThat(jsonNode.get("userType").asText()).isEqualTo(userType);
        assertThat(jsonNode.hasNonNull("iat")).isTrue();
        assertThat(jsonNode.hasNonNull("exp")).isTrue();
    }

    @Test
    @DisplayName("Should publish revoked token to Redis with key format expected by Ledger-Service TokenBlacklistFilter")
    void shouldPublishBlacklistMatchingLedgerServiceKeyFormat() {
        String jti = "e2e-revoked-jti-" + System.currentTimeMillis();
        Duration ttl = Duration.ofMinutes(10);

        tokenBlacklistService.revokeToken(jti, ttl);

        // Verify using auth-service service
        assertThat(tokenBlacklistService.isRevoked(jti)).isTrue();

        // Verify the exact Redis key format 'blacklist:jti:{jti}' expected by ledger-service
        String expectedRedisKey = "blacklist:jti:" + jti;
        String storedValue = stringRedisTemplate.opsForValue().get(expectedRedisKey);

        assertThat(storedValue).isEqualTo("revoked");
        Long remainingExpire = stringRedisTemplate.getExpire(expectedRedisKey);
        assertThat(remainingExpire).isPositive().isLessThanOrEqualTo(600L);

        // Cleanup
        stringRedisTemplate.delete(expectedRedisKey);
    }
}
