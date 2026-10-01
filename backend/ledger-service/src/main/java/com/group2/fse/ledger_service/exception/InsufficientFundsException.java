package com.group2.fse.ledger_service.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Thrown when an account does not hold sufficient available funds for a mutation.
 * Error Code: INSUFFICIENT_FUNDS (HTTP 422).
 */
@Getter
public class InsufficientFundsException extends RuntimeException {

    private final Long accountId;
    private final BigDecimal currentBalance;
    private final BigDecimal attemptedAmount;

    public InsufficientFundsException(Long accountId, BigDecimal currentBalance, BigDecimal attemptedAmount) {
        super(String.format("Account %d balance of %s is insufficient for amount %s",
                accountId, currentBalance != null ? currentBalance.toPlainString() : "0.0000",
                attemptedAmount != null ? attemptedAmount.toPlainString() : "0.0000"));
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.attemptedAmount = attemptedAmount;
    }

    public InsufficientFundsException(String message) {
        super(message);
        this.accountId = null;
        this.currentBalance = null;
        this.attemptedAmount = null;
    }
}
