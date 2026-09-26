package com.group2.fse.ledger_service.security.blacklist;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when a revoked token is presented.
 * Handled centrally by CustomAuthenticationEntryPoint and GlobalExceptionHandler
 * to produce standardized RFC-7807 problem details (FSE-405).
 */
public class TokenRevokedException extends AuthenticationException {

    public TokenRevokedException(String message) {
        super(message);
    }

    public TokenRevokedException(String message, Throwable cause) {
        super(message, cause);
    }
}