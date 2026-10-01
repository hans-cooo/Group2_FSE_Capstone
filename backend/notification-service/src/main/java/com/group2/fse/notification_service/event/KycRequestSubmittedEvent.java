package com.group2.fse.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Consumer DTO for KYC update request submission.
 * Matches account-service KycRequestSubmittedEvent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KycRequestSubmittedEvent {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String version;
    private KycSubmittedPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KycSubmittedPayload {
        private Long requestId;
        private Long customerId;
        private String status;
        private String submittedAt;
    }
}
