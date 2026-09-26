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
 * Domain Event published immediately post-commit for double-entry atomic account transfers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransferEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "LEDGER_TRANSFER_COMPLETED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String version = "1.0";

    private TransferPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferPayload {
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
    }
}
