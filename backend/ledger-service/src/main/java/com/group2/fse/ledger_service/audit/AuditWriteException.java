package com.group2.fse.ledger_service.audit;

public class AuditWriteException extends RuntimeException {

    private final Long transactionId;
    private final Long accountId;
    private final String referenceNo;

    public AuditWriteException(String message, Long transactionId, Long accountId, String referenceNo, Throwable cause) {
        super(message, cause);
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.referenceNo = referenceNo;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }
}
