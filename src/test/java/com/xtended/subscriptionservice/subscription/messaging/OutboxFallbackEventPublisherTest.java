package com.xtended.subscriptionservice.subscription.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtended.subscriptionservice.subscription.service.OutboxPersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для OutboxFallbackEventPublisher")
class OutboxFallbackEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OutboxPersistenceService outboxPersistenceService;

    @InjectMocks
    private OutboxFallbackEventPublisher publisher;

    private final String routingKey = "test.routing.key";
    private final Object payload = new Object();
    private final String aggregateId = "agg-123";
    private final String serializedPayload = "{\"test\":\"json\"}";

    @Test
    @DisplayName("Успешная отправка события в RabbitMQ после коммита транзакции")
    void publishAfterCommit_successfulSend_shouldSendToRabbit() throws Exception {
        try (var mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class)) {
            AtomicReference<TransactionSynchronization> syncRef = new AtomicReference<>();
            mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocation -> {
                        syncRef.set(invocation.getArgument(0));
                        return null;
                    });

            when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);

            publisher.publishAfterCommit(routingKey, payload, aggregateId);
            TransactionSynchronization synchronization = syncRef.get();
            assertThat(synchronization).isNotNull();
            synchronization.afterCommit();

            verify(objectMapper).writeValueAsString(payload);
            verify(rabbitTemplate).convertAndSend(routingKey, serializedPayload);
            verify(outboxPersistenceService, never()).saveOutboxEvent(any(), any(), any());
        }
    }

    @Test
    @DisplayName("Сохранение события в outbox при ошибке RabbitMQ")
    void publishAfterCommit_rabbitThrowsException_shouldSaveToOutbox() throws Exception {
        try (var mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class)) {
            AtomicReference<TransactionSynchronization> syncRef = new AtomicReference<>();
            mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocation -> {
                        syncRef.set(invocation.getArgument(0));
                        return null;
                    });

            when(objectMapper.writeValueAsString(payload)).thenReturn(serializedPayload);
            doThrow(new AmqpException("Broker unavailable"))
                    .when(rabbitTemplate).convertAndSend(eq(routingKey), eq(serializedPayload));

            publisher.publishAfterCommit(routingKey, payload, aggregateId);
            TransactionSynchronization synchronization = syncRef.get();
            assertThat(synchronization).isNotNull();
            synchronization.afterCommit();

            verify(objectMapper).writeValueAsString(payload);
            verify(rabbitTemplate).convertAndSend(routingKey, serializedPayload);
            verify(outboxPersistenceService).saveOutboxEvent(routingKey, payload, aggregateId);
        }
    }

    @Test
    @DisplayName("Сохранение события в outbox при ошибке сериализации")
    void publishAfterCommit_serializationFails_shouldSaveToOutbox() throws Exception {
        try (var mockedTransactionManager = mockStatic(TransactionSynchronizationManager.class)) {
            AtomicReference<TransactionSynchronization> syncRef = new AtomicReference<>();
            mockedTransactionManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(invocation -> {
                        syncRef.set(invocation.getArgument(0));
                        return null;
                    });

            when(objectMapper.writeValueAsString(payload))
                    .thenThrow(new JsonProcessingException("Invalid JSON") {});

            publisher.publishAfterCommit(routingKey, payload, aggregateId);
            TransactionSynchronization synchronization = syncRef.get();
            assertThat(synchronization).isNotNull();
            synchronization.afterCommit();

            verify(objectMapper).writeValueAsString(payload);
            verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class));
            verify(outboxPersistenceService).saveOutboxEvent(routingKey, payload, aggregateId);
        }
    }
}
