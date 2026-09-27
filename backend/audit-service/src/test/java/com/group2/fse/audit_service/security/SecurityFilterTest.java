package com.group2.fse.audit_service.security;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.audit_service.security.filter.JwtAuthenticationFilter;
import com.group2.fse.audit_service.security.handler.CustomAccessDeniedHandler;
import com.group2.fse.audit_service.security.handler.CustomAuthenticationEntryPoint;
import com.group2.fse.audit_service.security.jwt.JwtTokenProvider;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Service Security Components & RFC-7807 Handlers Tests")
class SecurityFilterTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 3600000;

    private JwtTokenProvider jwtTokenProvider;
    private CustomAuthenticationEntryPoint entryPoint;
    private CustomAccessDeniedHandler accessDeniedHandler;
    private JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", EXPIRATION);
        jwtTokenProvider.init();

        entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
        jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider);

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JwtTokenProvider: Generates and parses valid token with roles")
    void testTokenGenerationAndParsing() {
        String token = jwtTokenProvider.generateToken(1L, "auditor_user", List.of("ROLE_AUDITOR"));
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals("auditor_user", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(1L, jwtTokenProvider.getUserIdFromToken(token));

        Authentication auth = jwtTokenProvider.getAuthentication(token);
        assertNotNull(auth);
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_AUDITOR", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("CustomAuthenticationEntryPoint: Emits RFC-7807 401 response")
    void testAuthenticationEntryPointRfc7807() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/audit/verify-chain/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertEquals(401, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("AUTH_INVALID_CREDENTIALS"));
        assertTrue(body.contains("Unauthorized"));
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler: Emits RFC-7807 403 response")
    void testAccessDeniedHandlerRfc7807() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/audit/verify-chain/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access is denied"));

        assertEquals(403, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("AUTH_ACCESS_DENIED"));
        assertTrue(body.contains("Forbidden"));
    }

    @Test
    @DisplayName("JwtAuthenticationFilter: Sets SecurityContext when Bearer token present")
    void testJwtAuthenticationFilter_SetsAuthentication() throws ServletException, IOException {
        String token = jwtTokenProvider.generateToken(2L, "admin_user", List.of("ROLE_ADMIN"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtFilter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("admin_user", auth.getName());
    }
}
