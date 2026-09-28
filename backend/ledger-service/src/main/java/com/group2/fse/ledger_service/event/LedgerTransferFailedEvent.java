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
 * Domain Event published when an account transfer fails validation or execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransferFailedEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "LEDGER_TRANSFER_FAILED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String version = "1.0";

    private FailedTransferPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedTransferPayload {
        private String transferReference;
        private Long sourceAccountId;
        private Long destinationAccountId;
        private Long sourceCustomerId;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal amount;

        private String failureReason;
        private String errorCode;
    }
}
