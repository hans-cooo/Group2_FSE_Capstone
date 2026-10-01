package com.group2.fse.account_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycRequestEvaluatedEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "KYC_REQUEST_EVALUATED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String version = "1.0";

    private KycEvaluatedPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycEvaluatedPayload {
        private Long requestId;
        private Long customerId;
        private String status; // "APPROVED" or "REJECTED"
        private Long reviewedByStaffId;
        private String reviewedAt;
        private String rejectionReason;
    }
}
