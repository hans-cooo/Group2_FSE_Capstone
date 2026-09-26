package com.group2.fse.ledger_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response payload for completed double-entry fund transfers.
 * Conforms to API_DESIGN_SPECIFICATION.md Section 4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {

    private String transferReference;
    private Long sourceAccountId;
    private Long destinationAccountId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal sourcePreviousBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal sourceNewBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal destinationPreviousBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal destinationNewBalance;

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
}
