package com.group2.fse.ledger_service.exception;

import lombok.Getter;

/**
 * Thrown when an account mutation is blocked by an active administrative hold,
 * judicial flag, or anti-money-laundering (AML) risk freeze.
 * Maps to RFC-7807 problem details with error code ACCOUNT_ACTIVE_FLAG_EXISTS (422).
 */
@Getter
public class AccountActiveFlagExistsException extends RuntimeException {

    private final Long accountId;
    private final String flagType;

    public AccountActiveFlagExistsException(Long accountId, String flagType) {
        super(String.format("Account %s has an active administrative hold [%s]. Financial mutations are restricted.",
                accountId, flagType != null ? flagType : "GENERAL_HOLD"));
        this.accountId = accountId;
        this.flagType = flagType;
    }

    public AccountActiveFlagExistsException(String message) {
        super(message);
        this.accountId = null;
        this.flagType = null;
    }
}
