package com.group2.fse.ledger_service.interceptor;

import com.group2.fse.ledger_service.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyInterceptor Unit Tests (Carl - FSE-301)")
class IdempotencyInterceptorTest {

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private IdempotencyInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should bypass idempotency check for GET requests")
    void shouldBypassForGetRequests() throws Exception {
        request.setMethod("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(idempotencyService);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when Idempotency-Key header is missing on POST")
    void shouldRejectWhenHeaderMissing() throws Exception {
        request.setMethod("POST");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("MISSING_IDEMPOTENCY_KEY"));
        verifyNoInteractions(idempotencyService);
    }

    @Test
    @DisplayName("Should allow request through when idempotency lock is newly acquired")
    void shouldAllowWhenLockAcquired() throws Exception {
        request.setMethod("POST");
        request.addHeader("Idempotency-Key", "idemp-uuid-test-1");

        when(idempotencyService.tryAcquire(eq("idemp-uuid-test-1"), any(Duration.class))).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals("idemp-uuid-test-1", request.getAttribute(IdempotencyInterceptor.IDEMPOTENCY_KEY_ATTR));
        verify(idempotencyService).tryAcquire(eq("idemp-uuid-test-1"), any(Duration.class));
    }

    @Test
    @DisplayName("Should return 409 Conflict when duplicate transaction is in-flight")
    void shouldRejectConcurrentInFlightDuplicate() throws Exception {
        request.setMethod("POST");
        request.addHeader("Idempotency-Key", "idemp-in-flight-key");

        when(idempotencyService.tryAcquire(eq("idemp-in-flight-key"), any(Duration.class))).thenReturn(false);
        when(idempotencyService.getCachedResponse("idemp-in-flight-key")).thenReturn(Optional.empty());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("CONCURRENT_TRANSACTION_IN_FLIGHT"));
    }

    @Test
    @DisplayName("Should replay cached response with 200 OK when transaction was previously completed")
    void shouldReplayCachedResponse() throws Exception {
        request.setMethod("POST");
        request.addHeader("Idempotency-Key", "idemp-completed-key");

        String cachedJson = "{\"referenceNo\":\"REF-1001\",\"status\":\"COMPLETED\",\"amount\":5000}";

        when(idempotencyService.tryAcquire(eq("idemp-completed-key"), any(Duration.class))).thenReturn(false);
        when(idempotencyService.getCachedResponse("idemp-completed-key")).thenReturn(Optional.of(cachedJson));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(200, response.getStatus());
        assertEquals("true", response.getHeader("X-Cache-Replay"));
        assertEquals(cachedJson, response.getContentAsString());
    }
}
