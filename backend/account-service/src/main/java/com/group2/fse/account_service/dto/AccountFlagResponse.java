package com.group2.fse.account_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountFlagResponse {
    private Long flagId;
    private Long accountId;
    private String reason;
    private String status;
    private Long flaggedBy;
    private LocalDateTime flaggedAt;
    private Long removedBy;
    private LocalDateTime removedAt;
    private String message;
}
