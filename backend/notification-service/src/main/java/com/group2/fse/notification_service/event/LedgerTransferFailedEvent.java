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
 * Consumer DTO for failed ledger transfers.
 * Matches ledger-service LedgerTransferFailedEvent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerTransferFailedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private FailedTransferPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
