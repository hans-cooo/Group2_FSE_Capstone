package com.group2.fse.audit_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChainVerificationResponseDto {
    private Long accountId;
    private long totalRecordsVerified;
    @com.fasterxml.jackson.annotation.JsonProperty("isChainIntact")
    private boolean isChainIntact;
    private String latestHash;
    private LocalDateTime verifiedAt;
    private String message;
}