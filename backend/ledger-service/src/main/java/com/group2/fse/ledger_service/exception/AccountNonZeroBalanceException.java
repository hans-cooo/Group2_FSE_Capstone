package com.group2.fse.ledger_service.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Thrown when an account closure request is submitted while available funds remain.
 * Maps to RFC-7807 problem details with error code ACCOUNT_NON_ZERO_BALANCE (400).
 */
@Getter
public class AccountNonZeroBalanceException extends RuntimeException {

    private final String accountNumber;
    private final BigDecimal availableBalance;

    public AccountNonZeroBalanceException(String accountNumber, BigDecimal availableBalance) {
        super(String.format("Account %s has an available balance of %s PHP. Funds must be withdrawn or transferred prior to closure.",
                accountNumber, availableBalance != null ? availableBalance.toPlainString() : "0.0000"));
        this.accountNumber = accountNumber;
        this.availableBalance = availableBalance;
    }

    public AccountNonZeroBalanceException(String message) {
        super(message);
        this.accountNumber = null;
        this.availableBalance = null;
    }
}
