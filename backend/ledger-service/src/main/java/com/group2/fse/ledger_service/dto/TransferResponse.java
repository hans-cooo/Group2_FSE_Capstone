package com.group2.fse.ledger_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {

    private boolean success;
    private String referenceNo;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private BigDecimal sourceNewBalance;
    private BigDecimal destinationNewBalance;
    private String status;
    private String errorMessage;
    private LocalDateTime timestamp;

    public TransferResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static TransferResponse success(String referenceNo, Long sourceAccountId, Long destinationAccountId,
                                           BigDecimal amount, BigDecimal sourceNewBalance, BigDecimal destinationNewBalance) {
        TransferResponse res = new TransferResponse();
        res.success = true;
        res.referenceNo = referenceNo;
        res.sourceAccountId = sourceAccountId;
        res.destinationAccountId = destinationAccountId;
        res.amount = amount;
        res.sourceNewBalance = sourceNewBalance;
        res.destinationNewBalance = destinationNewBalance;
        res.status = "COMPLETED";
        res.timestamp = LocalDateTime.now();
        return res;
    }

    public static TransferResponse failure(String referenceNo, Long sourceAccountId, Long destinationAccountId,
                                           BigDecimal amount, String status, String errorMessage) {
        TransferResponse res = new TransferResponse();
        res.success = false;
        res.referenceNo = referenceNo;
        res.sourceAccountId = sourceAccountId;
        res.destinationAccountId = destinationAccountId;
        res.amount = amount;
        res.status = status;
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

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getSourceNewBalance() {
        return sourceNewBalance;
    }

    public void setSourceNewBalance(BigDecimal sourceNewBalance) {
        this.sourceNewBalance = sourceNewBalance;
    }

    public BigDecimal getDestinationNewBalance() {
        return destinationNewBalance;
    }

    public void setDestinationNewBalance(BigDecimal destinationNewBalance) {
        this.destinationNewBalance = destinationNewBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
