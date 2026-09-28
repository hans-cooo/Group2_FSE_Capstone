package com.group2.fse.notification_service.domain;

public enum NotificationChannel {
    IN_APP,
    PUSH,
    @Deprecated
    SMS,
    @Deprecated
    EMAIL
}
