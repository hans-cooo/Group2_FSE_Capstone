package com.group2.fse.ledger_service.event.publisher;

import com.group2.fse.ledger_service.event.LedgerMutationEvent;
import com.group2.fse.ledger_service.event.LedgerTransferEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transactional Kafka Ledger Event Publisher Tests (FSE-501)")
class LedgerEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LedgerEventPublisher ledgerEventPublisher;

    private static final String MUTATION_TOPIC = "ledger.mutation.completed.v1";
    private static final String TRANSFER_TOPIC = "ledger.transfer.completed.v1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ledgerEventPublisher, "mutationTopic", MUTATION_TOPIC);
        ReflectionTestUtils.setField(ledgerEventPublisher, "transferTopic", TRANSFER_TOPIC);
    }

    @Test
    @DisplayName("Should publish LedgerMutationEvent to Kafka mutation topic keyed by account ID")
    void shouldPublishLedgerMutationEventSuccessfully() {
        // Arrange
        LedgerMutationEvent event = LedgerMutationEvent.builder()
                .eventId("evt_12345")
                .eventType("LEDGER_MUTATION_COMPLETED")
                .timestamp(Instant.now())
                .version("1.0")
                .payload(LedgerMutationEvent.MutationPayload.builder()
                        .transactionId(801L)
                        .accountId(1L)
                        .accountNumber("ACC_10000001")
                        .transactionType("DEBIT")
                        .amount(new BigDecimal("2500.0000"))
                        .previousBalance(new BigDecimal("50000.0000"))
                        .newBalance(new BigDecimal("47500.0000"))
                        .referenceNo("DEB-20260925-001")
                        .customerId(101L)
                        .build())
                .build();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(MUTATION_TOPIC, 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(MUTATION_TOPIC), eq("1"), eq(event))).thenReturn(future);

        // Act
        ledgerEventPublisher.handleMutationEvent(event);

        // Assert
        verify(kafkaTemplate, times(1)).send(eq(MUTATION_TOPIC), eq("1"), eq(event));
    }

    @Test
    @DisplayName("Should publish LedgerTransferEvent to Kafka transfer topic keyed by source account ID")
    void shouldPublishLedgerTransferEventSuccessfully() {
        // Arrange
        LedgerTransferEvent event = LedgerTransferEvent.builder()
                .eventId("evt_trf_999")
                .eventType("LEDGER_TRANSFER_COMPLETED")
                .timestamp(Instant.now())
                .version("1.0")
                .payload(LedgerTransferEvent.TransferPayload.builder()
                        .transferReference("TRF-20260926-001")
                        .sourceAccountId(10L)
                        .destinationAccountId(20L)
                        .amount(new BigDecimal("1500.0000"))
                        .sourcePreviousBalance(new BigDecimal("10000.0000"))
                        .sourceNewBalance(new BigDecimal("8500.0000"))
                        .destinationPreviousBalance(new BigDecimal("5000.0000"))
                        .destinationNewBalance(new BigDecimal("6500.0000"))
                        .build())
                .build();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TRANSFER_TOPIC, 1),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(TRANSFER_TOPIC), eq("10"), eq(event))).thenReturn(future);

        // Act
        ledgerEventPublisher.handleTransferEvent(event);

        // Assert
        verify(kafkaTemplate, times(1)).send(eq(TRANSFER_TOPIC), eq("10"), eq(event));
    }

    @Test
    @DisplayName("Should handle Kafka broker send failure gracefully without throwing exception")
    void shouldHandleKafkaSendFailureGracefully() {
        // Arrange
        LedgerMutationEvent event = LedgerMutationEvent.builder()
                .eventId("evt_fail")
                .payload(LedgerMutationEvent.MutationPayload.builder()
                        .transactionId(999L)
                        .accountId(5L)
                        .build())
                .build();

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection broker timeout"));

        when(kafkaTemplate.send(eq(MUTATION_TOPIC), eq("5"), eq(event))).thenReturn(failedFuture);

        // Act & Assert (should not throw exception out to caller)
        ledgerEventPublisher.handleMutationEvent(event);

        verify(kafkaTemplate, times(1)).send(eq(MUTATION_TOPIC), eq("5"), eq(event));
    }

    @Test
    @DisplayName("Should ignore null event or null payload without calling KafkaTemplate")
    void shouldIgnoreNullEventGracefully() {
        ledgerEventPublisher.handleMutationEvent(null);
        ledgerEventPublisher.handleMutationEvent(LedgerMutationEvent.builder().payload(null).build());
        ledgerEventPublisher.handleTransferEvent(null);
        ledgerEventPublisher.handleTransferEvent(LedgerTransferEvent.builder().payload(null).build());

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
