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
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Task: FSE-301 / FSE-307
 * HTTP request interceptor enforcing transaction idempotency via Redis locks,
 * error cleanup on abort, and 24-hour response caching with X-Cache-Replay replay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String ALT_IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    public static final String IDEMPOTENCY_KEY_ATTR = "IDEMPOTENCY_KEY_ATTR";

    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CACHE_TTL = Duration.ofHours(24);

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
            writeErrorResponse(request, response, HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY",
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
        writeErrorResponse(request, response, HttpStatus.CONFLICT, "CONCURRENT_TRANSACTION_IN_FLIGHT",
                "A transaction with this Idempotency-Key is currently being processed. Duplicate submission blocked.");
        return false;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) throws Exception {

        String idempotencyKey = (String) request.getAttribute(IDEMPOTENCY_KEY_ATTR);
        if (idempotencyKey == null) {
            return;
        }

        int status = response.getStatus();

        // If the transaction aborted or errored (HTTP 4xx/5xx or unhandled exception), release lock for retry
        if (ex != null || status >= 400) {
            log.warn("Transaction failed (status={}, ex={}); releasing idempotency lock for key: {}",
                    status, ex != null ? ex.getMessage() : "none", idempotencyKey);
            idempotencyService.release(idempotencyKey);
            return;
        }

        // If 2xx successful response, cache response body for 24 hours
        if (status >= 200 && status < 300) {
            ContentCachingResponseWrapper wrapper = null;
            if (response instanceof ContentCachingResponseWrapper ccrw) {
                wrapper = ccrw;
            } else if (request.getAttribute(ContentCachingResponseWrapperFilter.CACHED_RESPONSE_WRAPPER_ATTR)
                    instanceof ContentCachingResponseWrapper ccrw) {
                wrapper = ccrw;
            }

            if (wrapper != null) {
                byte[] contentBytes = wrapper.getContentAsByteArray();
                if (contentBytes.length > 0) {
                    String responseBody = new String(contentBytes, StandardCharsets.UTF_8);
                    idempotencyService.complete(idempotencyKey, responseBody, CACHE_TTL);
                    log.info("Successfully cached 2xx response for idempotency key {} (TTL=24h)", idempotencyKey);
                }
            }
        }
    }

    private String extractIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_HEADER);
        if (key == null || key.isBlank()) {
            key = request.getHeader(ALT_IDEMPOTENCY_HEADER);
        }
        return key != null ? key.trim() : null;
    }

    private void writeErrorResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String errorCode,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String json = String.format(
                "{\"type\":\"https://api.corebank.local/errors/%s\","
                        + "\"title\":\"%s\","
                        + "\"status\":%d,"
                        + "\"detail\":\"%s\","
                        + "\"instance\":\"%s\","
                        + "\"errorCode\":\"%s\","
                        + "\"timestamp\":\"%s\"}",
                errorCode,
                status.getReasonPhrase(),
                status.value(),
                message,
                request.getRequestURI(),
                errorCode,
                Instant.now().toString()
        );
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
