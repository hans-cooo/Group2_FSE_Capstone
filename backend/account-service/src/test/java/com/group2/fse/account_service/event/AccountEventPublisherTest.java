package com.group2.fse.account_service.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Kafka Event Publisher Tests")
class AccountEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AccountEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "closureTopic", "account.closure.completed.v1");
        ReflectionTestUtils.setField(publisher, "createdTopic", "account.created.v1");
    }

    @Test
    @DisplayName("[EVT-001] Should successfully publish AccountClosureCompletedEvent to Kafka topic")
    void shouldPublishAccountClosureEvent() {
        AccountClosureCompletedEvent event = AccountClosureCompletedEvent.builder()
                .closureRequestId(88L)
                .accountId(4L)
                .accountNumber("ACC_10000004")
                .customerId(101L)
                .approvedBy(1L)
                .timestamp(LocalDateTime.now())
                .build();

        publisher.publishAccountClosure(event);

        verify(kafkaTemplate).send(eq("account.closure.completed.v1"), eq("4"), eq(event));
    }

    @Test
    @DisplayName("[EVT-002] Should successfully publish AccountCreatedEvent to Kafka topic")
    void shouldPublishAccountCreatedEvent() {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(305L)
                .accountNumber("ACC_10000005")
                .accountType("SAVINGS")
                .customerId(101L)
                .currency("PHP")
                .timestamp(LocalDateTime.now())
                .build();

        publisher.publishAccountCreated(event);

        verify(kafkaTemplate).send(eq("account.created.v1"), eq("305"), eq(event));
    }

    @Test
    @DisplayName("[EVT-003] Should handle Kafka publisher exceptions gracefully without throwing")
    void shouldHandleKafkaExceptionGracefully() {
        AccountClosureCompletedEvent event = AccountClosureCompletedEvent.builder()
                .closureRequestId(88L)
                .accountId(4L)
                .build();

        doThrow(new RuntimeException("Kafka broker down"))
                .when(kafkaTemplate).send(anyString(), anyString(), any());

        // Should not throw exception
        publisher.publishAccountClosure(event);

        verify(kafkaTemplate).send(eq("account.closure.completed.v1"), eq("4"), eq(event));
    }
}
