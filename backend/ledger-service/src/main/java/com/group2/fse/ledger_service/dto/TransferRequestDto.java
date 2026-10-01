package com.group2.fse.ledger_service.dto;

import jakarta.validation.constraints.AssertTrue;
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
 * Request payload for atomic double-entry fund transfers between accounts.
 * Conforms to API_DESIGN_SPECIFICATION.md Section 4.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDto {

    @NotNull(message = "sourceAccountId is mandatory")
    private Long sourceAccountId;

    @NotNull(message = "destinationAccountId is mandatory")
    private Long destinationAccountId;

    @NotNull(message = "amount is mandatory")
    @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
    @Digits(integer = 14, fraction = 4, message = "amount must have at most 4 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "referenceNo is mandatory")
    @Size(max = 100, message = "referenceNo must not exceed 100 characters")
    private String referenceNo;

    @Size(max = 255, message = "remarks must not exceed 255 characters")
    private String remarks;

    @AssertTrue(message = "sourceAccountId and destinationAccountId must be distinct accounts")
    public boolean isDistinctAccounts() {
        if (sourceAccountId == null || destinationAccountId == null) {
            return true; // handled by @NotNull
        }
        return !sourceAccountId.equals(destinationAccountId);
    }
}
