package com.group2.fse.account_service.security;

import com.group2.fse.account_service.security.blacklist.TokenBlacklistFilter;
import com.group2.fse.account_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.account_service.security.blacklist.TokenRevokedException;
import com.group2.fse.account_service.security.handler.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Distributed Token Revocation Blacklist Filter Tests")
class TokenBlacklistFilterTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private TokenBlacklistFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TokenBlacklistFilter(tokenBlacklistService, authenticationEntryPoint);
    }

    private String buildTokenWithJti(String jti) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"user\",\"jti\":\"" + jti + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".dummySig";
    }

    @Test
    @DisplayName("[BLK-001] Should allow request when token jti is NOT blacklisted in Redis")
    void shouldAllowNonBlacklistedToken() throws ServletException, IOException {
        String token = buildTokenWithJti("active-uuid-123");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted("active-uuid-123")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authenticationEntryPoint);
    }

    @Test
    @DisplayName("[BLK-002] Should intercept and reject request when token jti is revoked in Redis blacklist")
    void shouldRejectBlacklistedToken() throws ServletException, IOException {
        String token = buildTokenWithJti("revoked-uuid-999");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted("revoked-uuid-999")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(request).setAttribute(eq("SECURITY_ERROR_CODE"), eq("AUTH_TOKEN_REVOKED"));
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(TokenRevokedException.class));
        verify(filterChain, never()).doFilter(request, response);
    }
}
