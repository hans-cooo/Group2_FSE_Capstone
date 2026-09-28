package com.group2.fse.notification_service.service;

import com.group2.fse.notification_service.domain.Notification;
import com.group2.fse.notification_service.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Task: FSE-601
 * In-App Notification Dispatcher.
 * SMS and Email channels are retired / out of scope.
 * Delivers customer in-app notifications and prints real-time terminal audit banners.
 */
@Slf4j
@Service
public class NotificationDispatcher {

    @Async
    public void dispatch(Notification notification) {
        if (notification == null) {
            return;
        }

        dispatchInApp(notification);
        logTerminalBanner(notification);
    }

    private void dispatchInApp(Notification notification) {
        log.info("[IN-APP FEED] Stored in-app notification for Customer [{}]: ID={}, Title='{}'",
                notification.getCustomerId(), notification.getId(), notification.getTitle());
    }

    private void logTerminalBanner(Notification n) {
        String banner = String.format(
                """
                
                ========================================================================================
                🔔 [IN-APP NOTIFICATION SERVICE DISPATCHED ALERT]
                Channel:     [%s]
                Customer ID: [%s]
                Event ID:    [%s]
                Title:       [%s]
                Message:     %s
                Timestamp:   [%s]
                ========================================================================================
                """,
                n.getChannel() != null ? n.getChannel() : NotificationChannel.IN_APP,
                n.getCustomerId(),
                n.getEventId() != null ? n.getEventId() : "N/A",
                n.getTitle(),
                n.getMessage(),
                n.getCreatedAt()
        );
        log.info("{}", banner);
    }
}
