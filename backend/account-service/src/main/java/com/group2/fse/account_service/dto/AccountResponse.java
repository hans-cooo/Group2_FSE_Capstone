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
public class AccountResponse {
    private Long accountId;
    private Long customerId;
    private String customerName;
    private String accountNumber;
    private String accountType;
    private String currency;
    private String status;
    private BigDecimal availableBalance;
    private LocalDateTime createdAt;
    private String message;
}
