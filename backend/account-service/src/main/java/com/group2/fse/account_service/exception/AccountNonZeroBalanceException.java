package com.group2.fse.account_service.exception;

import java.math.BigDecimal;

public class AccountNonZeroBalanceException extends RuntimeException {
    private final String accountNumber;
    private final BigDecimal balance;

    public AccountNonZeroBalanceException(String accountNumber, BigDecimal balance) {
        super(String.format("Account %s has an available balance of %s PHP. Funds must be withdrawn or transferred prior to closure.",
                accountNumber, balance != null ? balance.toPlainString() : "0.0000"));
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
