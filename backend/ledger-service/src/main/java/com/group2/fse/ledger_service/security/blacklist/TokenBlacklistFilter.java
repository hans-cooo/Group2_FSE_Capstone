package com.group2.fse.ledger_service.security.blacklist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Task: FSE-404
 * Assigned to: Gabriel
 *
 * Runs AFTER JwtAuthenticationFilter (Jared, FSE-402) per Section 4A of the
 * Epic D plan. By the time it executes, the token's signature has already
 * been verified upstream, so this filter reads the jti claim directly off
 * the (already-trusted) token payload -- it does NOT depend on Jared's
 * UserPrincipal shape, keeping this package independently buildable.
 *
 * TODO once FSE-401 (spring-boot-starter-security) and FSE-405 (Alyssa's
 * CustomAuthenticationEntryPoint) merge: replace writeRevokedResponse() with
 * throwing TokenRevokedException and let Spring Security's exception
 * translation handle the 401 response centrally.
 */
@Slf4j
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Deliberately not using a JSON library here: Jackson isn't resolvable on
    // this module's classpath, and pulling in a dependency just to read one
    // claim off an already-signature-verified token is overkill.
    private static final Pattern JTI_PATTERN = Pattern.compile("\"jti\"\\s*:\\s*\"([^\"]+)\"");

    private final TokenBlacklistService tokenBlacklistService;

    public TokenBlacklistFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
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
            writeRevokedResponse(response, request.getRequestURI());
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
        response.setContentType("application/json");
        String body = String.format(
            "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Token has been revoked/logged out\",\"path\":\"%s\"}",
            path);
        response.getWriter().write(body);
    }
}