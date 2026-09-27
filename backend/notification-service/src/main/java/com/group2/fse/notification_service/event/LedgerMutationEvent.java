package com.group2.fse.notification_service.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Task: FSE-601
 * Contract-compatible consumer DTO for ledger mutations (DEBIT / CREDIT).
 * Matches API_DESIGN_SPECIFICATION.md Section 6.2 and ledger-service LedgerMutationEvent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerMutationEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private MutationPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MutationPayload {
        private Long transactionId;
        private Long accountId;
        private String accountNumber;
        private String transactionType; // DEBIT, CREDIT

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
