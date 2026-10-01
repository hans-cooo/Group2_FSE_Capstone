package com.group2.fse.ledger_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response payload for account balance queries.
 * Conforms to API_DESIGN_SPECIFICATION.md Section 4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponseDto {

    private Long accountId;
    private String currency;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal availableBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant asOfTimestamp;

    private Boolean isCached;
}
