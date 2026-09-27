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
 * Contract-compatible consumer DTO for atomic ledger transfers.
 * Matches ledger-service LedgerTransferEvent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerTransferEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private TransferPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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

        private Long sourceCustomerId;
        private Long destinationCustomerId;
    }
}
