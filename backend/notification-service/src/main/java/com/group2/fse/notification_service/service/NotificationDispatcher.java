package com.group2.fse.notification_service.service;

import com.group2.fse.notification_service.domain.Notification;
import com.group2.fse.notification_service.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Task: FSE-601
 * Multi-channel alert dispatcher (PUSH, SMS, EMAIL, IN_APP).
 * Guaranteed fallback to formatted console banners for live evaluations and testing.
 */
@Slf4j
@Service
public class NotificationDispatcher {

    @Async
    public void dispatch(Notification notification) {
        if (notification == null) {
            return;
        }

        NotificationChannel channel = notification.getChannel() != null 
                ? notification.getChannel() 
                : NotificationChannel.IN_APP;

        switch (channel) {
            case SMS -> dispatchSms(notification);
            case EMAIL -> dispatchEmail(notification);
            case PUSH -> dispatchPush(notification);
            case IN_APP -> dispatchInApp(notification);
        }

        logTerminalBanner(notification);
    }

    private void dispatchSms(Notification notification) {
        log.info("[SMS GATEWAY] Sending SMS to Customer [{}]: Title='{}' Body='{}'",
                notification.getCustomerId(), notification.getTitle(), notification.getMessage());
    }

    private void dispatchEmail(Notification notification) {
        log.info("[EMAIL GATEWAY] Sending Email to Customer [{}]: Subject='{}' Content='{}'",
                notification.getCustomerId(), notification.getTitle(), notification.getMessage());
    }

    private void dispatchPush(Notification notification) {
        log.info("[PUSH GATEWAY] Pushing alert to Customer [{}] Device: Title='{}' Payload='{}'",
                notification.getCustomerId(), notification.getTitle(), notification.getMessage());
    }

    private void dispatchInApp(Notification notification) {
        log.info("[IN-APP FEED] Stored in-app notification for Customer [{}]: ID={}",
                notification.getCustomerId(), notification.getId());
    }

    private void logTerminalBanner(Notification n) {
        String banner = String.format(
                """
                
                ========================================================================================
                ?? [NOTIFICATION SERVICE DISPATCHED ALERT]
                Channel:     [%s]
                Customer ID: [%s]
                Event ID:    [%s]
                Title:       [%s]
                Message:     %s
                Timestamp:   [%s]
                ========================================================================================
                """,
                n.getChannel(),
                n.getCustomerId(),
                n.getEventId() != null ? n.getEventId() : "N/A",
                n.getTitle(),
                n.getMessage(),
                n.getCreatedAt()
        );
        log.info("{}", banner);
    }
}
