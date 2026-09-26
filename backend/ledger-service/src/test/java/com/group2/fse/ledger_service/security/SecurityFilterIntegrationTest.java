package com.group2.fse.ledger_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group2.fse.ledger_service.security.blacklist.TokenBlacklistFilter;
import com.group2.fse.ledger_service.security.blacklist.TokenBlacklistService;
import com.group2.fse.ledger_service.security.filter.JwtAuthenticationFilter;
import com.group2.fse.ledger_service.security.handler.CustomAuthenticationEntryPoint;
import com.group2.fse.ledger_service.security.jwt.JwtTokenProvider;
import com.group2.fse.ledger_service.security.jwt.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Perimeter Filter Pipeline Integration Tests")
class SecurityFilterIntegrationTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtFilter;
    private TokenBlacklistFilter blacklistFilter;
    private CustomAuthenticationEntryPoint entryPoint;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        blacklistFilter = new TokenBlacklistFilter(tokenBlacklistService, entryPoint);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String createBearerToken(String jti) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"teller_jane\",\"userId\":1001,\"jti\":\"" + jti + "\"}").getBytes(StandardCharsets.UTF_8));
        return "Bearer " + header + "." + payload + ".signature";
    }

    @Test
    @DisplayName("Should successfully authenticate request and proceed through filter pipeline when token is valid and active")
    void shouldAuthenticateValidTokenAcrossPipeline() throws Exception {
        String tokenHeader = createBearerToken("jti-active-100");
        String rawToken = tokenHeader.substring(7);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ledger/transfers");
        request.addHeader("Authorization", tokenHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(rawToken)).thenReturn(true);
        UserPrincipal principal = UserPrincipal.create(1001L, "teller_jane", List.of("ROLE_TELLER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, rawToken, principal.getAuthorities());
        when(jwtTokenProvider.getAuthentication(rawToken)).thenReturn(auth);
        when(tokenBlacklistService.isRevoked("jti-active-100")).thenReturn(false);

        // Step 1: JWT Filter
        jwtFilter.doFilter(request, response, (req, res) -> {
            // Verify SecurityContext was populated
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("teller_jane");

            // Step 2: Blacklist Filter
            blacklistFilter.doFilter(req, res, filterChain);
        });

        // Verify downstream was reached
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should reject request with 401 when token is revoked in Redis blacklist")
    void shouldRejectRevokedTokenInPipeline() throws Exception {
        String tokenHeader = createBearerToken("jti-revoked-200");
        String rawToken = tokenHeader.substring(7);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ledger/transfers");
        request.addHeader("Authorization", tokenHeader);
        request.setRequestURI("/api/v1/ledger/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(rawToken)).thenReturn(true);
        UserPrincipal principal = UserPrincipal.create(1001L, "teller_jane", List.of("ROLE_TELLER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, rawToken, principal.getAuthorities());
        when(jwtTokenProvider.getAuthentication(rawToken)).thenReturn(auth);
        when(tokenBlacklistService.isRevoked("jti-revoked-200")).thenReturn(true);

        jwtFilter.doFilter(request, response, (req, res) -> {
            blacklistFilter.doFilter(req, res, filterChain);
        });

        // Downstream should NOT be called
        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("\"title\":\"Token Revoked\"")
                .contains("\"errorCode\":\"AUTH_TOKEN_REVOKED\"")
                .contains("revoked");
    }

    @Test
    @DisplayName("Should set AUTH_TOKEN_EXPIRED when expired token is processed by filter pipeline")
    void shouldSetExpiredTokenAttributesInPipeline() throws Exception {
        String tokenHeader = createBearerToken("jti-expired-300");
        String rawToken = tokenHeader.substring(7);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ledger/transfers");
        request.addHeader("Authorization", tokenHeader);
        request.setRequestURI("/api/v1/ledger/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(rawToken)).thenReturn(false);
        when(jwtTokenProvider.isTokenExpired(rawToken)).thenReturn(true);

        jwtFilter.doFilter(request, response, (req, res) -> {
            entryPoint.commence((HttpServletRequest) req, (HttpServletResponse) res,
                    new BadCredentialsException("The token has expired"));
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("\"title\":\"Token Expired\"")
                .contains("\"errorCode\":\"AUTH_TOKEN_EXPIRED\"")
                .contains("Your session has expired");
    }

    @Test
    @DisplayName("Should return RFC-7807 problem details on unauthenticated entry point challenge")
    void shouldProduceRfc7807ProblemDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ledger/debit");
        request.setRequestURI("/api/v1/ledger/debit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("\"title\":\"Unauthorized\"")
                .contains("\"errorCode\":\"AUTH_INVALID_CREDENTIALS\"")
                .contains("\"instance\":\"/api/v1/ledger/debit\"");
    }
}

