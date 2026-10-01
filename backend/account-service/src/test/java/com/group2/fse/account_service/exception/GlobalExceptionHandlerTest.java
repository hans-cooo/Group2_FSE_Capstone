package com.group2.fse.account_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RFC-7807 Global Exception Handler Problem Details Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/accounts/4/closure-request");
    }

    @Test
    @DisplayName("[ERR-001] Should return RFC-7807 problem details for AccountNonZeroBalanceException with 400 Bad Request")
    void shouldHandleAccountNonZeroBalance() {
        AccountNonZeroBalanceException ex =
                new AccountNonZeroBalanceException("ACC_10000004", new BigDecimal("12500.0000"));

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccountNonZeroBalance(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ACCOUNT_NON_ZERO_BALANCE", body.get("errorCode"));
        assertEquals(400, body.get("status"));
        assertEquals("ACC_10000004", body.get("accountNumber"));
        assertEquals(new BigDecimal("12500.0000"), body.get("availableBalance"));
        assertTrue(((String) body.get("detail")).contains("12500.0000"));
    }

    @Test
    @DisplayName("[ERR-002] Should return RFC-7807 problem details for AccountActiveFlagException with 422 Unprocessable Entity")
    void shouldHandleAccountActiveFlag() {
        AccountActiveFlagException ex =
                new AccountActiveFlagException("Operation blocked by active risk/judicial hold on account ACC_10000001");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccountActiveFlag(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ACCOUNT_ACTIVE_FLAG_EXISTS", body.get("errorCode"));
        assertEquals(422, body.get("status"));
    }

    @Test
    @DisplayName("[ERR-003] Should return RFC-7807 problem details for AccountNotFoundException with 404 Not Found")
    void shouldHandleAccountNotFound() {
        AccountNotFoundException ex = new AccountNotFoundException("Account not found with ID: 999");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleAccountNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ACCOUNT_NOT_FOUND", body.get("errorCode"));
        assertEquals(404, body.get("status"));
    }

    @Test
    @DisplayName("[ERR-004] Should return RFC-7807 problem details for DuplicateClosureRequestException with 409 Conflict")
    void shouldHandleDuplicateClosureRequest() {
        DuplicateClosureRequestException ex =
                new DuplicateClosureRequestException("A pending closure request already exists");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDuplicateClosureRequest(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("DUPLICATE_CLOSURE_REQUEST", body.get("errorCode"));
        assertEquals(409, body.get("status"));
    }

    @Test
    @DisplayName("[ERR-005] Should return RFC-7807 problem details for UnauthorizedAccountAccessException with 403 Forbidden")
    void shouldHandleUnauthorizedAccountAccess() {
        UnauthorizedAccountAccessException ex =
                new UnauthorizedAccountAccessException("You do not own this account");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleUnauthorizedAccountAccess(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("AUTH_ACCESS_DENIED", body.get("errorCode"));
        assertEquals(403, body.get("status"));
    }
}
