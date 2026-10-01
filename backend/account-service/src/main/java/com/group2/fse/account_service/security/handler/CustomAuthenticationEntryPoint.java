package com.group2.fse.account_service.security.handler;

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
 * Authentication Entry Point returning RFC-7807 compliant application/problem+json on 401 Unauthorized.
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
                detail = "Token has been revoked or logged out. Please log in again.";
            } else {
                detail = "Full authentication is required to access this resource.";
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://api.corebank.local/errors/" + errorCode);
        body.put("title", "Unauthorized");
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("errorCode", errorCode);
        body.put("timestamp", Instant.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
