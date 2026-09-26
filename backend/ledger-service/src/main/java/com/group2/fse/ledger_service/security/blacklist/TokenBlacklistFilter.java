package com.group2.fse.ledger_service.security.blacklist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.group2.fse.ledger_service.security.handler.CustomAuthenticationEntryPoint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Task: FSE-404 & FSE-405
 * Assigned to: Gabriel & Alyssa
 *
 * Runs AFTER JwtAuthenticationFilter per Section 4A of the Epic D plan.
 * Verifies token revocation state in Redis.
 * If revoked, delegates to Alyssa's CustomAuthenticationEntryPoint (FSE-405)
 * throwing TokenRevokedException for standardized RFC-7807 error responses.
 */
@Slf4j
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Deliberately not using a JSON library here: avoids extra parsing dependencies
    // for reading one claim off an already-signature-verified token.
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

        String jti = extractJti(header.substring(BEARER_PREFIX.length()));
        if (jti != null && tokenBlacklistService.isRevoked(jti)) {
            log.warn("Rejected request with revoked token: jti={}, path={}", jti, request.getRequestURI());
            SecurityContextHolder.clearContext();
            request.setAttribute(CustomAuthenticationEntryPoint.ATTR_ERROR_CODE, "AUTH_TOKEN_REVOKED");
            request.setAttribute(CustomAuthenticationEntryPoint.ATTR_ERROR_DETAIL,
                    "The token has been revoked or logged out. Please authenticate with new credentials.");

            if (authenticationEntryPoint != null) {
                authenticationEntryPoint.commence(request, response,
                        new TokenRevokedException("Token has been revoked/logged out"));
            } else {
                writeRevokedResponse(response, request.getRequestURI());
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
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            Matcher matcher = JTI_PATTERN.matcher(payloadJson);
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception malformedToken) {
            log.debug("Could not extract jti from token; skipping blacklist check.", malformedToken);
            return null;
        }
    }

    private void writeRevokedResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        String body = String.format(
            "{\"type\":\"https://api.corebank.local/errors/AUTH_TOKEN_REVOKED\"," +
            "\"title\":\"Token Revoked\"," +
            "\"status\":401," +
            "\"detail\":\"The token has been revoked or logged out. Please authenticate with new credentials.\"," +
            "\"instance\":\"%s\"," +
            "\"errorCode\":\"AUTH_TOKEN_REVOKED\"," +
            "\"timestamp\":\"%s\"}",
            path,
            java.time.Instant.now().toString());
        response.getWriter().write(body);
    }
}