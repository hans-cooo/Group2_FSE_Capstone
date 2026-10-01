package com.group2.fse.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group2.fse.notification_service.domain.Notification;
import com.group2.fse.notification_service.domain.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Task: FSE-601
 * Response DTO for an individual in-app notification.
 * Conforms strictly to API_DESIGN_SPECIFICATION.md Section 6.1.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long notificationId;
    private String title;
    private String message;
    private NotificationChannel channel;
    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private Boolean isRead;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    public static NotificationResponseDto fromEntity(Notification entity) {
        if (entity == null) {
            return null;
        }
        return NotificationResponseDto.builder()
                .notificationId(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .channel(entity.getChannel())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
