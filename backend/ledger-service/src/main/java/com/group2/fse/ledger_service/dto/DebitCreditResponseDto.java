package com.group2.fse.ledger_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response payload for completed balance mutations.
 * Conforms to API_DESIGN_SPECIFICATION.md Section 4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitCreditResponseDto {

    private Long transactionId;
    private Long accountId;
    private String transactionType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal previousBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal newBalance;

    private String status;
    private String referenceNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
}
