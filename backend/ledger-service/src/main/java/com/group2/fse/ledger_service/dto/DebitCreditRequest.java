package com.group2.fse.ledger_service.dto;

import java.math.BigDecimal;

public class DebitCreditRequest {

    private Long accountId;
    private BigDecimal amount;
    private String referenceNo;

    public DebitCreditRequest() {}

    public DebitCreditRequest(Long accountId, BigDecimal amount, String referenceNo) {
        this.accountId = accountId;
        this.amount = amount;
        this.referenceNo = referenceNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }
}