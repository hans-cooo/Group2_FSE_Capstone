package com.group2.fse.ledger_service.interceptor;

import com.group2.fse.ledger_service.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Task: FSE-301
 * Assigned to: Carl
 * HTTP request interceptor enforcing transaction idempotency via Redis locks and caching.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String ALT_IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    public static final String IDEMPOTENCY_KEY_ATTR = "IDEMPOTENCY_KEY_ATTR";

    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(30);

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();

        // Only enforce idempotency on state-mutating requests
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }

        String idempotencyKey = extractIdempotencyKey(request);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY",
                    "Idempotency-Key header is required for transaction mutation requests.");
            return false;
        }

        // Attempt atomic acquisition via Redis SETNX
        boolean acquired = idempotencyService.tryAcquire(idempotencyKey, LOCK_TIMEOUT);

        if (acquired) {
            request.setAttribute(IDEMPOTENCY_KEY_ATTR, idempotencyKey);
            return true;
        }

        // Duplicate detected: Check if previously completed and cached
        Optional<String> cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            log.info("Idempotent replay: Returning cached response for key: {}", idempotencyKey);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-Cache-Replay", "true");
            response.getWriter().write(cachedResponse.get());
            response.getWriter().flush();
            return false;
        }

        // Key is still in-flight
        log.warn("Blocked duplicate in-flight transaction with key: {}", idempotencyKey);
        writeErrorResponse(response, HttpStatus.CONFLICT, "CONCURRENT_TRANSACTION_IN_FLIGHT",
                "A transaction with this Idempotency-Key is currently being processed. Duplicate submission blocked.");
        return false;
    }

    private String extractIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_HEADER);
        if (key == null || key.isBlank()) {
            key = request.getHeader(ALT_IDEMPOTENCY_HEADER);
        }
        return key != null ? key.trim() : null;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format("{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                errorCode, message, status.value());
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
