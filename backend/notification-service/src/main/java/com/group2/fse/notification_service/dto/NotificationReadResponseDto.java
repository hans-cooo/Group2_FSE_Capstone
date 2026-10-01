package com.group2.fse.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task: FSE-601
 * Acknowledgement response when marking a notification as read.
 * Conforms strictly to API_DESIGN_SPECIFICATION.md Section 6.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReadResponseDto {
    private Long notificationId;
    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private Boolean isRead;
}
