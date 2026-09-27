package com.group2.fse.audit_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForensicAuditResponseDto {
    private Long auditId;
    private Long transactionId;
    private Long accountId;
    private String referenceNo;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private String previousHash;
    private String currentHash;
    private Long actorId;
    private String clientIp;
    private LocalDateTime eventTimestamp;
}