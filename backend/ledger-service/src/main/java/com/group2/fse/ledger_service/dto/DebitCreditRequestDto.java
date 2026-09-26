package com.group2.fse.ledger_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for authoritative debit and credit balance mutations.
 * Conforms to API_DESIGN_SPECIFICATION.md Section 4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitCreditRequestDto {

    @NotNull(message = "accountId is mandatory")
    private Long accountId;

    @NotNull(message = "amount is mandatory")
    @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
    @Digits(integer = 14, fraction = 4, message = "amount must have at most 4 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "referenceNo is mandatory")
    @Size(max = 100, message = "referenceNo must not exceed 100 characters")
    private String referenceNo;

    @Size(max = 255, message = "remarks must not exceed 255 characters")
    private String remarks;
}
