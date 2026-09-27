package com.group2.fse.account_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClosureRequestResponse {
    private Long closureRequestId;
    private Long accountId;
    private String accountNumber;
    private String accountType;
    private Long customerId;
    private String customerName;
    private BigDecimal availableBalance;
    private String reason;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private Long approvedBy;
    private String rejectionReason;

    // Additional response fields for approval
    private String accountStatus;
    private String customerStatus;
    private String message;
}
