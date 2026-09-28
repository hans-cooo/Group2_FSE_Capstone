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
public class KycRequestSubmittedEvent {

    @Builder.Default
    private String eventId = "evt_" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "KYC_REQUEST_SUBMITTED";

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String version = "1.0";

    private KycSubmittedPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycSubmittedPayload {
        private Long requestId;
        private Long customerId;
        private String status;
        private String submittedAt;
    }
}
