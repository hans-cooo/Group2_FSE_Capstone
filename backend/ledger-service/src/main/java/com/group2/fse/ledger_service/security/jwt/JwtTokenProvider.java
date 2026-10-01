package com.group2.fse.ledger_service.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * JWT Token Provider for signing, parsing, and validating JWT tokens in ledger-service.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            // Attempt to decode as Base64/Hex
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            // Pad to 256 bits if secret was shorter
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate token with subject, userId, roles, and jti.
     */
    public String generateToken(String username, Long userId, List<String> roles, String jti, long expirationMs) {
        Date now = new Date();
        long duration = expirationMs != 0 ? expirationMs : jwtExpirationMs;
        Date expiryDate = new Date(now.getTime() + duration);

        return Jwts.builder()
                .subject(username)
                .id(jti)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extract claims payload from signed token.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public enum JwtValidationStatus {
        VALID,
        EXPIRED,
        INVALID
    }

    /**
     * Validate JWT token signature and expiration, returning detailed validation status.
     */
    public JwtValidationStatus validateTokenDetailed(String token) {
        if (token == null || token.isBlank()) {
            return JwtValidationStatus.INVALID;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return JwtValidationStatus.VALID;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            return JwtValidationStatus.EXPIRED;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token: {}", e.getMessage());
            return JwtValidationStatus.INVALID;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return JwtValidationStatus.INVALID;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid: {}", e.getMessage());
            return JwtValidationStatus.INVALID;
        }
    }

    /**
     * Check specifically if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        return validateTokenDetailed(token) == JwtValidationStatus.EXPIRED;
    }

    /**
     * Validate JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        return validateTokenDetailed(token) == JwtValidationStatus.VALID;
    }


    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        } else if (userIdObj instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = getClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection<?> collection) {
            List<String> roles = new ArrayList<>();
            for (Object item : collection) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            return roles;
        } else if (rolesObj instanceof String rolesStr) {
            return List.of(rolesStr.split(","));
        }
        return List.of();
    }

    /**
     * Build Spring Security Authentication token from JWT.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String username = claims.getSubject();
        Long userId = getUserId(token);
        List<String> roles = getRoles(token);

        UserPrincipal principal = UserPrincipal.create(userId, username, roles);
        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }
}
