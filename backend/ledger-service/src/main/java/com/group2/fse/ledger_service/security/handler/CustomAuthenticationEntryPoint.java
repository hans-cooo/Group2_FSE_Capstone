package com.group2.fse.ledger_service.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Task: FSE-405
 * Assigned to: Alyssa
 * Authentication Entry Point returning RFC-7807 compliant application/problem+json on 401 Unauthorized.
 * Differentiates security violations into user-friendly error codes:
 * - AUTH_TOKEN_EXPIRED: When JWT lifetime has expired
 * - AUTH_TOKEN_REVOKED: When session has been logged out in Redis blacklist
 * - AUTH_INVALID_CREDENTIALS: For missing or malformed signatures
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ATTR_ERROR_CODE = "SECURITY_ERROR_CODE";
    public static final String ATTR_ERROR_DETAIL = "SECURITY_ERROR_DETAIL";

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(),
                authException != null ? authException.getMessage() : "Authentication exception");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String errorCode = (String) request.getAttribute(ATTR_ERROR_CODE);
        String detail = (String) request.getAttribute(ATTR_ERROR_DETAIL);

        if (errorCode == null || errorCode.isBlank()) {
            errorCode = "AUTH_INVALID_CREDENTIALS";
        }

        if (detail == null || detail.isBlank()) {
            if ("AUTH_TOKEN_EXPIRED".equals(errorCode)) {
                detail = "Your session has expired. Please log in again to continue.";
            } else if ("AUTH_TOKEN_REVOKED".equals(errorCode)) {
                detail = "The token has been revoked or logged out. Please authenticate with new credentials.";
            } else {
                detail = authException != null && authException.getMessage() != null && !authException.getMessage().isBlank()
                        ? authException.getMessage()
                        : "Full authentication is required to access this resource.";
            }
        }

        String title = "Unauthorized";
        if ("AUTH_TOKEN_EXPIRED".equals(errorCode)) {
            title = "Token Expired";
        } else if ("AUTH_TOKEN_REVOKED".equals(errorCode)) {
            title = "Token Revoked";
        }

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://api.corebank.local/errors/" + errorCode);
        problem.put("title", title);
        problem.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());
        problem.put("errorCode", errorCode);
        problem.put("timestamp", Instant.now().toString());
        problem.put("error", "UNAUTHORIZED");
        problem.put("message", detail);
        problem.put("path", request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
