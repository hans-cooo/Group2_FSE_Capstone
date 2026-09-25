package com.group2.fse.ledger_service.dto;

import java.math.BigDecimal;

public class TransferRequestDto {

    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String referenceNo;
    private String idempotencyKey;
    private Long actorId;
    private String clientIp;

    public TransferRequestDto() {}

    public TransferRequestDto(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String referenceNo) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.referenceNo = referenceNo;
    }

    public TransferRequestDto(Long sourceAccountId, Long destinationAccountId, BigDecimal amount,
                              String referenceNo, String idempotencyKey, Long actorId, String clientIp) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.referenceNo = referenceNo;
        this.idempotencyKey = idempotencyKey;
        this.actorId = actorId;
        this.clientIp = clientIp;
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

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}
