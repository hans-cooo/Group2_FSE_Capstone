package com.group2.fse.ledger_service.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CustomAuthenticationEntryPoint entryPoint;
    private CustomAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        entryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
    }

    @Test
    @DisplayName("Should return RFC-7807 401 problem details on authentication failure")
    void shouldReturn401ProblemDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ledger/transfers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Invalid token signature"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":401")
                .contains("\"title\":\"Unauthorized\"")
                .contains("\"errorCode\":\"AUTH_INVALID_CREDENTIALS\"")
                .contains("\"instance\":\"/api/v1/ledger/transfers\"");
    }

    @Test
    @DisplayName("Should return RFC-7807 401 problem details on expired token")
    void shouldReturn401ExpiredTokenProblemDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ledger/transfers");
        request.setAttribute(CustomAuthenticationEntryPoint.ATTR_ERROR_CODE, "AUTH_TOKEN_EXPIRED");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("The token has expired"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":401")
                .contains("\"title\":\"Token Expired\"")
                .contains("\"errorCode\":\"AUTH_TOKEN_EXPIRED\"")
                .contains("Your session has expired")
                .contains("\"instance\":\"/api/v1/ledger/transfers\"");
    }

    @Test
    @DisplayName("Should return RFC-7807 401 problem details on revoked token")
    void shouldReturn401RevokedTokenProblemDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ledger/transfers");
        request.setAttribute(CustomAuthenticationEntryPoint.ATTR_ERROR_CODE, "AUTH_TOKEN_REVOKED");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Token has been revoked/logged out"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":401")
                .contains("\"title\":\"Token Revoked\"")
                .contains("\"errorCode\":\"AUTH_TOKEN_REVOKED\"")
                .contains("revoked")
                .contains("\"instance\":\"/api/v1/ledger/transfers\"");
    }

    @Test
    @DisplayName("Should return RFC-7807 403 problem details on access denied")
    void shouldReturn403ProblemDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ledger/debit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Insufficient role privileges"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":403")
                .contains("\"title\":\"Forbidden\"")
                .contains("\"errorCode\":\"AUTH_ACCESS_DENIED\"")
                .contains("\"instance\":\"/api/v1/ledger/debit\"");
    }
}

