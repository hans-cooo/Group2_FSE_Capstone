package com.group2.fse.ledger_service.event.publisher;

import com.group2.fse.ledger_service.event.LedgerMutationEvent;
import com.group2.fse.ledger_service.event.LedgerTransferEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Task: FSE-501
 * Transactional Event Publisher for Ledger Domain Events.
 * Listens strictly on TransactionPhase.AFTER_COMMIT so events are dispatched to Kafka
 * only after the Oracle and PostgreSQL transactions have successfully committed.
 * Rolled-back transactions emit zero events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private volatile boolean closed = false;

    @PreDestroy
    public void shutdown() {
        this.closed = true;
        log.info("LedgerEventPublisher marked as closed for shutdown.");
    }

    @Value("${ledger.kafka.topics.mutation:ledger.mutation.completed.v1}")
    private String mutationTopic;

    @Value("${ledger.kafka.topics.transfer:ledger.transfer.completed.v1}")
    private String transferTopic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMutationEvent(LedgerMutationEvent event) {
        if (closed) {
            log.warn("Attempted to publish LedgerMutationEvent after publisher shutdown. Skipping.");
            return;
        }
        if (event == null || event.getPayload() == null) {
            log.warn("Skipping null ledger mutation event or empty payload");
            return;
        }

        String partitionKey = String.valueOf(event.getPayload().getAccountId());
        log.info("Publishing LedgerMutationEvent to Kafka topic [{}] with key [{}]: eventId={}, txnId={}",
                mutationTopic, partitionKey, event.getEventId(), event.getPayload().getTransactionId());

        try {
            kafkaTemplate.send(mutationTopic, partitionKey, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish LedgerMutationEvent [{}] to topic [{}]: {}",
                                    event.getEventId(), mutationTopic, ex.getMessage(), ex);
                        } else if (result != null && result.getRecordMetadata() != null) {
                            log.info("Successfully published LedgerMutationEvent [{}] to topic [{}] partition [{}] offset [{}]",
                                    event.getEventId(),
                                    mutationTopic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            if (closed) {
                log.debug("Kafka producer closed during send for event [{}]", event.getEventId());
            } else {
                log.error("Exception during Kafka dispatch for LedgerMutationEvent [{}]: {}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransferEvent(LedgerTransferEvent event) {
        if (closed) {
            log.warn("Attempted to publish LedgerTransferEvent after publisher shutdown. Skipping.");
            return;
        }
        if (event == null || event.getPayload() == null) {
            log.warn("Skipping null ledger transfer event or empty payload");
            return;
        }

        String partitionKey = String.valueOf(event.getPayload().getSourceAccountId());
        log.info("Publishing LedgerTransferEvent to Kafka topic [{}] with key [{}]: eventId={}, ref={}",
                transferTopic, partitionKey, event.getEventId(), event.getPayload().getTransferReference());

        try {
            kafkaTemplate.send(transferTopic, partitionKey, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish LedgerTransferEvent [{}] to topic [{}]: {}",
                                    event.getEventId(), transferTopic, ex.getMessage(), ex);
                        } else if (result != null && result.getRecordMetadata() != null) {
                            log.info("Successfully published LedgerTransferEvent [{}] to topic [{}] partition [{}] offset [{}]",
                                    event.getEventId(),
                                    transferTopic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            if (closed) {
                log.debug("Kafka producer closed during send for event [{}]", event.getEventId());
            } else {
                log.error("Exception during Kafka dispatch for LedgerTransferEvent [{}]: {}",
                        event.getEventId(), ex.getMessage(), ex);
            }
        }
    }
}
