package com.group2.fse.ledger_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MutationResult {

    private boolean success;
    private String referenceNo;
    private Long accountId;
    private Long transactionId;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private BigDecimal mutatedAmount;
    private String errorMessage;
    private LocalDateTime timestamp;

    public MutationResult() {
        this.timestamp = LocalDateTime.now();
    }

    public static MutationResult success(Long accountId, String referenceNo, 
                                          BigDecimal previousBalance, BigDecimal newBalance, 
                                          BigDecimal mutatedAmount) {
        return success(accountId, referenceNo, null, previousBalance, newBalance, mutatedAmount);
    }

    public static MutationResult success(Long accountId, String referenceNo, Long transactionId,
                                          BigDecimal previousBalance, BigDecimal newBalance, 
                                          BigDecimal mutatedAmount) {
        MutationResult res = new MutationResult();
        res.success = true;
        res.accountId = accountId;
        res.referenceNo = referenceNo;
        res.transactionId = transactionId;
        res.previousBalance = previousBalance;
        res.newBalance = newBalance;
        res.mutatedAmount = mutatedAmount;
        res.timestamp = LocalDateTime.now();
        return res;
    }

    public static MutationResult failure(Long accountId, String referenceNo, String errorMessage) {
        MutationResult res = new MutationResult();
        res.success = false;
        res.accountId = accountId;
        res.referenceNo = referenceNo;
        res.errorMessage = errorMessage;
        res.timestamp = LocalDateTime.now();
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(BigDecimal previousBalance) {
        this.previousBalance = previousBalance;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public BigDecimal getMutatedAmount() {
        return mutatedAmount;
    }

    public void setMutatedAmount(BigDecimal mutatedAmount) {
        this.mutatedAmount = mutatedAmount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}