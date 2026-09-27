package com.group2.fse.account_service.security.blacklist;

import com.group2.fse.account_service.security.handler.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that runs after JwtAuthenticationFilter.
 * Verifies if the token's jti is in the Redis blacklist.
 * If revoked, delegates to CustomAuthenticationEntryPoint throwing TokenRevokedException.
 */
@Slf4j
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern JTI_PATTERN = Pattern.compile("\"jti\"\\s*:\\s*\"([^\"]+)\"");

    private final TokenBlacklistService tokenBlacklistService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    public TokenBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this(tokenBlacklistService, null);
    }

    public TokenBlacklistFilter(TokenBlacklistService tokenBlacklistService,
                                CustomAuthenticationEntryPoint authenticationEntryPoint) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        String jti = extractJti(token);

        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            log.warn("Revoked token detected (jti: {}). Access denied to URI: {}", jti, request.getRequestURI());
            SecurityContextHolder.clearContext();

            request.setAttribute(CustomAuthenticationEntryPoint.ATTR_ERROR_CODE, "AUTH_TOKEN_REVOKED");
            request.setAttribute(CustomAuthenticationEntryPoint.ATTR_ERROR_DETAIL, "Token has been revoked or logged out. Please log in again.");

            if (authenticationEntryPoint != null) {
                authenticationEntryPoint.commence(request, response,
                        new TokenRevokedException("Access token has been revoked in Redis blacklist: " + jti));
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractJti(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            Matcher matcher = JTI_PATTERN.matcher(payload);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("Could not extract jti from token: {}", e.getMessage());
        }
        return null;
    }
}
