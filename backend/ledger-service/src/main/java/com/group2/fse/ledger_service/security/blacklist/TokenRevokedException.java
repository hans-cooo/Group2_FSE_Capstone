package com.group2.fse.ledger_service.security.blacklist;

/**
 * Thrown internally when a revoked token is presented. Currently caught and
 * translated to a 401 directly inside TokenBlacklistFilter, since
 * spring-boot-starter-security (Carl, FSE-401) isn't merged yet and there's
 * no AuthenticationEntryPoint to delegate to. Once FSE-401/FSE-405 land,
 * swap the catch block in the filter to let this propagate to Alyssa's
 * CustomAuthenticationEntryPoint instead.
 */
public class TokenRevokedException extends RuntimeException {
    public TokenRevokedException(String message) {
        super(message);
    }
}