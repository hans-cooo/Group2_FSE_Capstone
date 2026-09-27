package com.group2.fse.notification_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Task: FSE-601
 * Persistent entity for customer transactional alerts and notifications.
 * Implements idempotent tracking via unique eventId.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_customer", columnList = "customer_id"),
        @Index(name = "idx_notifications_event_id", columnList = "event_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
