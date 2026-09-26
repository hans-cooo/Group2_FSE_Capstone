package com.group2.fse.ledger_service.exception;

import lombok.Getter;

/**
 * Thrown when an account is FROZEN, CLOSED, or not in an ACTIVE status.
 * Maps to RFC-7807 problem details with error code ACCOUNT_NOT_ACTIVE (422).
 */
@Getter
public class AccountNotActiveException extends RuntimeException {

    private final Long accountId;
    private final String accountStatus;

    public AccountNotActiveException(Long accountId, String accountStatus) {
        super(String.format("Account %s is not active (current status: %s). Mutations are prohibited.",
                accountId, accountStatus));
        this.accountId = accountId;
        this.accountStatus = accountStatus;
    }

    public AccountNotActiveException(String message) {
        super(message);
        this.accountId = null;
        this.accountStatus = null;
    }
}
