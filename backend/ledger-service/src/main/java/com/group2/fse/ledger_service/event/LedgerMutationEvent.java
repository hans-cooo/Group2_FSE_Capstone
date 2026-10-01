package com.group2.fse.ledger_service.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Task: FSE-501
 * Domain Event published immediately post-commit for ledger mutations (DEBIT / CREDIT).
 * Strict compliance with API_DESIGN_SPECIFICATION.md Section 6.2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerMutationEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "LEDGER_MUTATION_COMPLETED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String version = "1.0";

    private MutationPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MutationPayload {
        private Long transactionId;
        private Long accountId;
        private String accountNumber;
        private String transactionType; // "DEBIT" or "CREDIT"

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal amount;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal previousBalance;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal newBalance;

        private String referenceNo;
        private Long customerId;
    }
}
