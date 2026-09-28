package com.group2.fse.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Consumer DTO for KYC update request evaluation (approval or rejection).
 * Matches account-service KycRequestEvaluatedEvent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KycRequestEvaluatedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private KycEvaluatedPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KycEvaluatedPayload {
        private Long requestId;
        private Long customerId;
        private String status; // "APPROVED" or "REJECTED"
        private Long reviewedByStaffId;
        private String reviewedAt;
        private String rejectionReason;
    }
}
