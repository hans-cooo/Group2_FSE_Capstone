package com.group2.fse.notification_service.listener;

import com.group2.fse.notification_service.event.AccountClosureEvent;
import com.group2.fse.notification_service.event.KycRequestEvaluatedEvent;
import com.group2.fse.notification_service.event.KycRequestSubmittedEvent;
import com.group2.fse.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Task: FSE-601
 * Asynchronous Kafka consumer for account lifecycle domain events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountNotificationListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${notification.kafka.topics.account-closure:account.closure.completed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onAccountClosureEvent(@Payload AccountClosureEvent event) {
        log.info("Received AccountClosureEvent from Kafka: eventId={}, customerId={}",
                event != null ? event.getEventId() : "null",
                event != null && event.getPayload() != null ? event.getPayload().getCustomerId() : "null");
        try {
            notificationService.processAccountClosureEvent(event);
        } catch (Exception ex) {
            log.error("Error processing AccountClosureEvent [{}]: {}",
                    event != null ? event.getEventId() : "null", ex.getMessage(), ex);
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.kyc-submitted:kyc.request.submitted.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onKycSubmittedEvent(@Payload KycRequestSubmittedEvent event) {
        log.info("Received KycRequestSubmittedEvent from Kafka: eventId={}, customerId={}",
                event != null ? event.getEventId() : "null",
                event != null && event.getPayload() != null ? event.getPayload().getCustomerId() : "null");
        try {
            notificationService.processKycSubmittedEvent(event);
        } catch (Exception ex) {
            log.error("Error processing KycRequestSubmittedEvent [{}]: {}",
                    event != null ? event.getEventId() : "null", ex.getMessage(), ex);
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.kyc-evaluated:kyc.request.evaluated.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onKycEvaluatedEvent(@Payload KycRequestEvaluatedEvent event) {
        log.info("Received KycRequestEvaluatedEvent from Kafka: eventId={}, customerId={}, status={}",
                event != null ? event.getEventId() : "null",
                event != null && event.getPayload() != null ? event.getPayload().getCustomerId() : "null",
                event != null && event.getPayload() != null ? event.getPayload().getStatus() : "null");
        try {
            notificationService.processKycEvaluatedEvent(event);
        } catch (Exception ex) {
            log.error("Error processing KycRequestEvaluatedEvent [{}]: {}",
                    event != null ? event.getEventId() : "null", ex.getMessage(), ex);
            throw ex;
        }
    }
}
