package com.group2.fse.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Task: FSE-601
 * Core Banking Notification & Event Consumer Microservice.
 * Listens to Kafka domain events and delivers multi-channel customer alerts.
 */
@EnableAsync
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
