package com.group2.fse.ledger_service.exception;

import lombok.Getter;

/**
 * Thrown when an account or balance record cannot be located.
 * Error Code: ACCOUNT_NOT_FOUND (HTTP 404).
 */
@Getter
public class AccountNotFoundException extends RuntimeException {

    private final Long accountId;

    public AccountNotFoundException(Long accountId) {
        super(String.format("Account %d balance record not found", accountId));
        this.accountId = accountId;
    }

    public AccountNotFoundException(String message) {
        super(message);
        this.accountId = null;
    }
}
