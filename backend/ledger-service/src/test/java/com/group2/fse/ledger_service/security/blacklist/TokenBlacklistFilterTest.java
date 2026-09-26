package com.group2.fse.ledger_service.security.blacklist;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class TokenBlacklistFilterTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private TokenBlacklistFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new TokenBlacklistFilter(tokenBlacklistService);
    }

    private String fakeToken(String jti) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"jti\":\"" + jti + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".fakesignature";
    }

    @Test
    void noAuthHeader_passesThroughChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void activeToken_passesThroughChain() throws Exception {
        String token = fakeToken("jti-active-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isRevoked("jti-active-1")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void revokedToken_shortCircuitsWith401() throws Exception {
        String token = fakeToken("jti-revoked-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/v1/ledger/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isRevoked("jti-revoked-1")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(401, response.getStatus());
        assertEquals(true, response.getContentAsString().contains("revoked"));
    }

    @Test
    void revokedToken_delegatesToCustomAuthenticationEntryPoint() throws Exception {
        com.group2.fse.ledger_service.security.handler.CustomAuthenticationEntryPoint mockEntryPoint =
                org.mockito.Mockito.mock(com.group2.fse.ledger_service.security.handler.CustomAuthenticationEntryPoint.class);
        TokenBlacklistFilter filterWithEntryPoint = new TokenBlacklistFilter(tokenBlacklistService, mockEntryPoint);

        String token = fakeToken("jti-revoked-2");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/v1/ledger/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isRevoked("jti-revoked-2")).thenReturn(true);

        filterWithEntryPoint.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(mockEntryPoint, times(1)).commence(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                org.mockito.ArgumentMatchers.any(TokenRevokedException.class)
        );
        assertEquals("AUTH_TOKEN_REVOKED", request.getAttribute(
                com.group2.fse.ledger_service.security.handler.CustomAuthenticationEntryPoint.ATTR_ERROR_CODE));
    }
}