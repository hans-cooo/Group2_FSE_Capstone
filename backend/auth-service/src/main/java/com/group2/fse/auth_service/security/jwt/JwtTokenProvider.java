package com.group2.fse.auth_service.security.jwt;

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
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.security.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    @Value("${app.security.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed access token with sub, userId, roles, userType, and unique jti.
     */
    public String generateAccessToken(String username, Long userId, List<String> roles, String userType) {
        String jti = UUID.randomUUID().toString();
        return generateToken(username, userId, roles, userType, jti, jwtExpirationMs);
    }

    /**
     * Generates a token with an explicit jti and custom expiration duration.
     */
    public String generateToken(String username, Long userId, List<String> roles, String userType, String jti, long expirationMs) {
        Date now = new Date();
        long duration = expirationMs > 0 ? expirationMs : jwtExpirationMs;
        Date expiryDate = new Date(now.getTime() + duration);

        return Jwts.builder()
                .subject(username)
                .id(jti)
                .claim("userId", userId)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .claim("userType", userType != null ? userType : "CUSTOMER")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Parses and extracts claims from a signed JWT token.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates JWT signature, format, and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object val = getClaims(token).get("userId");
        if (val instanceof Number num) {
            return num.longValue();
        } else if (val != null) {
            return Long.parseLong(val.toString());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object val = getClaims(token).get("roles");
        if (val instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    public String getUserType(String token) {
        Object val = getClaims(token).get("userType");
        return val != null ? val.toString() : "CUSTOMER";
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    public Date getExpirationDate(String token) {
        return getClaims(token).getExpiration();
    }

    /**
     * Returns the remaining time-to-live for a token, or Duration.ZERO if already expired.
     */
    public Duration getRemainingTtl(String token) {
        try {
            Date exp = getExpirationDate(token);
            long diff = exp.getTime() - System.currentTimeMillis();
            return diff > 0 ? Duration.ofMillis(diff) : Duration.ZERO;
        } catch (Exception ex) {
            return Duration.ZERO;
        }
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}
