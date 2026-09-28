package com.group2.fse.notification_service.listener;

import com.group2.fse.notification_service.event.LedgerMutationEvent;
import com.group2.fse.notification_service.event.LedgerTransferEvent;
import com.group2.fse.notification_service.event.LedgerTransferFailedEvent;
import com.group2.fse.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Task: FSE-601
 * Asynchronous Kafka consumer for ledger balance mutations and transfers.
 * Implements at-least-once ingestion backed by the Idempotent Consumer Pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerNotificationListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${notification.kafka.topics.mutation:ledger.mutation.completed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onMutationEvent(@Payload LedgerMutationEvent event) {
        log.info("Received LedgerMutationEvent from Kafka: eventId={}, type={}",
                event != null ? event.getEventId() : "null",
                event != null ? event.getEventType() : "null");
        try {
            notificationService.processMutationEvent(event);
        } catch (Exception ex) {
            log.error("Error processing LedgerMutationEvent [{}]: {}",
                    event != null ? event.getEventId() : "null", ex.getMessage(), ex);
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.transfer:ledger.transfer.completed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onTransferEvent(@Payload LedgerTransferEvent event) {
        log.info("Received LedgerTransferEvent from Kafka: eventId={}, ref={}",
                event != null ? event.getEventId() : "null",
                event != null && event.getPayload() != null ? event.getPayload().getTransferReference() : "null");
        try {
            notificationService.processTransferEvent(event);
        } catch (Exception ex) {
            log.error("Error processing LedgerTransferEvent [{}]: {}",
                    event != null ? event.getEventId() : "null", ex.getMessage(), ex);
            throw ex;
        }
    }

    @KafkaListener(
            topics = "${notification.kafka.topics.transfer-failed:ledger.transfer.failed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onTransferFailedEvent(@Payload LedgerTransferFailedEvent event) {
        log.info("Received LedgerTransferFailedEvent from Kafka: eventId={}, ref={}",
                event != null ? event.getEventId() : "null",
                event != null && event.getPayload() != null ? event.getPayload().getTransferReference() : "null");
        try {
            notificationService.processTransferFailedEvent(event);
        } catch (Exception ex) {
            log.error("Error processing LedgerTransferFailedEvent [{}]: {}",
                    event != null ? event.getEventId() : "null", ex.getMessage(), ex);
            throw ex;
        }
    }
}
