package com.xtended.subscriptionservice.subscription.messaging;

import com.xtended.subscriptionservice.subscription.model.Outbox;
import com.xtended.subscriptionservice.subscription.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для OutboxProcessor")
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    private Outbox testOutbox;

    @BeforeEach
    void setUp() {
        testOutbox = Outbox.builder()
                .id(UUID.randomUUID())
                .aggregateId("test-aggregate-id")
                .eventType("TEST_EVENT")
                .routingKey("test.routing.key")
                .payload("{\"test\": \"payload\"}")
                .build();
    }

    @Test
    @DisplayName("Должен успешно обработать события из outbox")
    void processPendingEvents_WhenEventsExist_ShouldProcessSuccessfully() {
        List<Outbox> events = List.of(testOutbox);
        when(repository.findAll()).thenReturn(events);

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq("test.routing.key"), eq("{\"test\": \"payload\"}"));
        verify(repository).delete(testOutbox);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Должен обработать несколько событий успешно")
    void processPendingEvents_WhenMultipleEventsExist_ShouldProcessAllSuccessfully() {
        Outbox secondOutbox = Outbox.builder()
                .id(UUID.randomUUID())
                .aggregateId("second-aggregate-id")
                .eventType("SECOND_EVENT")
                .routingKey("second.routing.key")
                .payload("{\"second\": \"payload\"}")
                .build();

        List<Outbox> events = List.of(testOutbox, secondOutbox);
        when(repository.findAll()).thenReturn(events);

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq("test.routing.key"), eq("{\"test\": \"payload\"}"));
        verify(rabbitTemplate).convertAndSend(eq("second.routing.key"), eq("{\"second\": \"payload\"}"));
        verify(repository).delete(testOutbox);
        verify(repository).delete(secondOutbox);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Должен обработать ошибку при отправке в RabbitMQ и не удалять событие")
    void processPendingEvents_WhenRabbitMQFails_ShouldLogErrorAndNotDeleteEvent() {
        List<Outbox> events = List.of(testOutbox);
        when(repository.findAll()).thenReturn(events);
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class));

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq("test.routing.key"), eq("{\"test\": \"payload\"}"));
        verify(repository, never()).delete(testOutbox);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Должен обработать частичный сбой - одно событие успешно, другое с ошибкой")
    void processPendingEvents_WhenPartialFailure_ShouldProcessSuccessfulEventsAndLogFailures() {
        Outbox failingOutbox = Outbox.builder()
                .id(UUID.randomUUID())
                .aggregateId("failing-aggregate-id")
                .eventType("FAILING_EVENT")
                .routingKey("failing.routing.key")
                .payload("{\"failing\": \"payload\"}")
                .build();

        List<Outbox> events = List.of(testOutbox, failingOutbox);
        when(repository.findAll()).thenReturn(events);
        doNothing().when(rabbitTemplate).convertAndSend(eq("test.routing.key"), eq("{\"test\": \"payload\"}"));
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend("failing.routing.key", "{\"failing\": \"payload\"}");

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq("test.routing.key"), eq("{\"test\": \"payload\"}"));
        verify(rabbitTemplate).convertAndSend(eq("failing.routing.key"), eq("{\"failing\": \"payload\"}"));
        verify(repository).delete(testOutbox);
        verify(repository, never()).delete(failingOutbox);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Должен корректно обработать пустой список событий")
    void processPendingEvents_WhenNoEventsExist_ShouldDoNothing() {
        List<Outbox> events = List.of();
        when(repository.findAll()).thenReturn(events);

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class));
        verify(repository, never()).delete(any(Outbox.class));
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Должен обработать событие с пустым payload")
    void processPendingEvents_WhenEventHasEmptyPayload_ShouldProcessSuccessfully() {
        Outbox emptyPayloadOutbox = Outbox.builder()
                .id(UUID.randomUUID())
                .aggregateId("empty-payload-aggregate")
                .eventType("EMPTY_PAYLOAD_EVENT")
                .routingKey("empty.payload.routing.key")
                .payload("")
                .build();

        List<Outbox> events = List.of(emptyPayloadOutbox);
        when(repository.findAll()).thenReturn(events);

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq("empty.payload.routing.key"), eq(""));
        verify(repository).delete(emptyPayloadOutbox);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Должен обработать событие с null routing key")
    void processPendingEvents_WhenEventHasNullRoutingKey_ShouldProcessSuccessfully() {
        Outbox nullRoutingKeyOutbox = Outbox.builder()
                .id(UUID.randomUUID())
                .aggregateId("null-routing-aggregate")
                .eventType("NULL_ROUTING_EVENT")
                .routingKey(null)
                .payload("{\"test\": \"payload\"}")
                .build();

        List<Outbox> events = List.of(nullRoutingKeyOutbox);
        when(repository.findAll()).thenReturn(events);

        outboxProcessor.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq(null), eq("{\"test\": \"payload\"}"));
        verify(repository).delete(nullRoutingKeyOutbox);
        verify(repository).findAll();
    }
}
